package com.example.passagenexpress.feature.bus.trips

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.onibus.ViagemOnibus
import com.example.passagenexpress.core.domain.usecase.onibus.BuscarViagensOnibusUseCase
import com.example.passagenexpress.feature.bus.BusSaleSession
import com.example.passagenexpress.feature.bus.Sentido
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

sealed interface ViagensState {
    data object Loading : ViagensState
    data class Loaded(val viagens: List<ViagemOnibus>) : ViagensState
    data class Error(val message: String) : ViagensState
}

data class BusTripsUiState(
    val sentido: Sentido = Sentido.Ida,
    val origemNome: String = "",
    val destinoNome: String = "",
    val data: LocalDate = LocalDate.now(),
    /** Menor data permitida (hoje na ida; a data da ida na volta). */
    val minData: LocalDate = LocalDate.now(),
    val viagens: ViagensState = ViagensState.Loading,
)

@HiltViewModel
class BusTripsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val buscarViagens: BuscarViagensOnibusUseCase,
    private val session: BusSaleSession,
) : ViewModel() {

    private val sentido: Sentido = savedStateHandle.get<String>(ARG_SENTIDO)
        ?.let { runCatching { Sentido.valueOf(it) }.getOrNull() } ?: Sentido.Ida

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<BusTripsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    private fun initialState(): BusTripsUiState {
        val s = session.value
        val origem = s.origem?.nome.orEmpty()
        val destino = s.destino?.nome.orEmpty()
        return if (sentido == Sentido.Ida) {
            BusTripsUiState(sentido, origem, destino, s.dataIda, LocalDate.now())
        } else {
            BusTripsUiState(sentido, destino, origem, s.dataVolta ?: s.dataIda, s.dataIda)
        }
    }

    fun onRetry() = load()

    fun onPrevDay() = changeDate(_uiState.value.data.minusDays(1))

    fun onNextDay() = changeDate(_uiState.value.data.plusDays(1))

    private fun changeDate(date: LocalDate) {
        if (date.isBefore(_uiState.value.minData)) return
        session.setData(sentido, date)
        _uiState.update { it.copy(data = date) }
        load()
    }

    private fun load() {
        val s = session.value
        val origem = s.origem?.slug.orEmpty()
        val destino = s.destino?.slug.orEmpty()
        // Volta = mesma rota com from/to trocados.
        val (from, to) = if (sentido == Sentido.Ida) origem to destino else destino to origem
        val data = _uiState.value.data
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(viagens = ViagensState.Loading) }
            when (val result = buscarViagens(from, to, data)) {
                is AppResult.Success -> {
                    // Só viagens futuras e com assento livre (o back às vezes devolve as já saídas no dia).
                    val agora = LocalDateTime.now()
                    val lista = result.value.filter { it.saida.isAfter(agora) && it.assentosDisponiveis > 0 }
                    _uiState.update { it.copy(viagens = ViagensState.Loaded(lista)) }
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(viagens = ViagensState.Error(result.error.message.orEmpty()))
                }
            }
        }
    }

    /** Grava a viagem escolhida na sessão; a tela navega pros assentos. */
    fun onViagemSelected(viagem: ViagemOnibus) {
        session.setViagem(sentido, viagem)
    }

    companion object {
        const val ARG_SENTIDO = "sentido"
    }
}
