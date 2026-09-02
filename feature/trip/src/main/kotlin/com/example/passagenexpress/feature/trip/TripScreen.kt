package com.example.passagenexpress.feature.trip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBoatFilled
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passagenexpress.core.designsystem.component.ProvideDialogLocale
import com.example.passagenexpress.core.designsystem.component.TotemErrorState
import com.example.passagenexpress.core.designsystem.component.TotemLoading
import com.example.passagenexpress.core.designsystem.component.TotemPrimaryButton
import com.example.passagenexpress.core.designsystem.component.TotemScreenScaffold
import com.example.passagenexpress.core.designsystem.component.TotemSecondaryButton
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme
import com.example.passagenexpress.core.domain.model.Municipio
import com.example.passagenexpress.core.domain.model.TipoComodo
import com.example.passagenexpress.core.domain.model.Trecho
import com.example.passagenexpress.core.domain.model.precoBaseComDesconto
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TripScreen(
    onTripConfirmed: (Trecho) -> Unit,
    onBack: () -> Unit,
    viewModel: TripViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TripContent(
        state = state,
        onTripClick = onTripConfirmed,
        onJumpToProxima = viewModel::onJumpToProximaViagem,
        onRetry = viewModel::onRetry,
        onBack = onBack,
        onOpenDateFilter = viewModel::onOpenDateFilter,
        onOpenDestinoFilter = viewModel::onOpenDestinoFilter,
        onDismissFilters = viewModel::onDismissFilters,
        onSheetSelectDestino = viewModel::onSheetSelectDestino,
        onSheetSelectDate = viewModel::onSheetSelectDate,
        onSheetPrevMonth = viewModel::onSheetPrevMonth,
        onSheetNextMonth = viewModel::onSheetNextMonth,
        onApplyFilters = viewModel::onApplyFilters,
    )
}

