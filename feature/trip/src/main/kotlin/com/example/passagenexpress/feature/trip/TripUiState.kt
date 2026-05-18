package com.example.passagenexpress.feature.trip

import com.example.passagenexpress.core.domain.model.Trecho
import java.time.LocalDate

data class TripUiState(
    val origemNome: String = "",
    val destinoNome: String = "",
    val date: LocalDate = LocalDate.now(),
    val selectedTrechoId: Long? = null,
    val trips: TripsState = TripsState.Loading,
)

sealed interface TripsState {
    data object Loading : TripsState
    data class Loaded(val trechos: List<Trecho>, val proximaViagem: Trecho? = null) : TripsState
    data class Error(val message: String) : TripsState
}
