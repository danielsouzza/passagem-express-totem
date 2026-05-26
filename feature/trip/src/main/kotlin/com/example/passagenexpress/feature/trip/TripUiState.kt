package com.example.passagenexpress.feature.trip

import com.example.passagenexpress.core.domain.model.Municipio
import com.example.passagenexpress.core.domain.model.Trecho
import java.time.LocalDate
import java.time.YearMonth

data class TripUiState(
    val origemNome: String = "",
    val destinoNome: String = "",
    val date: LocalDate = LocalDate.now(),
    val selectedTrechoId: Long? = null,
    val trips: TripsState = TripsState.Loading,
    val filterSheet: FilterSheetState = FilterSheetState.Hidden,
)

sealed interface TripsState {
    data object Loading : TripsState
    data class Loaded(val trechos: List<Trecho>, val proximaViagem: Trecho? = null) : TripsState
    data class Error(val message: String) : TripsState
}

sealed interface FilterSheetState {
    data object Hidden : FilterSheetState
    data object Loading : FilterSheetState
    data class Open(
        val destinations: List<Municipio>,
        val selectedDestinoSlug: String,
        val selectedDestinoNome: String,
        val selectedDate: LocalDate,
        val visibleMonth: YearMonth,
        val error: String? = null,
    ) : FilterSheetState
}
