package com.example.passagenexpress.feature.room

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBoatFilled
import androidx.compose.material.icons.filled.KingBed
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passagenexpress.core.designsystem.component.TotemErrorState
import com.example.passagenexpress.core.designsystem.component.TotemLoading
import com.example.passagenexpress.core.designsystem.component.TotemPrimaryButton
import com.example.passagenexpress.core.designsystem.component.TotemScreenScaffold
import com.example.passagenexpress.core.designsystem.component.TotemSecondaryButton
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme
import com.example.passagenexpress.core.domain.model.Comodo
import com.example.passagenexpress.core.domain.model.Trecho
import com.example.passagenexpress.core.domain.model.precoBaseComDesconto
import com.example.passagenexpress.core.domain.repository.InicioVenda
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RoomScreen(
    onSaleStarted: (InicioVenda, String) -> Unit,
    onBack: () -> Unit,
    viewModel: RoomViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sale = state.saleStarted
    if (sale != null) {
        androidx.compose.runtime.LaunchedEffect(sale) { onSaleStarted(sale, viewModel.rawTrechoArg) }
    }
    RoomContent(
        state = state,
        onSelectComodo = viewModel::onClickComodoIndividual,
        onAddTipo = viewModel::onAddTipo,
        onIncrementTipo = viewModel::onIncrementTipo,
        onDecrementTipo = viewModel::onDecrementTipo,
        onRemoveTipo = viewModel::onRemoveTipo,
        onContinue = viewModel::onAvancar,
        onRetry = viewModel::onRetry,
        onBack = {
            viewModel.onAbandonScreen()
            onBack()
        },
        onDismissError = viewModel::dismissError,
    )
}

@Composable
private fun RoomContent(
    state: RoomUiState,
    onSelectComodo: (Comodo) -> Unit,
    onAddTipo: (Long) -> Unit,
    onIncrementTipo: (Long) -> Unit,
    onDecrementTipo: (Long) -> Unit,
    onRemoveTipo: (Long) -> Unit,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
) {
    val trecho = state.trecho

    val canContinue = trecho != null && !state.selecionados.isEmpty() && !state.startingSale

    TotemScreenScaffold(
        step = 4,
        title = stringRes(R.string.room_title),
        footer = {
            TotemSecondaryButton(text = stringRes(R.string.room_back), onClick = onBack)
            Spacer(Modifier.width(TotemTheme.dimens.space12))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TotemPrimaryButton(
                    text = if (state.startingSale) stringRes(R.string.room_starting_sale)
                    else stringRes(R.string.room_continue),
                    onClick = onContinue,
                    enabled = canContinue,
                )
            }
        },
    ) {
        when {
            trecho == null || state.content is RoomContentState.Loading ->
                TotemLoading(label = stringRes(R.string.room_loading))
            state.content is RoomContentState.Error -> TotemErrorState(
                title = stringRes(R.string.room_error_title),
                message = state.content.message,
                actionLabel = stringRes(R.string.room_retry),
                onAction = onRetry,
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
            ) {
                TripInfoBanner(trecho = trecho)
                if (state.content is RoomContentState.Poltronas) {
                    Legend()
                }
                if (trecho.poltronaLivre && state.content is RoomContentState.Livres) {
                    FreeSeatingNotice()
                }
                when (val c = state.content) {
                    is RoomContentState.Poltronas -> {
                        SeatGrid(
                            trecho = trecho,
                            assentos = c.assentos,
                            selectedIds = state.selecionados.porId.map { it.id }.toSet(),
                            onClick = onSelectComodo,
                        )
                        val items = buildRoomGridItems(
                            camarotes = emptyList(),
                            tipos = c.tiposLivres,
                        )
                        if (items.isNotEmpty()) {
                            RoomGrid(
                                items = items,
                                trecho = trecho,
                                selecionados = state.selecionados,
                                onSelectComodo = onSelectComodo,
                                onAdd = onAddTipo,
                                onIncrement = onIncrementTipo,
                                onDecrement = onDecrementTipo,
                            )
                        }
                    }
                    is RoomContentState.Camarotes -> {
                        RoomGrid(
                            items = buildRoomGridItems(
                                camarotes = c.camarotes,
                                tipos = c.tiposLivres,
                            ),
                            trecho = trecho,
                            selecionados = state.selecionados,
                            onSelectComodo = onSelectComodo,
                            onAdd = onAddTipo,
                            onIncrement = onIncrementTipo,
                            onDecrement = onDecrementTipo,
                        )
                    }
                    is RoomContentState.Livres -> RoomGrid(
                        items = buildRoomGridItems(
                            camarotes = emptyList(),
                            tipos = c.tipos,
                        ),
                        trecho = trecho,
                        selecionados = state.selecionados,
                        onSelectComodo = onSelectComodo,
                        onAdd = onAddTipo,
                        onIncrement = onIncrementTipo,
                        onDecrement = onDecrementTipo,
                    )
                    else -> Unit
                }
                if (state.erroTransitorio != null) {
                    TransientErrorBanner(
                        message = state.erroTransitorio,
                        onDismiss = onDismissError,
                    )
                }
                Spacer(Modifier.height(TotemTheme.dimens.space12))
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space32, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top,
    ) {
        LegendDot(color = LegendColors.Unavailable, label = stringRes(R.string.room_legend_unavailable))
        LegendDot(color = LegendColors.Selected, label = stringRes(R.string.room_legend_selected))
        LegendDot(color = LegendColors.Free, label = stringRes(R.string.room_legend_free))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = TotemPalette.InkMuted,
        )
    }
}

