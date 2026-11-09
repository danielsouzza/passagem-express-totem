package com.example.passagenexpress.feature.bus.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passagenexpress.core.designsystem.component.TotemAlphaKeypad
import com.example.passagenexpress.core.designsystem.component.TotemCard
import com.example.passagenexpress.core.designsystem.component.TotemErrorState
import com.example.passagenexpress.core.designsystem.component.TotemInputField
import com.example.passagenexpress.core.designsystem.component.TotemLoading
import com.example.passagenexpress.core.designsystem.component.TotemPrimaryButton
import com.example.passagenexpress.core.designsystem.component.TotemScreenScaffold
import com.example.passagenexpress.core.designsystem.component.TotemSecondaryButton
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme
import com.example.passagenexpress.core.domain.model.onibus.LocalOnibus
import com.example.passagenexpress.core.domain.model.onibus.toOpcoes
import com.example.passagenexpress.feature.bus.R
import com.example.passagenexpress.feature.bus.ui.BusCalendar
import com.example.passagenexpress.feature.bus.ui.formatShortDate
import java.time.LocalDate

@Composable
fun BusSearchScreen(
    onSearch: () -> Unit,
    onBack: () -> Unit,
    viewModel: BusSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val title = if (state.stage == BusSearchStage.Destino) R.string.bus_search_title else R.string.bus_dates_title

    TotemScreenScaffold(
        step = 1,
        totalSteps = TOTAL_STEPS,
        showCancel = false,
        title = stringResource(title),
        subtitle = state.origem?.nome?.let { stringResource(R.string.bus_search_from, it) },
        footer = {
            TotemSecondaryButton(
                text = stringResource(R.string.bus_back),
                onClick = if (state.stage == BusSearchStage.Datas) viewModel::onEditDestino else onBack,
            )
        },
        stickyBottom = if (state.stage == BusSearchStage.Destino && state.keypadOpen) ({
            TotemAlphaKeypad(
                label = stringResource(R.string.bus_search_destination_label),
                currentValue = state.destinoQuery,
                onChar = viewModel::onKeypadChar,
                onBackspace = viewModel::onKeypadBackspace,
                onClose = viewModel::onCloseKeypad,
                onDone = viewModel::onCloseKeypad,
                doneLabel = stringResource(R.string.bus_keypad_done),
                closeContentDesc = stringResource(R.string.bus_keypad_close),
                deleteContentDesc = stringResource(R.string.bus_keypad_delete),
                spaceContentDesc = stringResource(R.string.bus_keypad_space),
                shiftContentDesc = stringResource(R.string.bus_keypad_shift),
            )
        }) else null,
    ) {
        when (state.stage) {
            BusSearchStage.Destino -> DestinoStage(state, viewModel)
            BusSearchStage.Datas -> DatasStage(state, viewModel, onSearch)
        }
    }
}

internal const val TOTAL_STEPS = 5

@Composable
private fun DestinoStage(state: BusSearchUiState, viewModel: BusSearchViewModel) {
    val todosFmt = stringResource(R.string.bus_terminal_all)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
    ) {
        TotemInputField(
            label = stringResource(R.string.bus_search_destination_label),
            value = state.destinoQuery,
            placeholder = stringResource(R.string.bus_search_destination_hint),
            focused = state.keypadOpen,
            onClick = viewModel::onOpenKeypad,
            modifier = Modifier.fillMaxWidth(),
            trailing = if (state.destinoQuery.isNotEmpty()) ({
                IconButton(onClick = viewModel::onClearQuery) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.bus_keypad_delete))
                }
            }) else ({
                Icon(Icons.Filled.Search, contentDescription = null)
            }),
        )
        when (val destinos = state.destinos) {
            DestinosState.Idle -> Text(
                text = stringResource(R.string.bus_search_min_chars),
                style = MaterialTheme.typography.titleMedium,
                color = TotemPalette.InkMuted,
            )
            DestinosState.Loading -> TotemLoading(label = stringResource(R.string.bus_search_loading))
            is DestinosState.Error -> TotemErrorState(
                title = stringResource(R.string.bus_search_error_title),
                message = stringResource(R.string.bus_generic_error_message),
                actionLabel = stringResource(R.string.bus_retry),
                onAction = viewModel::onRetry,
            )
            is DestinosState.Loaded -> {
                // Não oferece como destino o próprio terminal de origem.
                val opcoes = destinos.terminais
                    .toOpcoes { cidade, uf -> todosFmt.format(cidade, uf) }
                    .filter { it.slug != state.origem?.slug }
                if (opcoes.isEmpty()) {
                    Text(
                        text = stringResource(R.string.bus_search_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = TotemPalette.InkMuted,
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 320.dp),
                        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
                        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(opcoes, key = { it.slug }) { opcao ->
                            DestinoCard(opcao, selected = state.destino?.slug == opcao.slug) {
                                viewModel.onDestinoSelected(opcao)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinoCard(opcao: LocalOnibus, selected: Boolean, onClick: () -> Unit) {
    TotemCard(selected = selected, onClick = onClick) {
        Row(
            modifier = Modifier.padding(TotemTheme.dimens.space20),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            Icon(Icons.Filled.DirectionsBus, contentDescription = null, tint = TotemPalette.Accent)
            Text(
                text = opcao.nome,
                style = MaterialTheme.typography.titleLarge,
                color = TotemPalette.Ink,
            )
        }
    }
}

/**
 * Só o calendário (fluxo guiado): tocar no dia já avança pra lista de viagens. Pra trocar o
 * destino, o cliente usa o "Voltar" do rodapé.
 */
@Composable
private fun DatasStage(state: BusSearchUiState, viewModel: BusSearchViewModel, onSearch: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        BusCalendar(
            selected = state.dataIda,
            visibleMonth = state.visibleMonth,
            minDate = LocalDate.now(),
            onSelect = { date ->
                viewModel.onDateSelected(date)
                if (viewModel.onSearch()) onSearch()
            },
            onPrevMonth = viewModel::onPrevMonth,
            onNextMonth = viewModel::onNextMonth,
            modifier = Modifier.widthIn(max = 640.dp),
        )
    }
}