@Composable
private fun TripContent(
    state: TripUiState,
    onTripClick: (Trecho) -> Unit,
    onJumpToProxima: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onOpenDateFilter: () -> Unit = {},
    onOpenDestinoFilter: () -> Unit = {},
    onDismissFilters: () -> Unit = {},
    onSheetSelectDestino: (String, String) -> Unit = { _, _ -> },
    onSheetSelectDate: (LocalDate) -> Unit = {},
    onSheetPrevMonth: () -> Unit = {},
    onSheetNextMonth: () -> Unit = {},
    onApplyFilters: () -> Unit = {},
) {
    val subtitle = when {
        state.origemNome.isNotEmpty() && state.destinoNome.isNotEmpty() ->
            stringResFmt(R.string.trip_subtitle_route, state.origemNome, state.destinoNome)
        state.origemNome.isNotEmpty() ->
            stringResFmt(R.string.trip_subtitle_origin, state.origemNome)
        else -> null
    }

    TotemScreenScaffold(
        step = 1,
        totalSteps = 4,
        // Cancelar só a partir do passo 2 (quando já há reserva/dados); no passo 1 basta Voltar/Início.
        showCancel = false,
        title = stringRes(R.string.trip_title),
        subtitle = subtitle,
        footer = {
            TotemSecondaryButton(text = stringRes(R.string.trip_back), onClick = onBack)
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FilterButtonsRow(
                dateLabel = if (state.dataIrrestrita) {
                    stringRes(R.string.trip_filter_upcoming)
                } else {
                    formatCardDate(state.date)
                },
                destinoLabel = state.destinoNome.ifEmpty { stringRes(R.string.trip_filter_all_destinations) },
                onDateClick = onOpenDateFilter,
                onDestinoClick = onOpenDestinoFilter,
            )
            when (val trips = state.trips) {
                TripsState.Loading -> TotemLoading(label = stringRes(R.string.trip_loading))
                is TripsState.Error -> TotemErrorState(
                    title = stringRes(R.string.trip_error_title),
                    message = trips.message,
                    actionLabel = stringRes(R.string.trip_retry),
                    onAction = onRetry,
                )
                is TripsState.Loaded -> {
                    if (trips.trechos.isEmpty()) {
                        EmptyWithProxima(
                            proxima = trips.proximaViagem,
                            onJumpToProxima = onJumpToProxima,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
                            contentPadding = PaddingValues(vertical = TotemTheme.dimens.space12),
                        ) {
                            itemsIndexed(
                                items = trips.trechos,
                                key = { _, t -> t.rowKey() },
                            ) { index, trecho ->
                                StaggeredItem(index = index) {
                                    TripCard(
                                        trecho = trecho,
                                        onClick = { onTripClick(trecho) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val sheet = state.filterSheet
    if (sheet !is FilterSheetState.Hidden) {
        FilterDialog(
            sheet = sheet,
            onDismiss = onDismissFilters,
            onSelectDestino = onSheetSelectDestino,
            onSelectDate = onSheetSelectDate,
            onPrevMonth = onSheetPrevMonth,
            onNextMonth = onSheetNextMonth,
            onApply = onApplyFilters,
        )
    }
}

@Composable
private fun FilterDialog(
    sheet: FilterSheetState,
    onDismiss: () -> Unit,
    onSelectDestino: (String, String) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onApply: () -> Unit,
) {
    val dialogTitle = when (sheet) {
        is FilterSheetState.Open ->
            if (sheet.mode == FilterMode.Date) stringRes(R.string.trip_filter_date_title)
            else stringRes(R.string.trip_filter_destino_title)
        else -> stringRes(R.string.trip_filter_destino_title)
    }
    Dialog(onDismissRequest = onDismiss) {
        ProvideDialogLocale {
        Surface(
            color = TotemPalette.Paper,
            shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp, max = 820.dp),
        ) {
            Column(modifier = Modifier.padding(TotemTheme.dimens.space24)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = dialogTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = TotemPalette.Ink,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringRes(R.string.trip_filter_cancel),
                            tint = TotemPalette.Ink,
                        )
                    }
                }
                Spacer(Modifier.height(TotemTheme.dimens.space16))
                when (sheet) {
                    FilterSheetState.Hidden -> Unit
                    FilterSheetState.Loading -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = TotemTheme.dimens.space24),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = TotemPalette.Accent)
                            Spacer(Modifier.height(TotemTheme.dimens.space12))
                            Text(
                                text = stringRes(R.string.trip_filter_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TotemPalette.InkMuted,
                            )
                        }
                    }
                    is FilterSheetState.Open -> {
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
                        ) {
                            when (sheet.mode) {
                                FilterMode.Date -> CalendarPicker(
                                    today = LocalDate.now(),
                                    selected = sheet.selectedDate,
                                    visibleMonth = sheet.visibleMonth,
                                    onSelect = onSelectDate,
                                    onPrevMonth = onPrevMonth,
                                    onNextMonth = onNextMonth,
                                )
                                FilterMode.Destino -> DestinationList(
                                    destinations = sheet.destinations,
                                    selectedSlug = sheet.selectedDestinoSlug,
                                    onSelect = onSelectDestino,
                                )
                            }
                            if (sheet.error != null) {
                                Text(
                                    text = sheet.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        Spacer(Modifier.height(TotemTheme.dimens.space20))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
                        ) {
                            TotemSecondaryButton(
                                text = stringRes(R.string.trip_filter_cancel),
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                            )
                            TotemPrimaryButton(
                                text = stringRes(R.string.trip_filter_apply),
                                onClick = onApply,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun FilterButtonsRow(
    dateLabel: String,
    destinoLabel: String,
    onDateClick: () -> Unit,
    onDestinoClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = TotemTheme.dimens.space12),
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
    ) {
        FilterPillButton(
            icon = Icons.Filled.CalendarMonth,
            label = stringRes(R.string.trip_filter_date),
            value = dateLabel,
            onClick = onDateClick,
            modifier = Modifier.weight(1f),
        )
        FilterPillButton(
            icon = Icons.Filled.LocationOn,
            label = stringRes(R.string.trip_filter_destination),
            value = destinoLabel,
            onClick = onDestinoClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FilterPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
        color = TotemPalette.Paper,
        border = BorderStroke(1.5.dp, TotemPalette.Hairline),
        modifier = modifier
            .clip(RoundedCornerShape(TotemTheme.dimens.radiusLg))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = TotemTheme.dimens.space20,
                vertical = TotemTheme.dimens.space16,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TotemPalette.Accent,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    ),
                    color = TotemPalette.InkMuted,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = TotemPalette.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DestinationList(
    destinations: List<Municipio>,
    selectedSlug: String,
    onSelect: (String, String) -> Unit,
) {
    if (destinations.isEmpty()) {
        Text(
            text = stringRes(R.string.trip_filter_destinations_empty),
            style = MaterialTheme.typography.titleMedium,
            color = TotemPalette.InkMuted,
            modifier = Modifier.padding(vertical = TotemTheme.dimens.space12),
        )
        return
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        DestinationRow(
            nome = stringRes(R.string.trip_filter_all_destinations),
            selected = selectedSlug.isEmpty(),
            onClick = { onSelect("", "") },
        )
        HorizontalDivider(
            color = TotemPalette.Hairline,
            modifier = Modifier.padding(horizontal = TotemTheme.dimens.space20),
        )
        destinations.forEachIndexed { index, m ->
            DestinationRow(
                nome = m.nome,
                selected = m.slug == selectedSlug,
                onClick = { onSelect(m.slug, m.nome) },
            )
            if (index != destinations.lastIndex) {
                HorizontalDivider(
                    color = TotemPalette.Hairline,
                    modifier = Modifier.padding(horizontal = TotemTheme.dimens.space20),
                )
            }
        }
    }
}

@Composable
private fun DestinationRow(nome: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = TotemTheme.dimens.space20,
                vertical = TotemTheme.dimens.space16,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
    ) {
        Text(
            text = nome,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = if (selected) TotemPalette.Accent else TotemPalette.Ink,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = TotemPalette.Accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private val LocaleBR: Locale = Locale.forLanguageTag("pt-BR")
private val ORDERED_DOW: List<DayOfWeek> = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

@Composable
private fun CalendarPicker(
    today: LocalDate,
    selected: LocalDate,
    visibleMonth: YearMonth,
    onSelect: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = TotemPalette.Paper,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
        border = BorderStroke(1.5.dp, TotemPalette.Hairline),
    ) {
        Column(
            modifier = Modifier.padding(TotemTheme.dimens.space12),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
        ) {
            CalendarHeader(
                visibleMonth = visibleMonth,
                onPrev = onPrevMonth,
                onNext = onNextMonth,
            )
            CalendarWeekdays()
            CalendarGrid(
                visibleMonth = visibleMonth,
                today = today,
                selected = selected,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun CalendarHeader(
    visibleMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val monthLabel = visibleMonth.month
        .getDisplayName(TextStyle.FULL, LocaleBR)
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
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = TotemPalette.Ink,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TotemPalette.Ink,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
        ) {
            CalendarArrow(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                description = stringRes(R.string.trip_prev_date),
                onClick = onPrev,
            )
            CalendarArrow(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                description = stringRes(R.string.trip_next_date),
                onClick = onNext,
            )
        }
    }
}

@Composable
private fun CalendarArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

@Composable
private fun CalendarWeekdays() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ORDERED_DOW.forEach { dow ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = dow.getDisplayName(TextStyle.SHORT_STANDALONE, LocaleBR)
                        .uppercase(LocaleBR).take(3),
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
private fun CalendarGrid(
    visibleMonth: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    val firstOfMonth = visibleMonth.atDay(1)
    val leadingOffset = firstOfMonth.dayOfWeek.value % 7
    val daysInMonth = visibleMonth.lengthOfMonth()
    val totalCells = ((leadingOffset + daysInMonth + 6) / 7) * 7

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        var cell = 0
        while (cell < totalCells) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                repeat(7) { col ->
                    val index = cell + col
                    val dayNumber = index - leadingOffset + 1
                    val cellModifier = Modifier
                        .weight(1f)
                        .aspectRatio(1.4f)
                    if (dayNumber in 1..daysInMonth) {
                        val date = visibleMonth.atDay(dayNumber)
                        val selectable = !date.isBefore(today)
                        CalendarDayCell(
                            modifier = cellModifier,
                            date = date,
                            isSelected = date == selected,
                            isToday = date == today,
                            selectable = selectable,
                            onClick = { onSelect(date) },
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
private fun CalendarDayCell(
    modifier: Modifier,
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    selectable: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) TotemPalette.Accent else Color.Transparent
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
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = (-0.2).sp,
            ),
            color = fg,
        )
    }
}

private const val MAX_STAGGER_INDEX = 8

@Composable
private fun StaggeredItem(index: Int, content: @Composable () -> Unit) {
    val visibility = remember(index) {
        MutableTransitionState(false).apply { targetState = true }
    }
    // Capa o stagger nos primeiros itens pra lista não demorar a aparecer por inteiro.
    val staggerDelay = index.coerceAtMost(MAX_STAGGER_INDEX) * 45
    AnimatedVisibility(
        visibleState = visibility,
        enter = fadeIn(tween(260, delayMillis = staggerDelay)) +
            slideInVertically(
                animationSpec = tween(320, delayMillis = staggerDelay),
                initialOffsetY = { it / 5 },
            ),
    ) {
        content()
    }
}

@Composable
private fun TripCard(
    trecho: Trecho,
    onClick: () -> Unit,
) {
    val borderColor = TotemPalette.Hairline
    val borderWidth = 1.5.dp
    val nameColor = TotemPalette.Ink
    val chipBg = TotemPalette.AccentTint
    val chipFg = TotemPalette.Accent
    val type = vesselType(trecho.embarcacao)
    val priceWithDiscount = trecho.precoBaseComDesconto() + trecho.taxaDeEmbarque
    val priceOriginal = trecho.valor + trecho.taxaDeEmbarque
    val hasDiscount = trecho.desconto != null && trecho.desconto!!.valor > 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TotemTheme.dimens.radiusLg))
            .clickable(onClick = onClick),
        color = TotemPalette.Paper,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TotemTheme.dimens.space20,
                    vertical = TotemTheme.dimens.space20,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space20),
        ) {
            VesselChip(type = type, bg = chipBg, fg = chipFg)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "${trecho.municipioOrigem.nome} → ${trecho.municipioDestino.nome}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                    ),
                    color = nameColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = trecho.embarcacao.ifEmpty { stringRes(R.string.trip_title) },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = TotemPalette.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatCardDate(trecho.dataEmbarque),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp,
                    ),
                    color = TotemPalette.InkMuted,
                )
            }
            TimeStat(value = formatTime(trecho.horario), label = stringRes(R.string.trip_label_departure))
            TimeStat(
                value = formatDuration(trecho.tempoViagemMinutos),
                label = stringRes(R.string.trip_label_duration),
            )
            VerticalDivider()
            PriceStat(
                value = formatMoney(priceWithDiscount),
                originalValue = if (hasDiscount) formatMoney(priceOriginal) else null,
            )
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .size(width = 1.dp, height = 40.dp)
            .background(TotemPalette.Hairline),
    )
}

@Composable
private fun TimeStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = (-0.5).sp,
            ),
            color = TotemPalette.Ink,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
            color = TotemPalette.InkMuted,
        )
    }
}

@Composable
private fun PriceStat(value: String, originalValue: String? = null) {
    Column(horizontalAlignment = Alignment.End) {
        if (originalValue != null) {
            Text(
                text = originalValue,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                ),
                color = TotemPalette.InkMuted,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = (-0.5).sp,
            ),
            color = TotemPalette.Accent,
        )
        Text(
            text = stringRes(R.string.trip_label_pix).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
            color = TotemPalette.InkMuted,
        )
    }
}

@Composable
private fun VesselChip(
    type: VesselType,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color,
) {
    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusSm),
        modifier = Modifier.size(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            VesselIcon(type = type)
        }
    }
}

@Composable
private fun VesselIcon(type: VesselType) {
    val size = 28.dp
    when (type) {
        VesselType.Ferryboat -> Icon(
            imageVector = Icons.Filled.DirectionsBoat,
            contentDescription = null,
            modifier = Modifier.size(size),
        )
        VesselType.Lancha -> Icon(
            imageVector = Icons.Filled.Sailing,
            contentDescription = null,
            modifier = Modifier.size(size),
        )
        VesselType.Catamara -> Icon(
            imageVector = Icons.Filled.DirectionsBoatFilled,
            contentDescription = null,
            modifier = Modifier.size(size),
        )
        VesselType.Navio -> Icon(
            imageVector = Icons.Filled.Anchor,
            contentDescription = null,
            modifier = Modifier.size(size),
        )
    }
}

private enum class VesselType(val label: String) {
    Ferryboat("FERRYBOAT"),
    Lancha("LANCHA"),
    Catamara("CATAMARÃ"),
    Navio("NAVIO"),
}

private fun vesselType(embarcacao: String): VesselType {
    val lower = embarcacao.lowercase()
    return when {
        "lancha" in lower -> VesselType.Lancha
        "catamarã" in lower || "catamaran" in lower -> VesselType.Catamara
        "navio" in lower -> VesselType.Navio
        else -> VesselType.Ferryboat
    }
}

@Composable
private fun EmptyWithProxima(
    proxima: Trecho?,
    onJumpToProxima: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = TotemTheme.dimens.space24),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            // Ícone tonal no topo — comunica visualmente o "sem viagens" antes do texto.
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(TotemPalette.AccentTint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.EventBusy,
                    contentDescription = null,
                    tint = TotemPalette.Accent,
                    modifier = Modifier.size(56.dp),
                )
            }
            Spacer(Modifier.height(TotemTheme.dimens.space8))
            Text(
                text = stringRes(R.string.trip_empty_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = TotemPalette.Ink,
            )
            Text(
                text = stringRes(R.string.trip_empty_message),
                style = MaterialTheme.typography.bodyLarge,
                color = TotemPalette.InkMuted,
            )
            if (proxima != null) {
                Spacer(Modifier.height(TotemTheme.dimens.space24))
                Text(
                    text = stringRes(R.string.trip_next_available_title).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp,
                    ),
                    color = TotemPalette.Accent,
                )
                Text(
                    text = formatProximaDate(proxima.dataEmbarque),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = TotemPalette.Ink,
                )
                Spacer(Modifier.height(TotemTheme.dimens.space12))
                TotemPrimaryButton(
                    text = stringRes(R.string.trip_next_available_action),
                    onClick = onJumpToProxima,
                    accent = true,
                )
            }
        }
    }
}

