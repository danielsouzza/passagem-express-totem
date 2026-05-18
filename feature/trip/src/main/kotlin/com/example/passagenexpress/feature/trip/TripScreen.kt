package com.example.passagenexpress.feature.trip

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBoatFilled
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
import com.example.passagenexpress.core.domain.model.Municipio
import com.example.passagenexpress.core.domain.model.TipoComodo
import com.example.passagenexpress.core.domain.model.Trecho
import java.time.LocalDate
import java.time.LocalTime
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
        onSelectTrip = viewModel::onSelectTrip,
        onJumpToProxima = viewModel::onJumpToProximaViagem,
        onRetry = viewModel::onRetry,
        onContinue = {
            val selectedId = state.selectedTrechoId
            val loaded = state.trips as? TripsState.Loaded
            val trecho = loaded?.trechos?.firstOrNull { it.id == selectedId }
            if (trecho != null) onTripConfirmed(trecho)
        },
        onBack = onBack,
    )
}

@Composable
private fun TripContent(
    state: TripUiState,
    onSelectTrip: (Trecho) -> Unit,
    onJumpToProxima: () -> Unit,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val subtitle = if (state.origemNome.isNotEmpty() && state.destinoNome.isNotEmpty()) {
        stringResFmt(R.string.trip_subtitle_route, state.origemNome, state.destinoNome)
    } else null

    TotemScreenScaffold(
        step = 3,
        title = stringRes(R.string.trip_title),
        subtitle = subtitle,
        footer = {
            TotemSecondaryButton(text = stringRes(R.string.trip_back), onClick = onBack)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TotemPrimaryButton(
                    text = stringRes(R.string.trip_continue),
                    onClick = onContinue,
                    enabled = state.selectedTrechoId != null,
                )
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                            items(trips.trechos, key = { it.id }) { trecho ->
                                TripCard(
                                    trecho = trecho,
                                    selected = state.selectedTrechoId == trecho.id,
                                    onClick = { onSelectTrip(trecho) },
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
private fun TripCard(
    trecho: Trecho,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) TotemPalette.Ink else TotemPalette.Hairline
    val borderWidth = if (selected) 2.dp else 1.5.dp
    val nameColor = if (selected) TotemPalette.Accent else TotemPalette.Ink
    val chipBg = if (selected) TotemPalette.Accent else TotemPalette.AccentTint
    val chipFg = if (selected) TotemPalette.Paper else TotemPalette.Accent
    val type = vesselType(trecho.embarcacao)
    val priceTotal = trecho.valor + trecho.taxaDeEmbarque

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
                    text = trecho.embarcacao.ifEmpty { stringRes(R.string.trip_title) },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                    ),
                    color = nameColor,
                    maxLines = 1,
                )
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
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
            PriceStat(value = formatMoney(priceTotal))
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
private fun PriceStat(value: String) {
    Column(horizontalAlignment = Alignment.End) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        ) {
            Text(
                text = stringRes(R.string.trip_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                color = TotemPalette.Ink,
            )
            Text(
                text = stringRes(R.string.trip_empty_message),
                style = MaterialTheme.typography.bodyLarge,
                color = TotemPalette.InkMuted,
            )
            if (proxima != null) {
                Spacer(Modifier.height(TotemTheme.dimens.space8))
                Surface(
                    shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
                    color = TotemPalette.PaperWarm,
                ) {
                    Column(
                        modifier = Modifier.padding(TotemTheme.dimens.space24),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
                    ) {
                        Text(
                            text = stringRes(R.string.trip_next_available_title).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                            ),
                            color = TotemPalette.Accent,
                        )
                        Text(
                            text = formatProximaDate(proxima.dataEmbarque),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TotemPalette.Ink,
                        )
                        Spacer(Modifier.height(TotemTheme.dimens.space8))
                        TotemPrimaryButton(
                            text = stringRes(R.string.trip_next_available_action),
                            onClick = onJumpToProxima,
                            accent = true,
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(time: LocalTime): String =
    time.format(DateTimeFormatter.ofPattern("HH:mm"))

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}min"
        h > 0 -> "${h}h"
        else -> "${m}min"
    }
}

private fun formatMoney(value: Double): String {
    val formatted = String.format(Locale.forLanguageTag("pt-BR"), "%,.2f", value)
    return "R$ $formatted"
}

private fun formatProximaDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", Locale.forLanguageTag("pt-BR")))
        .replaceFirstChar { it.uppercase(Locale.forLanguageTag("pt-BR")) }

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
                selectedTrechoId = 1L,
            ),
            onSelectTrip = {},
            onJumpToProxima = {},
            onRetry = {},
            onContinue = {},
            onBack = {},
        )
    }
}
