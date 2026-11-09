package com.example.passagenexpress.feature.bus.trips

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passagenexpress.core.designsystem.component.TotemErrorState
import com.example.passagenexpress.core.designsystem.component.TotemLoading
import com.example.passagenexpress.core.designsystem.component.TotemScreenScaffold
import com.example.passagenexpress.core.designsystem.component.TotemSecondaryButton
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme
import com.example.passagenexpress.core.domain.model.onibus.PrecoOnibus
import com.example.passagenexpress.core.domain.model.onibus.ViagemOnibus
import com.example.passagenexpress.feature.bus.R
import com.example.passagenexpress.feature.bus.Sentido
import com.example.passagenexpress.feature.bus.search.TOTAL_STEPS
import com.example.passagenexpress.feature.bus.ui.RouteBanner
import com.example.passagenexpress.feature.bus.ui.formatMoney
import com.example.passagenexpress.feature.bus.ui.formatShortDate
import com.example.passagenexpress.feature.bus.ui.formatTime

@Composable
fun BusTripsScreen(
    onTripSelected: () -> Unit,
    onBack: () -> Unit,
    viewModel: BusTripsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val title = if (state.sentido == Sentido.Ida) R.string.bus_trips_title_ida else R.string.bus_trips_title_volta

    TotemScreenScaffold(
        step = 2,
        totalSteps = TOTAL_STEPS,
        showCancel = state.sentido == Sentido.Volta,
        title = stringResource(title),
        footer = { TotemSecondaryButton(text = stringResource(R.string.bus_back), onClick = onBack) },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            RouteBanner(origem = state.origemNome, destino = state.destinoNome, detalhe = null)
            DayNavigator(
                label = formatShortDate(state.data),
                canPrev = state.data.isAfter(state.minData),
                onPrev = viewModel::onPrevDay,
                onNext = viewModel::onNextDay,
            )
            when (val v = state.viagens) {
                ViagensState.Loading -> TotemLoading(label = stringResource(R.string.bus_trips_loading))
                is ViagensState.Error -> TotemErrorState(
                    title = stringResource(R.string.bus_trips_error_title),
                    message = stringResource(R.string.bus_generic_error_message),
                    actionLabel = stringResource(R.string.bus_retry),
                    onAction = viewModel::onRetry,
                )
                is ViagensState.Loaded -> if (v.viagens.isEmpty()) {
                    EmptyTrips()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
                        contentPadding = PaddingValues(vertical = TotemTheme.dimens.space8),
                    ) {
                        items(v.viagens, key = { it.tripUuid }) { viagem ->
                            BusTripCard(viagem) {
                                viewModel.onViagemSelected(viagem)
                                onTripSelected()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayNavigator(label: String, canPrev: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
    ) {
        DayButton(
            text = stringResource(R.string.bus_prev_day),
            icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp)) },
            enabled = canPrev,
            onClick = onPrev,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TotemPalette.Ink,
            modifier = Modifier.weight(1f),
        )
        DayButton(
            text = stringResource(R.string.bus_next_day),
            icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp)) },
            enabled = true,
            onClick = onNext,
        )
    }
}

@Composable
private fun DayButton(text: String, icon: @Composable () -> Unit, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        color = TotemPalette.Paper,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, TotemPalette.Hairline),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .let { if (enabled) it.clickable(onClick = onClick) else it },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            icon()
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) TotemPalette.Ink else TotemPalette.InkSoft,
            )
        }
    }
}

@Composable
private fun BusTripCard(viagem: ViagemOnibus, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TotemTheme.dimens.radiusLg))
            .clickable(onClick = onClick),
        color = TotemPalette.Paper,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
        border = BorderStroke(1.5.dp, TotemPalette.Hairline),
    ) {
        Row(
            modifier = Modifier.padding(TotemTheme.dimens.space20),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space20),
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
                    text = viagem.empresaNome,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = TotemPalette.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${viagem.origemNome} → ${viagem.destinoNome}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TotemPalette.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (viagem.classe.isNotBlank()) Tag(viagem.classe, TotemPalette.AccentTint, TotemPalette.Accent)
                    if (viagem.temConexao) {
                        Tag(stringResource(R.string.bus_connection), TotemPalette.WarningLight, TotemPalette.WarningDark)
                    }
                    Text(
                        text = stringResource(R.string.bus_seats_left, viagem.assentosDisponiveis),
                        style = MaterialTheme.typography.labelLarge,
                        color = TotemPalette.InkMuted,
                    )
                }
            }
            Stat(formatTime(viagem.saida), stringResource(R.string.bus_label_departure))
            viagem.chegada?.let { Stat(formatTime(it), stringResource(R.string.bus_label_arrival)) }
            if (viagem.duracao.isNotBlank()) Stat(viagem.duracao, stringResource(R.string.bus_label_duration))
            Box(modifier = Modifier.size(width = 1.dp, height = 40.dp).background(TotemPalette.Hairline))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.bus_from_price),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
                    color = TotemPalette.InkMuted,
                )
                Text(
                    text = formatMoney(PrecoOnibus.comTaxa(viagem.valor)),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
                    color = TotemPalette.Accent,
                )
            }
        }
    }
}

@Composable
private fun Tag(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
            color = TotemPalette.Ink,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
            color = TotemPalette.InkMuted,
        )
    }
}

@Composable
private fun EmptyTrips() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            Box(
                modifier = Modifier.size(120.dp).background(TotemPalette.AccentTint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.EventBusy, null, tint = TotemPalette.Accent, modifier = Modifier.size(56.dp))
            }
            Text(
                text = stringResource(R.string.bus_trips_empty_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = TotemPalette.Ink,
            )
            Text(
                text = stringResource(R.string.bus_trips_empty_message),
                style = MaterialTheme.typography.bodyLarge,
                color = TotemPalette.InkMuted,
            )
        }
    }
}
