package com.example.passagenexpress.feature.city

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passagenexpress.core.designsystem.component.TotemAlphaKeypad
import com.example.passagenexpress.core.designsystem.component.TotemErrorState
import com.example.passagenexpress.core.designsystem.component.TotemLoading
import com.example.passagenexpress.core.designsystem.component.TotemPrimaryButton
import com.example.passagenexpress.core.designsystem.component.TotemScreenScaffold
import com.example.passagenexpress.core.designsystem.component.TotemSecondaryButton
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme
import com.example.passagenexpress.core.domain.model.Municipio

@Composable
fun CityScreen(
    onDestinoSelected: (Municipio) -> Unit,
    onBack: () -> Unit,
    viewModel: CityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CityContent(
        state = state,
        onDestinoSearchChange = viewModel::onDestinoSearchChange,
        onDestinoSelected = viewModel::onDestinoSelected,
        onContinue = {
            state.destinoSelecionada?.let(onDestinoSelected)
        },
        onRetry = viewModel::onRetry,
        onBack = onBack,
    )
}

@Composable
private fun CityContent(
    state: CityUiState,
    onDestinoSearchChange: (String) -> Unit,
    onDestinoSelected: (Municipio) -> Unit,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val origemLabel = state.municipioOrigemNome.ifEmpty { state.portoNome }
    val subtitle = if (origemLabel.isNotEmpty()) {
        stringResFmt(R.string.city_destino_subtitle_porto_format, origemLabel)
    } else null

    // Totem usa keypad próprio (sem teclado do sistema). O field de busca é só um
    // gatilho que abre o TotemAlphaKeypad no slot stickyBottom do scaffold.
    var keypadOpen by remember { mutableStateOf(false) }

    TotemScreenScaffold(
        step = 1,
        title = stringRes(R.string.city_destino_title),
        subtitle = subtitle,
        footer = {
            TotemSecondaryButton(text = stringRes(R.string.city_back), onClick = onBack)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TotemPrimaryButton(
                    text = stringRes(R.string.city_continue),
                    onClick = onContinue,
                    enabled = state.destinoSelecionada != null,
                )
            }
        },
        stickyBottom = if (keypadOpen) {
            {
                TotemAlphaKeypad(
                    label = stringRes(R.string.city_search_hint),
                    currentValue = state.destinoSearch,
                    onChar = { c -> onDestinoSearchChange(state.destinoSearch + c) },
                    onBackspace = { onDestinoSearchChange(state.destinoSearch.dropLast(1)) },
                    onClose = { keypadOpen = false },
                    onDone = { keypadOpen = false },
                    doneLabel = stringRes(R.string.city_keypad_done),
                    closeContentDesc = stringRes(R.string.city_keypad_close),
                    deleteContentDesc = stringRes(R.string.city_keypad_delete),
                    spaceContentDesc = stringRes(R.string.city_keypad_space),
                    shiftContentDesc = stringRes(R.string.city_keypad_shift),
                )
            }
        } else null,
    ) {
        DestinoPicker(
            state = state.destinos.filter(state.destinoSearch),
            query = state.destinoSearch,
            onOpenKeypad = { keypadOpen = true },
            onClearQuery = { onDestinoSearchChange("") },
            selectedSlug = state.destinoSelecionada?.slug,
            onItemSelected = { municipio ->
                keypadOpen = false
                onDestinoSelected(municipio)
            },
            onRetry = onRetry,
        )
    }
}

