package com.example.passagenexpress.feature.date

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class DateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState(savedStateHandle))
    val uiState: StateFlow<DateUiState> = _uiState.asStateFlow()

    fun onDateSelected(date: LocalDate) {
        val s = _uiState.value
        if (!s.isSelectable(date)) return
        _uiState.update {
            it.copy(selected = date, visibleMonth = YearMonth.from(date))
        }
    }

    fun onPrevMonth() {
        _uiState.update { it.copy(visibleMonth = it.visibleMonth.minusMonths(1)) }
    }

    fun onNextMonth() {
        _uiState.update { it.copy(visibleMonth = it.visibleMonth.plusMonths(1)) }
    }

    private fun initialState(savedStateHandle: SavedStateHandle): DateUiState {
        val destinoNome: String = savedStateHandle.get<String>("destinoNome").orEmpty()
        val today = LocalDate.now()
        return DateUiState(
            municipioDestinoNome = destinoNome,
            today = today,
            selected = today,
            visibleMonth = YearMonth.from(today),
        )
    }
}
