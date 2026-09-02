package com.example.passagenexpress.feature.trip

import com.example.passagenexpress.core.domain.model.Municipio
import com.example.passagenexpress.core.domain.model.Trecho
import java.time.LocalDate
import java.time.YearMonth

data class TripUiState(
    val origemNome: String = "",
    val destinoNome: String = "",
    val date: LocalDate = LocalDate.now(),
    /** Na entrada busca as próximas viagens (sem data fixa); vira false ao aplicar um filtro. */
    val dataIrrestrita: Boolean = true,
    val trips: TripsState = TripsState.Loading,
    val filterSheet: FilterSheetState = FilterSheetState.Hidden,
)

/**
 * Identidade única de uma linha da lista. No modo irrestrito o mesmo `id` (trecho) aparece em
 * várias viagens/partidas, então a chave combina trecho + viagem.
 */
fun Trecho.rowKey(): String = "$id-$idViagem"

sealed interface TripsState {
    data object Loading : TripsState
    data class Loaded(val trechos: List<Trecho>, val proximaViagem: Trecho? = null) : TripsState
    data class Error(val message: String) : TripsState
}

/** Cada filtro tem seu próprio modal. */
enum class FilterMode { Date, Destino }

sealed interface FilterSheetState {
    data object Hidden : FilterSheetState
    data object Loading : FilterSheetState
    data class Open(
        val mode: FilterMode,
        val destinations: List<Municipio>,
        val selectedDestinoSlug: String,
        val selectedDestinoNome: String,
        val selectedDate: LocalDate,
        val visibleMonth: YearMonth,
        val error: String? = null,
    ) : FilterSheetState
}
