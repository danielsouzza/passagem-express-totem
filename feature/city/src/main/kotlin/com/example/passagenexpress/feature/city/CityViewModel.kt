package com.example.passagenexpress.feature.city

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.Municipio
import com.example.passagenexpress.core.domain.usecase.BuscarMunicipiosDestinoUseCase
import com.example.passagenexpress.core.domain.usecase.ObservarTotemConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pulamos a etapa "origem": o porto configurado no setup já define a cidade de embarque.
 * Buscamos os destinos direto via `/api/filtros?porto_id=X`.
 */
@HiltViewModel
class CityViewModel @Inject constructor(
    private val observarTotemConfig: ObservarTotemConfigUseCase,
    private val buscarMunicipiosDestino: BuscarMunicipiosDestinoUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CityUiState())
    val uiState: StateFlow<CityUiState> = _uiState.asStateFlow()

    private var portoSlug: String? = null

    init {
        viewModelScope.launch {
            val config = observarTotemConfig().first()
            portoSlug = config.portoSlug.ifEmpty { null }
            _uiState.update {
                it.copy(
                    portoNome = config.portoNome,
                    municipioOrigemNome = config.municipioNome,
                )
            }
            loadDestinos()
        }
    }

    fun onDestinoSearchChange(query: String) {
        _uiState.update { it.copy(destinoSearch = query) }
    }

    fun onDestinoSelected(municipio: Municipio) {
        _uiState.update { it.copy(destinoSelecionada = municipio) }
    }

    fun onRetry() {
        _uiState.update { it.copy(destinos = MunicipiosState.Loading) }
        viewModelScope.launch { loadDestinos() }
    }

    private suspend fun loadDestinos() {
        when (val result = buscarMunicipiosDestino(portoSlug, municipioOrigemCodigo = null)) {
            is AppResult.Success -> _uiState.update {
                it.copy(destinos = MunicipiosState.Loaded(result.value))
            }
            is AppResult.Failure -> _uiState.update {
                it.copy(destinos = MunicipiosState.Error(result.error.message ?: "Erro desconhecido"))
            }
        }
    }
}
