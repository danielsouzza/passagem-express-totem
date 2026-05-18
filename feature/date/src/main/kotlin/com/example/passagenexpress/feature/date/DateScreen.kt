package com.example.passagenexpress.feature.date

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passagenexpress.core.designsystem.component.TotemPrimaryButton
import com.example.passagenexpress.core.designsystem.component.TotemScreenScaffold
import com.example.passagenexpress.core.designsystem.component.TotemSecondaryButton
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val Locale_BR: Locale = Locale.forLanguageTag("pt-BR")
private val DAY_MONTH_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd 'de' MMMM", Locale_BR)

@Composable
fun DateScreen(
    onDateConfirmed: (LocalDate) -> Unit,
    onBack: () -> Unit,
    viewModel: DateViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DateContent(
        state = state,
        onDateSelected = viewModel::onDateSelected,
        onPrevMonth = viewModel::onPrevMonth,
        onNextMonth = viewModel::onNextMonth,
        onContinue = { state.selected?.let(onDateConfirmed) },
        onBack = onBack,
    )
}

@Composable
private fun DateContent(
    state: DateUiState,
    onDateSelected: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    TotemScreenScaffold(
        step = 2,
        title = stringRes(R.string.date_question),
        subtitle = stringRes(R.string.date_subhead),
        footer = {
            TotemSecondaryButton(text = stringRes(R.string.date_back), onClick = onBack)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TotemPrimaryButton(
                    text = stringRes(R.string.date_continue),
                    onClick = onContinue,
                    enabled = state.selected != null,
                )
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(
                TotemTheme.dimens.space24,
                Alignment.CenterVertically,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            QuickTiles(
                state = state,
                onDateSelected = onDateSelected,
            )
            OtherDateCard(
                state = state,
                onDateSelected = onDateSelected,
                onPrev = onPrevMonth,
                onNext = onNextMonth,
            )
        }
    }
}

@Composable
private fun QuickTiles(
    state: DateUiState,
    onDateSelected: (LocalDate) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
    ) {
        QuickTile(
            modifier = Modifier.weight(1f),
            label = stringRes(R.string.date_today),
            date = state.today,
            icon = Icons.Filled.CalendarMonth,
            selected = state.selected == state.today,
            onClick = { onDateSelected(state.today) },
        )
        QuickTile(
            modifier = Modifier.weight(1f),
            label = stringRes(R.string.date_tomorrow),
            date = state.tomorrow,
            icon = Icons.Filled.EventAvailable,
            selected = state.selected == state.tomorrow,
            onClick = { onDateSelected(state.tomorrow) },
        )
    }
}

@Composable
private fun QuickTile(
    modifier: Modifier = Modifier,
    label: String,
    date: LocalDate,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) TotemPalette.Accent else TotemPalette.AccentTint
    val titleColor = if (selected) TotemPalette.Paper else TotemPalette.Ink
    val subColor = if (selected) TotemPalette.Paper.copy(alpha = 0.85f) else TotemPalette.InkMuted

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(TotemTheme.dimens.radiusLg))
            .clickable(onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
    ) {
        Box(modifier = Modifier.padding(TotemTheme.dimens.space24)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = titleColor,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.height(TotemTheme.dimens.space8))
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = titleColor,
                )
                Text(
                    text = date.format(DAY_MONTH_FORMATTER),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = subColor,
                )
            }
            if (selected) {
                Surface(
                    color = TotemPalette.Paper.copy(alpha = 0.22f),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.TopEnd),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = TotemPalette.Paper,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OtherDateCard(
    state: DateUiState,
    onDateSelected: (LocalDate) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = TotemPalette.Paper,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
        border = BorderStroke(1.5.dp, TotemPalette.Hairline),
    ) {
        Column(
            modifier = Modifier.padding(TotemTheme.dimens.space24),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        ) {
            MonthHeader(state = state, onPrev = onPrev, onNext = onNext)
            WeekdayHeader()
            MonthGrid(state = state, onDateSelected = onDateSelected)
        }
    }
}

@Composable
private fun MonthHeader(
    state: DateUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val monthLabel = state.visibleMonth.month
        .getDisplayName(TextStyle.FULL, Locale_BR)
        .replaceFirstChar { it.uppercase(Locale_BR) } +
        " " + state.visibleMonth.year
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = TotemPalette.Ink,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringRes(R.string.date_other_date),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TotemPalette.Ink,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            ArrowSquare(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                description = stringRes(R.string.date_prev_month),
                onClick = onPrev,
            )
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TotemPalette.Ink,
            )
            ArrowSquare(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                description = stringRes(R.string.date_next_month),
                onClick = onNext,
            )
        }
    }
}

@Composable
private fun ArrowSquare(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        color = TotemPalette.Paper,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, TotemPalette.Hairline),
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = TotemPalette.Ink,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private val ORDERED_DOW: List<DayOfWeek> = listOf(
    DayOfWeek.SUNDAY,
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
)

@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space4),
    ) {
        ORDERED_DOW.forEach { dow ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = dow.getDisplayName(TextStyle.SHORT_STANDALONE, Locale_BR)
                        .uppercase(Locale_BR)
                        .take(3),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    ),
                    color = TotemPalette.InkMuted,
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    state: DateUiState,
    onDateSelected: (LocalDate) -> Unit,
) {
    val firstOfMonth = state.visibleMonth.atDay(1)
    // Sunday-first grid: offset = how many cells before day 1
    val leadingOffset = (firstOfMonth.dayOfWeek.value % 7) // SUN=0, MON=1, ..., SAT=6
    val daysInMonth = state.visibleMonth.lengthOfMonth()
    val totalCells = ((leadingOffset + daysInMonth + 6) / 7) * 7

    Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space4)) {
        var cell = 0
        while (cell < totalCells) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space4),
            ) {
                repeat(7) { col ->
                    val index = cell + col
                    val dayNumber = index - leadingOffset + 1
                    val cellModifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                    if (dayNumber in 1..daysInMonth) {
                        val date = state.visibleMonth.atDay(dayNumber)
                        DayCell(
                            modifier = cellModifier,
                            date = date,
                            selected = state.selected == date,
                            selectable = state.isSelectable(date),
                            isToday = date == state.today,
                            onClick = { onDateSelected(date) },
                        )
                    } else {
                        Box(modifier = cellModifier)
                    }
                }
            }
            cell += 7
        }
    }
}

@Composable
private fun DayCell(
    modifier: Modifier,
    date: LocalDate,
    selected: Boolean,
    selectable: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val bg = when {
        selected -> TotemPalette.Accent
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val fg = when {
        selected -> TotemPalette.Paper
        !selectable -> TotemPalette.InkSoft
        isToday -> TotemPalette.Accent
        else -> TotemPalette.Ink
    }
    val cellShape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(cellShape)
            .background(bg)
            .let { if (selectable) it.clickable(onClick = onClick) else it }
            .alpha(if (selectable || selected) 1f else 0.5f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = (-0.2).sp,
            ),
            color = fg,
        )
    }
}

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)

@Suppress("UnusedPrivateMember")
@Preview(name = "Date selection", widthDp = 800, heightDp = 1280)
@Composable
private fun DateScreenPreview() {
    val today = LocalDate.now()
    TotemTheme {
        DateContent(
            state = DateUiState(
                municipioDestinoNome = "Morro de São Paulo",
                today = today,
                selected = today,
                visibleMonth = YearMonth.from(today),
            ),
            onDateSelected = {},
            onPrevMonth = {},
            onNextMonth = {},
            onContinue = {},
            onBack = {},
        )
    }
}
