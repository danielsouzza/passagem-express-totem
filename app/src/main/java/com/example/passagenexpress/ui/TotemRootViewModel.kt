package com.example.passagenexpress.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.usecase.ObservarTotemConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TotemRootViewModel @Inject constructor(
    observarTotemConfig: ObservarTotemConfigUseCase,
) : ViewModel() {

    private val config = observarTotemConfig()

    val rootDestination: StateFlow<RootDestination> = config
        .map { config ->
            // Setup vale se concluído E com ao menos um filtro de origem: porto (totem em terra)
            // ou embarcação (totem a bordo). Sem nenhum, não há como listar viagens.
            val complete = config.setupComplete &&
                (config.portoSlug.isNotEmpty() || config.embarcacaoId != null)
            if (complete) RootDestination.Idle else RootDestination.Setup
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = RootDestination.Loading,
        )

    val language: StateFlow<AppLanguage> = config
        .map { it.defaultLanguage }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppLanguage.PtBr,
        )
}

sealed interface RootDestination {
    data object Loading : RootDestination
    data object Setup : RootDestination
    data object Idle : RootDestination
}