private fun formatTime(time: LocalTime): String =
    time.format(DateTimeFormatter.ofPattern("HH:mm"))

// Mesmo formato do web (formatarTempoViagem): "48H00", "02H30".
private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "%02dH%02d".format(h, m)
}

private fun formatMoney(value: Double): String {
    val formatted = String.format(Locale.forLanguageTag("pt-BR"), "%,.2f", value)
    return "R$ $formatted"
}

private fun formatProximaDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", Locale.forLanguageTag("pt-BR")))
        .replaceFirstChar { it.uppercase(Locale.forLanguageTag("pt-BR")) }

private fun formatCardDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE, dd/MM", LocaleBR))
        .replaceFirstChar { it.uppercase(LocaleBR) }

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)

@Composable
private fun stringResFmt(id: Int, vararg args: Any): String =
    androidx.compose.ui.res.stringResource(id, *args)

@Suppress("UnusedPrivateMember")
@Preview(name = "Trip list", widthDp = 800, heightDp = 1280)
@Composable
private fun TripScreenPreview() {
    val sample = Trecho(
        id = 1L,
        idViagem = 10L,
        dataEmbarque = LocalDate.now(),
        horario = LocalTime.of(7, 30),
        tempoViagemMinutos = 130,
        valor = 95.0,
        taxaDeEmbarque = 8.0,
        embarcacao = "Catamarã Boa Vista",
        poltronaLivre = false,
        linhas = 8,
        colunas = 6,
        tiposComodos = listOf(TipoComodo(1, "Poltrona")),
        municipioOrigem = Municipio(slug = "salvador", nome = "Salvador", uf = "BA"),
        municipioDestino = Municipio(slug = "morro-de-sao-paulo", nome = "Morro de São Paulo", uf = "BA"),
    )
    TotemTheme {
        TripContent(
            state = TripUiState(
                origemNome = "Porto de Salvador",
                destinoNome = "Morro de São Paulo",
                date = LocalDate.now(),
                trips = TripsState.Loaded(listOf(sample, sample.copy(id = 2L, horario = LocalTime.of(11, 0)))),
            ),
            onTripClick = {},
            onJumpToProxima = {},
            onRetry = {},
            onBack = {},
        )
    }
}
