package com.example.passagenexpress.feature.bus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme
import com.example.passagenexpress.feature.bus.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

internal val LocaleBR: Locale = Locale.forLanguageTag("pt-BR")

internal fun formatMoney(value: Double): String =
    "R$ " + String.format(LocaleBR, "%,.2f", value)

internal fun formatTime(dt: LocalDateTime): String = dt.format(DateTimeFormatter.ofPattern("HH:mm"))

internal fun formatShortDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE, dd/MM", LocaleBR)).replaceFirstChar { it.uppercase(LocaleBR) }

internal fun formatLongDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", LocaleBR)).replaceFirstChar { it.uppercase(LocaleBR) }

// ---------------------------------------------------------------------------
// Calendário — cópia do calendário do fluxo de barco (TripScreen), mantido separado pra não
// mexer no fluxo hidroviário já validado.
// ---------------------------------------------------------------------------

private val ORDERED_DOW = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

@Composable
internal fun BusCalendar(
    selected: LocalDate?,
    visibleMonth: YearMonth,
    minDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
    /** Dia marcado como referência (ex.: a ida, quando escolhendo a volta). */
    highlighted: LocalDate? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TotemPalette.Paper,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
        border = BorderStroke(1.5.dp, TotemPalette.Hairline),
    ) {
        Column(
            modifier = Modifier.padding(TotemTheme.dimens.space12),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
        ) {
            val monthLabel = visibleMonth.month.getDisplayName(TextStyle.FULL, LocaleBR)
                .replaceFirstChar { it.uppercase(LocaleBR) } + " " + visibleMonth.year
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
                ) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = TotemPalette.Ink, modifier = Modifier.size(20.dp))
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TotemPalette.Ink,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8)) {
                    CalendarArrow(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        description = stringResource(R.string.bus_calendar_prev),
                        enabled = YearMonth.from(minDate) < visibleMonth,
                        onClick = onPrevMonth,
                    )
                    CalendarArrow(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        description = stringResource(R.string.bus_calendar_next),
                        enabled = true,
                        onClick = onNextMonth,
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ORDERED_DOW.forEach { dow ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = dow.getDisplayName(TextStyle.SHORT_STANDALONE, LocaleBR).uppercase(LocaleBR).take(3),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                            color = TotemPalette.InkMuted,
                        )
                    }
                }
            }
            val firstOfMonth = visibleMonth.atDay(1)
            val leading = firstOfMonth.dayOfWeek.value % 7
            val days = visibleMonth.lengthOfMonth()
            val totalCells = ((leading + days + 6) / 7) * 7
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                var cell = 0
                while (cell < totalCells) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(7) { col ->
                            val dayNumber = cell + col - leading + 1
                            val cellMod = Modifier.weight(1f).aspectRatio(1.6f)
                            if (dayNumber in 1..days) {
                                val date = visibleMonth.atDay(dayNumber)
                                DayCell(
                                    modifier = cellMod,
                                    date = date,
                                    isSelected = date == selected,
                                    isHighlighted = date == highlighted,
                                    isToday = date == LocalDate.now(),
                                    selectable = !date.isBefore(minDate),
                                    onClick = { onSelect(date) },
                                )
                            } else {
                                Box(modifier = cellMod)
                            }
                        }
                    }
                    cell += 7
                }
            }
        }
    }
}

@Composable
private fun CalendarArrow(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        color = TotemPalette.Paper,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, TotemPalette.Hairline),
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .alpha(if (enabled) 1f else 0.35f)
            .let { if (enabled) it.clickable(onClick = onClick) else it },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = TotemPalette.Ink, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DayCell(
    modifier: Modifier,
    date: LocalDate,
    isSelected: Boolean,
    isHighlighted: Boolean,
    isToday: Boolean,
    selectable: Boolean,
    onClick: () -> Unit,
) {
    val bg = when {
        isSelected -> TotemPalette.Accent
        isHighlighted -> TotemPalette.AccentTint
        else -> Color.Transparent
    }
    val fg = when {
        isSelected -> TotemPalette.Paper
        !selectable -> TotemPalette.InkSoft
        isToday -> TotemPalette.Accent
        else -> TotemPalette.Ink
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .let { if (selectable) it.clickable(onClick = onClick) else it }
            .alpha(if (selectable || isSelected) 1f else 0.45f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
            ),
            color = fg,
        )
    }
}

/** Faixa de resumo da rota (origem → destino · data) usada no topo das telas do fluxo. */
@Composable
internal fun RouteBanner(
    origem: String,
    destino: String,
    detalhe: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TotemPalette.AccentTint,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusSm),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = TotemTheme.dimens.space16, vertical = TotemTheme.dimens.space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            Text(
                text = "$origem → $destino",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TotemPalette.Ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (detalhe != null) {
                Text(
                    text = detalhe,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TotemPalette.Accent,
                )
            }
        }
    }
}