@Composable
private fun FreeSeatingNotice() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(TotemTheme.dimens.space20),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space4),
        ) {
            Text(
                text = stringRes(R.string.room_free_seating_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringRes(R.string.room_free_seating_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun SeatGrid(
    trecho: Trecho,
    assentos: List<Comodo>,
    selectedIds: Set<Long>,
    onClick: (Comodo) -> Unit,
) {
    val seatById = assentos.associateBy { (it.coluna ?: 0) to (it.linha ?: 0) }
    val rows = trecho.colunas
    val cols = trecho.linhas
    val aisleAfterCol = if (cols >= 4) cols / 2 else 0 // visual aisle gap for 4+ columns

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (rowIdx in 1..rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (colIdx in cols downTo 1) {
                    val key =  (rowIdx to colIdx)
                    val seat = seatById[key]
                    SeatChip(
                        comodo = seat,
                        selected = seat != null && seat.id in selectedIds,
                        onClick = if (seat != null && !seat.isOcupado) {
                            { onClick(seat) }
                        } else null,
                        modifier = Modifier.size(80.dp),
                    )

                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SeatChip(
    comodo: Comodo?,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        comodo == null -> Color.Transparent
        comodo.isOcupado -> LegendColors.Unavailable
        selected -> LegendColors.Selected
        else -> LegendColors.Free
    }
    val fg = when {
        comodo == null -> Color.Transparent
        comodo.isOcupado -> SeatUnavailableText
        selected -> SeatSelectedText
        else -> SeatFreeText
    }
    val label = comodo?.numeracao?.let { String.format(Locale.US, "%02d", it) }.orEmpty()
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        modifier = modifier
            .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = fg,
                )
            }
        }
    }
}

private const val TIPO_ID_VEICULO = 6L

private sealed interface RoomGridItem {
    val sortKey: Int
    val gridKey: String

    data class Individual(val comodo: Comodo) : RoomGridItem {
        override val sortKey: Int = sortKeyFor(comodo.nome.orEmpty())
        override val gridKey: String = "c-${comodo.id}"
    }

    data class Tipo(val bucket: TipoComodoBucket) : RoomGridItem {
        override val sortKey: Int = sortKeyFor(bucket.tipo.nome)
        override val gridKey: String = "t-${bucket.tipo.id}"
    }
}

/** Lower sortKey = renders first. "rede" always wins. */
private fun sortKeyFor(name: String): Int = when {
    "rede" in name.lowercase() -> 0
    else -> 1
}

private fun buildRoomGridItems(
    camarotes: List<Comodo>,
    tipos: List<TipoComodoBucket>,
): List<RoomGridItem> {
    val tipoItems = tipos
        .filter { it.tipo.id != TIPO_ID_VEICULO }
        .map { RoomGridItem.Tipo(it) }
    val individualItems = camarotes.map { RoomGridItem.Individual(it) }
    return (tipoItems + individualItems).sortedBy { it.sortKey }
}

@Composable
private fun RoomGrid(
    items: List<RoomGridItem>,
    trecho: Trecho,
    selecionados: SelecaoState,
    onSelectComodo: (Comodo) -> Unit,
    onAdd: (Long) -> Unit,
    onIncrement: (Long) -> Unit,
    onDecrement: (Long) -> Unit,
) {
    val selectedIds = selecionados.porId.map { it.id }.toSet()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 900.dp),
    ) {
        items(items, key = { it.gridKey }) { item ->
            when (item) {
                is RoomGridItem.Individual -> {
                    val c = item.comodo
                    RoomCard(
                        name = (c.nome ?: c.numeracao?.toString().orEmpty()).uppercase(),
                        capacity = c.quantidade,
                        price = formatMoney(trecho.precoBaseComDesconto(c.valor) + trecho.taxaDeEmbarque),
                        disabled = c.isOcupado,
                        selected = c.id in selectedIds,
                        onClick = { onSelectComodo(c) },
                    )
                }
                is RoomGridItem.Tipo -> {
                    val bucket = item.bucket
                    val qty = selecionados.porTipo[bucket.tipo.id] ?: 0
                    RoomCard(
                        name = bucket.tipo.nome.uppercase(),
                        capacity = bucket.livre.quantidade,
                        price = formatMoney(trecho.precoBaseComDesconto() + trecho.taxaDeEmbarque),
                        disabled = bucket.livre.quantidade == 0,
                        selected = qty > 0,
                        quantity = qty,
                        onClick = { if (qty == 0) onAdd(bucket.tipo.id) },
                        onIncrement = { onIncrement(bucket.tipo.id) },
                        onDecrement = { onDecrement(bucket.tipo.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomCard(
    name: String,
    capacity: Int,
    price: String,
    disabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    quantity: Int = 0,
    onIncrement: (() -> Unit)? = null,
    onDecrement: (() -> Unit)? = null,
) {
    val borderColor = when {
        selected -> TotemPalette.Accent
        disabled -> TotemPalette.Hairline
        else -> TotemPalette.Hairline
    }
    val borderWidth = if (selected) 2.dp else 1.5.dp
    val bg = if (selected) TotemPalette.AccentTint else TotemPalette.Paper
    val nameColor = if (disabled) TotemPalette.InkMuted else TotemPalette.Ink
    val priceColor = if (selected) TotemPalette.Accent else TotemPalette.Ink

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (disabled) 0.5f else 1f)
            .clip(RoundedCornerShape(TotemTheme.dimens.radiusLg))
            .clickable(enabled = !disabled, onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(TotemTheme.dimens.space20),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Icon(
                    imageVector = roomIcon(name),
                    contentDescription = null,
                    tint = if (selected) TotemPalette.Accent else TotemPalette.Ink,
                    modifier = Modifier.size(28.dp),
                )
                CapacityBadge(capacity = capacity)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = nameColor,
                )
                Text(
                    text = stringRes(R.string.room_value_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                    ),
                    color = TotemPalette.InkMuted,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = priceColor,
                )
                if (selected && onIncrement != null && onDecrement != null) {
                    QtyStepper(
                        quantity = quantity,
                        onIncrement = onIncrement,
                        onDecrement = onDecrement,
                    )
                }
            }
        }
    }
}

@Composable
private fun CapacityBadge(capacity: Int) {
    Surface(
        color = TotemPalette.PaperDim,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusPill),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = TotemPalette.InkMuted,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = capacity.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TotemPalette.InkMuted,
            )
        }
    }
}

@Composable
private fun QtyStepper(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StepperButton(
            icon = Icons.Filled.Remove,
            filled = false,
            description = stringRes(R.string.room_decrement),
            onClick = onDecrement,
        )
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TotemPalette.Ink,
            modifier = Modifier.widthIn(min = 20.dp),
            textAlign = TextAlign.Center,
        )
        StepperButton(
            icon = Icons.Filled.Add,
            filled = true,
            description = stringRes(R.string.room_increment),
            onClick = onIncrement,
        )
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    filled: Boolean,
    description: String,
    onClick: () -> Unit,
) {
    val bg = if (filled) TotemPalette.Accent else TotemPalette.Paper
    val fg = if (filled) TotemPalette.Paper else TotemPalette.Ink
    val border = if (filled) null else BorderStroke(1.5.dp, TotemPalette.Hairline)
    Surface(
        color = bg,
        contentColor = fg,
        shape = CircleShape,
        border = border,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun roomIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    val lower = name.lowercase()
    return when {
        "rede" in lower -> Icons.Filled.Waves
        "camarote" in lower -> Icons.Filled.KingBed
        "suíte" in lower || "suite" in lower -> Icons.Filled.Bed
        "poltrona" in lower -> Icons.Filled.AirlineSeatReclineNormal
        else -> Icons.Filled.Bed
    }
}

@Composable
private fun SelectedSection(
    state: RoomUiState,
    onSelectComodo: (Comodo) -> Unit,
    onIncrement: (Long) -> Unit,
    onDecrement: (Long) -> Unit,
    onRemoveTipo: (Long) -> Unit,
) {
    val trecho = state.trecho ?: return
    val tiposComodos = trecho.tiposComodos.associateBy { it.id }
    Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8)) {
        SectionTitle(stringRes(R.string.room_selected_title))
        if (state.selecionados.isEmpty()) {
            Text(
                text = stringRes(R.string.room_selected_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            state.selecionados.porId.forEach { comodo ->
                SelectedComodoRow(
                    label = comodo.nome ?: comodo.numeracao?.let { String.format(Locale.US, "%02d", it) }.orEmpty(),
                    valor = formatMoney(trecho.precoBaseComDesconto(comodo.valor) + trecho.taxaDeEmbarque),
                    onRemove = { onSelectComodo(comodo) },
                )
            }
            state.selecionados.porTipo.forEach { (tipoId, qtd) ->
                val tipo = tiposComodos[tipoId] ?: return@forEach
                SelectedTipoRow(
                    nome = tipo.nome,
                    quantidade = qtd,
                    valorUnit = trecho.precoBaseComDesconto() + trecho.taxaDeEmbarque,
                    onIncrement = { onIncrement(tipoId) },
                    onDecrement = { onDecrement(tipoId) },
                    onRemove = { onRemoveTipo(tipoId) },
                )
            }
        }
    }
}

@Composable
private fun SelectedComodoRow(
    label: String,
    valor: String,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = LegendColors.Selected,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TotemTheme.dimens.space16, vertical = TotemTheme.dimens.space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringRes(R.string.room_remove),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SelectedTipoRow(
    nome: String,
    quantidade: Int,
    valorUnit: Double,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = LegendColors.Selected,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TotemTheme.dimens.space16, vertical = TotemTheme.dimens.space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = nome,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Surface(
                shape = CircleShape,
                color = Color.White,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    IconButton(onClick = onDecrement, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = stringRes(R.string.room_decrement),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = quantidade.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    IconButton(onClick = onIncrement, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringRes(R.string.room_increment),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Text(
                text = formatMoney(valorUnit * quantidade),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = TotemTheme.dimens.space8),
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringRes(R.string.room_remove),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun TransientErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TotemTheme.dimens.space16, vertical = TotemTheme.dimens.space8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

private object LegendColors {
    val Free = TotemPalette.PaperDim
    val Selected = TotemPalette.Accent
    val Unavailable = TotemPalette.Ink
}

private val SeatFreeText = TotemPalette.Ink
private val SeatSelectedText = TotemPalette.Paper
private val SeatUnavailableText = TotemPalette.Paper

private fun formatMoney(value: Double): String {
    val formatted = String.format(Locale.forLanguageTag("pt-BR"), "%,.2f", value)
    return "R$ $formatted"
}

@Composable
private fun TripInfoBanner(trecho: Trecho) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = TotemPalette.Paper,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
        border = BorderStroke(1.5.dp, TotemPalette.Hairline),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = TotemTheme.dimens.space20,
                vertical = TotemTheme.dimens.space16,
            ),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
            ) {
                VesselChip(embarcacao = trecho.embarcacao)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = trecho.embarcacao.ifEmpty { "EMBARCAÇÃO" },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.2).sp,
                        ),
                        color = TotemPalette.Ink,
                        maxLines = 1,
                    )
                    Text(
                        text = vesselLabel(trecho.embarcacao),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                        ),
                        color = TotemPalette.InkMuted,
                    )
                }
            }
            RouteRow(
                origem = trecho.municipioOrigem.nome,
                destino = trecho.municipioDestino.nome,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
            ) {
                InfoPill(
                    icon = Icons.Filled.CalendarToday,
                    label = formatBannerDate(trecho.dataEmbarque),
                    modifier = Modifier.weight(1f),
                )
                InfoPill(
                    icon = Icons.Filled.Schedule,
                    label = formatBannerTime(trecho.horario),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun VesselChip(embarcacao: String) {
    Surface(
        color = TotemPalette.AccentTint,
        contentColor = TotemPalette.Accent,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusSm),
        modifier = Modifier.size(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = vesselIcon(embarcacao),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun RouteRow(origem: String, destino: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
    ) {
        Text(
            text = origem.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp,
            ),
            color = TotemPalette.Ink,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TotemPalette.Accent,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = destino.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp,
            ),
            color = TotemPalette.Ink,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start,
            maxLines = 1,
        )
    }
}

@Composable
private fun InfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = TotemPalette.AccentTint,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusPill),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TotemPalette.Accent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp,
                ),
                color = TotemPalette.Ink,
                maxLines = 1,
            )
        }
    }
}

private fun vesselIcon(embarcacao: String): androidx.compose.ui.graphics.vector.ImageVector {
    val lower = embarcacao.lowercase()
    return when {
        "lancha" in lower -> Icons.Filled.Sailing
        "catamarã" in lower || "catamaran" in lower -> Icons.Filled.DirectionsBoatFilled
        "navio" in lower -> Icons.Filled.Anchor
        else -> Icons.Filled.DirectionsBoat
    }
}

private fun vesselLabel(embarcacao: String): String {
    val lower = embarcacao.lowercase()
    return when {
        "lancha" in lower -> "LANCHA"
        "catamarã" in lower || "catamaran" in lower -> "CATAMARÃ"
        "navio" in lower -> "NAVIO"
        else -> "FERRYBOAT"
    }
}

private fun formatBannerDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE, dd 'de' MMM", Locale.forLanguageTag("pt-BR")))
        .replaceFirstChar { it.titlecase(Locale.forLanguageTag("pt-BR")) }

private fun formatBannerTime(time: LocalTime): String =
    time.format(DateTimeFormatter.ofPattern("HH:mm"))

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)
