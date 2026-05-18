package com.example.passagenexpress.feature.trip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.BuscaViagensFiltros
import com.example.passagenexpress.core.domain.model.Trecho
import com.example.passagenexpress.core.domain.usecase.BuscarViagensUseCase
import com.example.passagenexpress.core.domain.usecase.ObservarTotemConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TripViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observarTotemConfig: ObservarTotemConfigUseCase,
    private val buscarViagens: BuscarViagensUseCase,
) : ViewModel() {

    private val destinoSlug: String = savedStateHandle.get<String>(ARG_DESTINO_SLUG).orEmpty()

    private val _uiState = MutableStateFlow(
        TripUiState(
            destinoNome = savedStateHandle.get<String>(ARG_DESTINO_NOME).orEmpty(),
            date = parseDate(savedStateHandle.get<String>(ARG_DATE)),
        )
    )
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch { search() }
    }

    fun onSelectTrip(trecho: Trecho) {
        _uiState.update { it.copy(selectedTrechoId = trecho.id) }
    }

    fun onPreviousDate() {
        changeDate(_uiState.value.date.minusDays(1))
    }

    fun onNextDate() {
        changeDate(_uiState.value.date.plusDays(1))
    }

    fun onJumpToProximaViagem() {
        val proxima = (_uiState.value.trips as? TripsState.Loaded)?.proximaViagem ?: return
        _uiState.update {
            it.copy(
                date = proxima.dataEmbarque,
                selectedTrechoId = null,
                trips = TripsState.Loading,
            )
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { search() }
    }

    fun onRetry() {
        _uiState.update { it.copy(trips = TripsState.Loading) }
        viewModelScope.launch { search() }
    }

    private fun changeDate(newDate: LocalDate) {
        if (newDate == _uiState.value.date) return
        _uiState.update {
            it.copy(date = newDate, selectedTrechoId = null, trips = TripsState.Loading)
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { search() }
    }

    private suspend fun search() {
        val config = observarTotemConfig().first()
        val portoSlug = config.portoSlug.ifEmpty { null }
        _uiState.update {
            it.copy(origemNome = config.municipioNome.ifEmpty { config.portoNome })
        }

        val filtros = BuscaViagensFiltros(
            portoSlug = portoSlug,
            municipioOrigemCodigo = null,
            municipioDestinoSlug = destinoSlug.ifEmpty { null },
            data = _uiState.value.date,
        )
        when (val result = buscarViagens(filtros)) {
            is AppResult.Success -> _uiState.update {
                it.copy(
                    trips = TripsState.Loaded(
                        trechos = result.value.trechos,
                        proximaViagem = result.value.proximaViagem,
                    ),
                )
            }
            is AppResult.Failure -> _uiState.update {
                it.copy(trips = TripsState.Error(result.error.message ?: "Erro desconhecido"))
            }
        }
    }

    private fun parseDate(raw: String?): LocalDate =
        runCatching { LocalDate.parse(raw) }.getOrDefault(LocalDate.now())

    companion object {
        const val ARG_DESTINO_SLUG = "destinoSlug"
        const val ARG_DESTINO_NOME = "destinoNome"
        const val ARG_DATE = "date"
    }
}
