package com.example.passagenexpress.ui

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
class TotemRootViewModel @Inject constructor(
    observarTotemConfig: ObservarTotemConfigUseCase,
) : ViewModel() {

    val rootDestination: StateFlow<RootDestination> = observarTotemConfig()
        .map { config ->
            val complete = config.setupComplete && config.portoSlug.isNotEmpty()
            if (complete) RootDestination.Idle else RootDestination.Setup
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = RootDestination.Loading,
        )
}

sealed interface RootDestination {
    data object Loading : RootDestination
    data object Setup : RootDestination
    data object Idle : RootDestination
}
