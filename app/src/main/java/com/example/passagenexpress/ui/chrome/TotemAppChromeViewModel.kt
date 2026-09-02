package com.example.passagenexpress.ui.chrome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.usecase.ObservarTotemConfigUseCase
import com.example.passagenexpress.core.domain.usecase.SetLanguageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TotemAppChromeState(
    val portoNome: String = "",
    val language: AppLanguage = AppLanguage.PtBr,
)

@HiltViewModel
class TotemAppChromeViewModel @Inject constructor(
    observarTotemConfig: ObservarTotemConfigUseCase,
    private val setLanguage: SetLanguageUseCase,
) : ViewModel() {

    val state: StateFlow<TotemAppChromeState> = observarTotemConfig()
        .map { config ->
            TotemAppChromeState(
                // Sem porto (totem a bordo): mostra o nome da embarcação no lugar do porto.
                portoNome = config.portoNome.ifEmpty { config.embarcacaoNome },
                language = config.defaultLanguage,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TotemAppChromeState(),
        )

    fun onLanguageChange(language: AppLanguage) {
        viewModelScope.launch { setLanguage(language) }
    }
}
