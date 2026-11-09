package com.example.passagenexpress.feature.bus.seats

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
import com.example.passagenexpress.core.domain.model.onibus.AssentoOnibus
import com.example.passagenexpress.core.domain.model.onibus.PrecoOnibus
import com.example.passagenexpress.feature.bus.R
import com.example.passagenexpress.feature.bus.search.TOTAL_STEPS
import com.example.passagenexpress.feature.bus.ui.formatMoney
import com.example.passagenexpress.feature.bus.ui.formatShortDate
import com.example.passagenexpress.feature.bus.ui.formatTime

/**
 * Poltronas do ônibus — mesmo layout da tela de cômodos do barco (`RoomScreen`): banner da viagem,
 * legenda e grade vertical de poltronas (frente do ônibus no topo), rodapé Voltar/Continuar.
 */
@Composable
fun BusSeatsScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: BusSeatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.concluido) {
        if (state.concluido) {
            viewModel.onNavigated()
            onDone()
        }
    }
    // Conexão: voltar recua uma perna antes de sair da tela.
    BackHandler(enabled = state.pernaIndex > 0) { viewModel.onBackPerna() }

    val ultimaPerna = state.pernaIndex + 1 >= state.totalPernas
    TotemScreenScaffold(
        step = 3,
        totalSteps = TOTAL_STEPS,
        title = stringResource(R.string.bus_seats_title),
        footer = {
            TotemSecondaryButton(
                text = stringResource(R.string.bus_back),
                onClick = { if (!viewModel.onBackPerna()) onBack() },
            )
            Spacer(Modifier.width(TotemTheme.dimens.space12))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TotemPrimaryButton(
                    text = when {
                        state.bloqueando -> stringResource(R.string.bus_seats_locking)
                        !ultimaPerna -> stringResource(R.string.bus_seats_next_leg)
                        else -> stringResource(R.string.bus_continue)
                    },
                    onClick = viewModel::onConfirm,
                    enabled = state.canConfirm,
                )
            }
        },
    ) {
        val perna = state.perna
        when {
            state.pernasErro != null -> TotemErrorState(
                title = stringResource(R.string.bus_seats_error_title),
                message = stringResource(R.string.bus_generic_error_message),
                actionLabel = stringResource(R.string.bus_retry),
                onAction = viewModel::onRetryPernas,
            )
            perna == null || state.mapa is MapaState.Loading ->
                TotemLoading(label = stringResource(R.string.bus_seats_loading))
            state.mapa is MapaState.Error -> TotemErrorState(
                title = stringResource(R.string.bus_seats_error_title),
                message = stringResource(R.string.bus_generic_error_message),
                actionLabel = stringResource(R.string.bus_retry),
                onAction = viewModel::onRetryMapa,
            )
            else -> {
                val assentos = (state.mapa as MapaState.Loaded).assentos
                val meus = state.bloqueadas[state.pernaIndex].orEmpty().map { it.label }.toSet()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
                ) {
                    TripInfoBanner(
                        empresa = state.viagem?.empresaNome.orEmpty(),
                        classe = state.viagem?.classe.orEmpty(),
                        origem = perna.origemNome,
                        destino = perna.destinoNome,
                        data = formatShortDate(perna.saida.toLocalDate()),
                        hora = formatTime(perna.saida),
                    )
                    if (state.totalPernas > 1) {
                        Text(
                            text = stringResource(R.string.bus_leg_of, state.pernaIndex + 1, state.totalPernas),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TotemPalette.WarningDark,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Legend()
                    SelectionHint(state, assentos)
                    SeatGrid(
                        assentos = assentos,
                        selecionados = state.selecionados.toSet(),
                        meusBloqueios = meus,
                        onClick = viewModel::onSeatToggled,
                    )
                    state.erroBloqueio?.let { erro ->
                        TransientErrorBanner(
                            message = erro.ifBlank { stringResource(R.string.bus_seats_lock_error) },
                            onDismiss = viewModel::onDismissError,
                        )
                    }
                    Spacer(Modifier.height(TotemTheme.dimens.space12))
                }
            }
        }
    }
}