@Composable
private fun DestinoPicker(
    state: MunicipiosState,
    query: String,
    onOpenKeypad: () -> Unit,
    onClearQuery: () -> Unit,
    selectedSlug: String?,
    onItemSelected: (Municipio) -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        MunicipiosState.Loading -> TotemLoading(label = stringRes(R.string.city_loading))
        is MunicipiosState.Error -> TotemErrorState(
            title = stringRes(R.string.city_error_title),
            message = state.message,
            actionLabel = stringRes(R.string.city_retry),
            onAction = onRetry,
        )
        is MunicipiosState.Loaded -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space20),
            ) {
                SearchField(query = query, onOpenKeypad = onOpenKeypad, onClear = onClearQuery)
                if (state.municipios.isEmpty() && query.isEmpty()) {
                    TotemErrorState(
                        title = stringRes(R.string.city_empty_destinos_title),
                        message = stringRes(R.string.city_empty_destinos_message),
                        actionLabel = stringRes(R.string.city_retry),
                        onAction = onRetry,
                    )
                } else if (state.municipios.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringRes(R.string.city_search_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = TotemPalette.InkMuted,
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
                        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
                    ) {
                        itemsIndexed(
                            items = state.municipios,
                            key = { _, m -> m.slug },
                        ) { index, municipio ->
                            StaggeredItem(index = index) {
                                CityCard(
                                    municipio = municipio,
                                    selected = selectedSlug == municipio.slug,
                                    onClick = { onItemSelected(municipio) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Wrapper que entra em cena com fade + slide-up, com delay escalonado pelo índice.
 * Dá efeito de cascata nos cards quando a tela monta a primeira vez.
 */
@Composable
private fun StaggeredItem(index: Int, content: @Composable () -> Unit) {
    // `MutableTransitionState(false)` + alterar pra true após composição garante que
    // a animação SEMPRE roda no primeiro draw. O Lazy* reusa composables ao scrollar,
    // então essa key é por índice — itens novos animam, reciclados não.
    val visibility = remember(index) {
        MutableTransitionState(false).apply { targetState = true }
    }
    AnimatedVisibility(
        visibleState = visibility,
        enter = fadeIn(animationSpec = tween(durationMillis = 260, delayMillis = index * 35)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 320, delayMillis = index * 35),
                initialOffsetY = { it / 5 },
            ),
    ) {
        content()
    }
}

/**
 * "Field" de busca que parece um input mas é só um gatilho. Toque abre o
 * `TotemAlphaKeypad` no rodapé do scaffold — não usa teclado nativo do Android
 * (totem não tem teclado físico e o IME do sistema atrapalha a UX touchscreen).
 */
@Composable
private fun SearchField(
    query: String,
    onOpenKeypad: () -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(TotemTheme.dimens.radius),
        color = TotemPalette.Paper,
        border = BorderStroke(1.5.dp, TotemPalette.Hairline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TotemTheme.dimens.touchTarget)
            .clip(RoundedCornerShape(TotemTheme.dimens.radius))
            .clickable(onClick = onOpenKeypad),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = TotemTheme.dimens.space20,
                vertical = TotemTheme.dimens.space14,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = TotemPalette.InkMuted,
            )
            Text(
                text = query.ifEmpty { stringRes(R.string.city_search_hint) },
                style = MaterialTheme.typography.bodyLarge,
                color = if (query.isEmpty()) TotemPalette.InkMuted else TotemPalette.Ink,
                modifier = Modifier.weight(1f),
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringRes(R.string.city_search_clear),
                        tint = TotemPalette.InkMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun CityCard(
    municipio: Municipio,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val border = if (selected) {
        BorderStroke(2.dp, TotemPalette.Ink)
    } else {
        BorderStroke(1.5.dp, TotemPalette.Hairline)
    }
    val bg = if (selected) TotemPalette.AccentTint else TotemPalette.Paper
    val titleColor = if (selected) TotemPalette.Accent else TotemPalette.Ink
    val chipBg = if (selected) TotemPalette.Accent else TotemPalette.AccentTint
    val chipFg = if (selected) TotemPalette.Paper else TotemPalette.Accent

    Surface(
        shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
        color = bg,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TotemTheme.dimens.radiusLg))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(TotemTheme.dimens.space24),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        ) {
            Surface(
                color = chipBg,
                contentColor = chipFg,
                shape = RoundedCornerShape(TotemTheme.dimens.radiusSm),
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = municipio.nome,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = titleColor,
                    maxLines = 2,
                )
                val uf = municipio.uf?.takeIf { it.isNotEmpty() }
                Text(
                    text = uf ?: stringRes(R.string.city_destino_title),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp,
                    ),
                    color = TotemPalette.InkMuted,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)

@Composable
private fun stringResFmt(id: Int, vararg args: Any): String =
    androidx.compose.ui.res.stringResource(id, *args)

@Suppress("UnusedPrivateMember")
@Preview(name = "City destino", widthDp = 800, heightDp = 1280)
@Composable
private fun CityScreenPreview() {
    TotemTheme {
        CityContent(
            state = CityUiState(
                portoNome = "Porto de Santarém",
                municipioOrigemNome = "Santarém",
                destinos = MunicipiosState.Loaded(
                    listOf(
                        Municipio(slug = "manaus", nome = "Manaus", uf = "AM"),
                        Municipio(slug = "belem", nome = "Belém", uf = "PA"),
                    ),
                ),
            ),
            onDestinoSearchChange = {},
            onDestinoSelected = {},
            onContinue = {},
            onRetry = {},
            onBack = {},
        )
    }
}
