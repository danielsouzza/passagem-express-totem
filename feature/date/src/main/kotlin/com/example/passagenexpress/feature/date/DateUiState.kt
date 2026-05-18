package com.example.passagenexpress.feature.date

import java.time.LocalDate
import java.time.YearMonth

data class DateUiState(
    val municipioDestinoNome: String = "",
    val today: LocalDate = LocalDate.now(),
    val selected: LocalDate? = null,
    val visibleMonth: YearMonth = YearMonth.from(LocalDate.now()),
) {
    val tomorrow: LocalDate get() = today.plusDays(1)

    fun isSelectable(date: LocalDate): Boolean = !date.isBefore(today)
}