/** "Escolha 2 poltronas · Selecionadas: 12, 20 · R$ …" — orientação + total parcial. */
@Composable
private fun SelectionHint(state: BusSeatsUiState, assentos: List<AssentoOnibus>) {
    val preco = assentos.associate { it.label to it.valor }
    val dica = state.quantidadeExigida?.let { stringResource(R.string.bus_seats_pick_exact, it) }
        ?: stringResource(R.string.bus_seats_pick_free, BusSeatsViewModel.MAX_ASSENTOS)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = dica, style = MaterialTheme.typography.bodyLarge, color = TotemPalette.InkMuted, textAlign = TextAlign.Center)
        if (state.selecionados.isNotEmpty()) {
            val total = PrecoOnibus.arredondar(state.selecionados.sumOf { PrecoOnibus.comTaxa(preco[it] ?: 0.0) })
            Text(
                text = stringResource(R.string.bus_seats_selected_summary, state.selecionados.joinToString(), formatMoney(total)),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TotemPalette.Accent,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// Mesmas cores da legenda do barco (RoomScreen.LegendColors).
private object LegendColors {
    val Free = Color(0xFF22C55E)
    val Selected = Color(0xFF3B82F6)
    val Unavailable = Color(0xFF9CA3AF)
    val Empty = Color(0xFFE5E7EB)
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space32, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top,
    ) {
        LegendDot(LegendColors.Free, stringResource(R.string.bus_seat_free))
        LegendDot(LegendColors.Selected, stringResource(R.string.bus_seat_selected))
        LegendDot(LegendColors.Unavailable, stringResource(R.string.bus_seat_taken))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(color))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = TotemPalette.InkMuted,
        )
    }
}

/**
 * Grade vertical como a do barco: cada linha da tela é uma fileira do ônibus (`coluna`, frente no
 * topo) e cada coluna é a posição lateral (`linha`, invertida como no mobile do site). A posição
 * lateral que não tem nenhuma poltrona é o corredor (espaço vazio, mais estreito).
 */
@Composable
private fun SeatGrid(
    assentos: List<AssentoOnibus>,
    selecionados: Set<String>,
    meusBloqueios: Set<String>,
    onClick: (AssentoOnibus) -> Unit,
) {
    if (assentos.isEmpty()) {
        Text(
            text = stringResource(R.string.bus_seats_empty),
            style = MaterialTheme.typography.titleMedium,
            color = TotemPalette.InkMuted,
        )
        return
    }
    val minLinha = assentos.minOf { it.linha }
    val maxLinha = assentos.maxOf { it.linha }
    val minColuna = assentos.minOf { it.coluna }
    val maxColuna = assentos.maxOf { it.coluna }
    val porPosicao = assentos.associateBy { it.coluna to it.linha }
    val linhasComAssento = assentos.map { it.linha }.toSet()

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Filled.DirectionsBus,
            contentDescription = stringResource(R.string.bus_driver),
            tint = TotemPalette.InkSoft,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(8.dp))
        for (fileira in minColuna..maxColuna) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (lateral in maxLinha downTo minLinha) {
                    if (lateral !in linhasComAssento) {
                        Spacer(Modifier.size(width = 40.dp, height = 80.dp)) // corredor
                        continue
                    }
                    val seat = porPosicao[fileira to lateral]
                    val ocupado = seat != null && seat.ocupado && seat.label !in meusBloqueios
                    SeatChip(
                        assento = seat,
                        ocupado = ocupado,
                        selected = seat != null && seat.label in selecionados,
                        onClick = if (seat != null && !ocupado) ({ onClick(seat) }) else null,
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
    assento: AssentoOnibus?,
    ocupado: Boolean,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        assento == null -> LegendColors.Empty
        selected -> LegendColors.Selected
        ocupado -> LegendColors.Unavailable
        else -> LegendColors.Free
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        modifier = modifier.let { m -> if (onClick != null) m.clickable(onClick = onClick) else m },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (assento != null) {
                Text(
                    text = assento.numeracao,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = TotemPalette.Paper,
                )
            }
        }
    }
}

/** Banner da viagem — mesmo visual do `TripInfoBanner` do barco, com dados do ônibus. */
@Composable
private fun TripInfoBanner(empresa: String, classe: String, origem: String, destino: String, data: String, hora: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = TotemPalette.Paper,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
        border = BorderStroke(1.5.dp, TotemPalette.Hairline),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = TotemTheme.dimens.space20, vertical = TotemTheme.dimens.space16),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
            ) {
                Surface(
                    color = TotemPalette.AccentTint,
                    contentColor = TotemPalette.Accent,
                    shape = RoundedCornerShape(TotemTheme.dimens.radiusSm),
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.DirectionsBus, contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = empresa,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
                        color = TotemPalette.Ink,
                        maxLines = 1,
                    )
                    Text(
                        text = classe.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
                        color = TotemPalette.InkMuted,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
            ) {
                Text(
                    text = origem.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp),
                    color = TotemPalette.Ink,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = TotemPalette.Accent, modifier = Modifier.size(22.dp))
                Text(
                    text = destino.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp),
                    color = TotemPalette.Ink,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12)) {
                InfoPill(Icons.Filled.CalendarToday, data, Modifier.weight(1f))
                InfoPill(Icons.Filled.Schedule, hora, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InfoPill(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(color = TotemPalette.AccentTint, shape = RoundedCornerShape(TotemTheme.dimens.radiusPill), modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Icon(icon, null, tint = TotemPalette.Accent, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp),
                color = TotemPalette.Ink,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TransientErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = TotemTheme.dimens.space16, vertical = TotemTheme.dimens.space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}
