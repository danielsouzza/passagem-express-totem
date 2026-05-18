package com.example.passagenexpress.feature.idle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.domain.usecase.ObservarTotemConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class IdleViewModel @Inject constructor(
    observarTotemConfig: ObservarTotemConfigUseCase,
) : ViewModel() {

    val uiState: StateFlow<IdleUiState> = observarTotemConfig()
        .map { config ->
            IdleUiState(
                portoNome = config.portoNome,
                defaultLanguage = config.defaultLanguage,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = IdleUiState(),
        )
}
