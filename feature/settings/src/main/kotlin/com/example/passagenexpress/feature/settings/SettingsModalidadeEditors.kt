package com.example.passagenexpress.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.passagenexpress.core.designsystem.component.TotemCard
import com.example.passagenexpress.core.designsystem.component.TotemErrorState
import com.example.passagenexpress.core.designsystem.component.TotemInputField
import com.example.passagenexpress.core.designsystem.component.TotemLoading
import com.example.passagenexpress.core.designsystem.theme.TotemTheme
import com.example.passagenexpress.core.domain.model.Modalidade
import com.example.passagenexpress.core.domain.model.onibus.LocalOnibus
import com.example.passagenexpress.core.domain.model.onibus.toOpcoes

/** Ações do editor de modalidades (agrupadas pra não inflar ainda mais as assinaturas da tela). */
internal class ModalidadeActions(
    val onEdit: () -> Unit,
    val onToggle: (Modalidade) -> Unit,
    val onSave: () -> Unit,
)

/** Ações do editor de terminal rodoviário. */
internal class TerminalActions(
    val onEdit: () -> Unit,
    val onClearQuery: () -> Unit,
    val onRetry: () -> Unit,
    val onSelected: (LocalOnibus) -> Unit,
    val onSave: () -> Unit,
)

@Composable
internal fun modalidadesLabel(modalidades: Set<Modalidade>): String {
    val hidro = stringResource(R.string.settings_modalidade_hidro)
    val rodo = stringResource(R.string.settings_modalidade_rodo)
    return buildList {
        if (Modalidade.Hidroviario in modalidades) add(hidro)
        if (Modalidade.Rodoviario in modalidades) add(rodo)
    }.joinToString(" + ")
}

@Composable
internal fun ModalidadesEditor(
    draft: Set<Modalidade>,
    hidroviarioConfigurado: Boolean,
    terminalConfigurado: Boolean,
    onToggle: (Modalidade) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space20),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ModalidadeCard(
                icon = Icons.Filled.DirectionsBoat,
                title = stringResource(R.string.settings_modalidade_hidro),
                description = stringResource(R.string.settings_modalidade_hidro_desc),
                selected = Modalidade.Hidroviario in draft,
                onClick = { onToggle(Modalidade.Hidroviario) },
                modifier = Modifier.weight(1f),
            )
            ModalidadeCard(
                icon = Icons.Filled.DirectionsBus,
                title = stringResource(R.string.settings_modalidade_rodo),
                description = stringResource(R.string.settings_modalidade_rodo_desc),
                selected = Modalidade.Rodoviario in draft,
                onClick = { onToggle(Modalidade.Rodoviario) },
                modifier = Modifier.weight(1f),
            )
        }
        // Explica por que o Salvar está desabilitado.
        val aviso = when {
            draft.isEmpty() -> R.string.settings_modalidade_min
            Modalidade.Hidroviario in draft && !hidroviarioConfigurado -> R.string.settings_modalidade_need_porto
            Modalidade.Rodoviario in draft && !terminalConfigurado -> R.string.settings_modalidade_need_terminal
            else -> null
        }
        if (aviso != null) {
            Text(
                text = stringResource(aviso),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ModalidadeCard(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TotemCard(modifier = modifier, selected = selected, onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TotemTheme.dimens.space24),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
internal fun TerminalEditor(
    query: String,
    terminais: SettingsTerminaisState,
    selected: LocalOnibus?,
    searchFocused: Boolean,
    onSearchClick: () -> Unit,
    onClearSearch: () -> Unit,
    onSelected: (LocalOnibus) -> Unit,
    onRetry: () -> Unit,
) {
    val todosFmt = stringResource(R.string.settings_terminal_all)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
    ) {
        if (selected != null) {
            Text(
                text = stringResource(R.string.settings_terminal_selected, selected.nome),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        TotemInputField(
            label = stringResource(R.string.settings_terminal_search_hint),
            value = query,
            placeholder = stringResource(R.string.settings_terminal_tap_to_type),
            focused = searchFocused,
            onClick = onSearchClick,
            modifier = Modifier.fillMaxWidth(),
            trailing = if (query.isNotEmpty()) ({
                IconButton(onClick = onClearSearch) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.settings_keypad_delete))
                }
            }) else ({
                Icon(Icons.Filled.Search, contentDescription = null)
            }),
        )
        when (terminais) {
            SettingsTerminaisState.Idle -> Text(
                text = stringResource(R.string.settings_terminal_min_chars),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsTerminaisState.Loading ->
                TotemLoading(label = stringResource(R.string.settings_terminal_loading))
            is SettingsTerminaisState.Error -> TotemErrorState(
                title = stringResource(R.string.settings_terminal_error_title),
                message = stringResource(R.string.settings_no_portos_message),
                actionLabel = stringResource(R.string.settings_retry),
                onAction = onRetry,
            )
            is SettingsTerminaisState.Loaded -> {
                val opcoes = terminais.terminais.toOpcoes { cidade, uf -> todosFmt.format(cidade, uf) }
                if (opcoes.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_terminal_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
                        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(opcoes, key = { it.slug }) { opcao ->
                            TotemCard(
                                selected = selected?.slug == opcao.slug,
                                onClick = { onSelected(opcao) },
                            ) {
                                Text(
                                    text = opcao.nome,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(TotemTheme.dimens.space20),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
