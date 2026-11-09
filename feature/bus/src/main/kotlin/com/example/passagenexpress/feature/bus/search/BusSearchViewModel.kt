package com.example.passagenexpress.feature.bus.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.onibus.LocalOnibus
import com.example.passagenexpress.core.domain.model.onibus.Terminal
import com.example.passagenexpress.core.domain.usecase.ObservarTotemConfigUseCase
import com.example.passagenexpress.core.domain.usecase.onibus.BuscarTerminaisUseCase
import com.example.passagenexpress.feature.bus.BusSaleSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class BusSearchStage { Destino, Datas }

/** Qual data o calendário está editando (ida ou volta). */
enum class DataEditando { Ida, Volta }

sealed interface DestinosState {
    /** Menos de 3 letras digitadas. */
    data object Idle : DestinosState
    data object Loading : DestinosState
    data class Loaded(val terminais: List<Terminal>) : DestinosState
    data class Error(val message: String) : DestinosState
}

data class BusSearchUiState(
    val origem: LocalOnibus? = null,
    val stage: BusSearchStage = BusSearchStage.Destino,
    val destinoQuery: String = "",
    val destinos: DestinosState = DestinosState.Idle,
    val destino: LocalOnibus? = null,
    /** Teclado aberto no campo de destino (já abre na entrada — fluxo guiado). */
    val keypadOpen: Boolean = true,
    val idaEVolta: Boolean = false,
    val dataIda: LocalDate = LocalDate.now(),
    val dataVolta: LocalDate? = null,
    val editando: DataEditando = DataEditando.Ida,
    val visibleMonth: YearMonth = YearMonth.now(),
) {
    val canSearch: Boolean
        get() = origem != null && destino != null && (!idaEVolta || (dataVolta != null && !dataVolta.isBefore(dataIda)))
}

@HiltViewModel
class BusSearchViewModel @Inject constructor(
    private val observarTotemConfig: ObservarTotemConfigUseCase,
    private val buscarTerminais: BuscarTerminaisUseCase,
    private val session: BusSaleSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusSearchUiState())
    val uiState: StateFlow<BusSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            // Origem = terminal fixo do totem. Nova entrada no grafo = nova venda (zera a sessão).
            val config = observarTotemConfig().first()
            val origem = LocalOnibus(slug = config.terminalSlug, nome = config.terminalNome)
            session.iniciar(origem)
            _uiState.update { it.copy(origem = origem) }
        }
    }

    // --- Destino ----------------------------------------------------------

    fun onOpenKeypad() = _uiState.update { it.copy(keypadOpen = true) }

    fun onCloseKeypad() = _uiState.update { it.copy(keypadOpen = false) }

    fun onKeypadChar(c: Char) {
        _uiState.update { it.copy(destinoQuery = it.destinoQuery + c) }
        scheduleSearch()
    }

    fun onKeypadBackspace() {
        _uiState.update { it.copy(destinoQuery = it.destinoQuery.dropLast(1)) }
        scheduleSearch()
    }

    fun onClearQuery() {
        searchJob?.cancel()
        _uiState.update { it.copy(destinoQuery = "", destinos = DestinosState.Idle, keypadOpen = true) }
    }

    fun onRetry() = scheduleSearch(debounce = false)

    private fun scheduleSearch(debounce: Boolean = true) {
        searchJob?.cancel()
        val query = _uiState.value.destinoQuery.trim()
        if (query.length < BuscarTerminaisUseCase.MinChars) {
            _uiState.update { it.copy(destinos = DestinosState.Idle) }
            return
        }
        searchJob = viewModelScope.launch {
            if (debounce) delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(destinos = DestinosState.Loading) }
            when (val result = buscarTerminais(query)) {
                is AppResult.Success -> _uiState.update { it.copy(destinos = DestinosState.Loaded(result.value)) }
                is AppResult.Failure -> _uiState.update {
                    it.copy(destinos = DestinosState.Error(result.error.message.orEmpty()))
                }
            }
        }
    }

    /** Escolheu o destino → vai pras datas (passo seguinte do mesmo formulário). */
    fun onDestinoSelected(destino: LocalOnibus) {
        _uiState.update { it.copy(destino = destino, keypadOpen = false, stage = BusSearchStage.Datas) }
    }

    fun onEditDestino() {
        _uiState.update { it.copy(stage = BusSearchStage.Destino, keypadOpen = true) }
    }

    // --- Datas ------------------------------------------------------------

    fun onIdaEVoltaChange(idaEVolta: Boolean) {
        _uiState.update {
            it.copy(
                idaEVolta = idaEVolta,
                // Ao ligar ida-e-volta, já leva o calendário pra escolher a volta.
                editando = if (idaEVolta) DataEditando.Volta else DataEditando.Ida,
                dataVolta = if (idaEVolta) it.dataVolta else null,
                visibleMonth = YearMonth.from(if (idaEVolta) (it.dataVolta ?: it.dataIda) else it.dataIda),
            )
        }
    }

    fun onEditarData(qual: DataEditando) {
        _uiState.update {
            val ref = if (qual == DataEditando.Ida) it.dataIda else (it.dataVolta ?: it.dataIda)
            it.copy(editando = qual, visibleMonth = YearMonth.from(ref))
        }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { s ->
            when (s.editando) {
                DataEditando.Ida -> {
                    // Volta antes da nova ida fica inválida → limpa.
                    val volta = s.dataVolta?.takeIf { !it.isBefore(date) }
                    s.copy(
                        dataIda = date,
                        dataVolta = volta,
                        editando = if (s.idaEVolta && volta == null) DataEditando.Volta else DataEditando.Ida,
                    )
                }
                DataEditando.Volta -> s.copy(dataVolta = date)
            }
        }
    }

    fun onPrevMonth() = _uiState.update { it.copy(visibleMonth = it.visibleMonth.minusMonths(1)) }

    fun onNextMonth() = _uiState.update { it.copy(visibleMonth = it.visibleMonth.plusMonths(1)) }

    /** Grava a busca na sessão; a tela navega pra lista de viagens de ida. */
    fun onSearch(): Boolean {
        val s = _uiState.value
        if (!s.canSearch) return false
        session.setBusca(
            destino = s.destino ?: return false,
            idaEVolta = s.idaEVolta,
            dataIda = s.dataIda,
            dataVolta = s.dataVolta,
        )
        return true
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
