package com.example.passagenexpress.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passagenexpress.core.designsystem.component.TotemAlphaKeypad
import com.example.passagenexpress.core.designsystem.component.TotemCard
import com.example.passagenexpress.core.designsystem.component.TotemErrorState
import com.example.passagenexpress.core.designsystem.component.TotemInputField
import com.example.passagenexpress.core.designsystem.component.TotemLoading
import com.example.passagenexpress.core.designsystem.component.TotemNumericKeypad
import com.example.passagenexpress.core.designsystem.component.TotemPrimaryButton
import com.example.passagenexpress.core.designsystem.component.TotemScreenScaffold
import com.example.passagenexpress.core.designsystem.component.TotemSecondaryButton
import com.example.passagenexpress.core.designsystem.theme.TotemTheme
import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.model.Embarcacao
import com.example.passagenexpress.core.domain.model.PaperWidth
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.printer.UsbPrinterDevice

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        state = state,
        onBack = onBack,
        onPinDigit = viewModel::onPinDigit,
        onPinBackspace = viewModel::onPinBackspace,
        onPinSubmit = viewModel::onPinSubmit,
        onEditSubdomain = viewModel::onEditSubdomain,
        onEditPorto = viewModel::onEditPorto,
        onEditEmbarcacao = viewModel::onEditEmbarcacao,
        onEditPrinter = viewModel::onEditPrinter,
        onEditLanguage = viewModel::onEditLanguage,
        onEditPin = viewModel::onEditPin,
        onCancelEdit = viewModel::onCancelEdit,
        onEmbarcacaoSearchChange = viewModel::onEmbarcacaoSearchChange,
        onRetryEmbarcacoes = viewModel::onRetryEmbarcacoes,
        onEmbarcacaoSelected = viewModel::onEmbarcacaoSelected,
        onEmbarcacaoCleared = viewModel::onEmbarcacaoCleared,
        onSaveEmbarcacao = viewModel::onSaveEmbarcacao,
        onPinDraftDigit = viewModel::onPinDraftDigit,
        onPinDraftBackspace = viewModel::onPinDraftBackspace,
        onSavePin = viewModel::onSavePin,
        onOpenKeypad = viewModel::onOpenKeypad,
        onKeypadChar = viewModel::onKeypadChar,
        onKeypadBackspace = viewModel::onKeypadBackspace,
        onCloseKeypad = viewModel::onCloseKeypad,
        onPortoSearchChange = viewModel::onPortoSearchChange,
        onRetryPortos = viewModel::onRetryPortos,
        onPortoSelected = viewModel::onPortoSelected,
        onPortoCleared = viewModel::onPortoCleared,
        onRefreshPrinters = viewModel::refreshPrinters,
        onPrinterSelected = viewModel::onPrinterSelected,
        onPaperWidthSelected = viewModel::onPaperWidthSelected,
        onTestPrint = viewModel::onTestPrint,
        onLanguageSelected = viewModel::onLanguageSelected,
        onSaveSubdomain = viewModel::onSaveSubdomain,
        onSavePorto = viewModel::onSavePorto,
        onSavePrinter = viewModel::onSavePrinter,
        onSaveLanguage = viewModel::onSaveLanguage,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onPinDigit: (Char) -> Unit,
    onPinBackspace: () -> Unit,
    onPinSubmit: () -> Unit,
    onEditSubdomain: () -> Unit,
    onEditPorto: () -> Unit,
    onEditEmbarcacao: () -> Unit,
    onEditPrinter: () -> Unit,
    onEditLanguage: () -> Unit,
    onEditPin: () -> Unit,
    onCancelEdit: () -> Unit,
    onEmbarcacaoSearchChange: (String) -> Unit,
    onRetryEmbarcacoes: () -> Unit,
    onEmbarcacaoSelected: (Embarcacao) -> Unit,
    onEmbarcacaoCleared: () -> Unit,
    onSaveEmbarcacao: () -> Unit,
    onPinDraftDigit: (Char) -> Unit,
    onPinDraftBackspace: () -> Unit,
    onSavePin: () -> Unit,
    onOpenKeypad: (SettingsKeypadField) -> Unit,
    onKeypadChar: (Char) -> Unit,
    onKeypadBackspace: () -> Unit,
    onCloseKeypad: () -> Unit,
    onPortoSearchChange: (String) -> Unit,
    onRetryPortos: () -> Unit,
    onPortoSelected: (Porto) -> Unit,
    onPortoCleared: () -> Unit,
    onRefreshPrinters: () -> Unit,
    onPrinterSelected: (UsbPrinterDevice) -> Unit,
    onPaperWidthSelected: (PaperWidth) -> Unit,
    onTestPrint: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSaveSubdomain: () -> Unit,
    onSavePorto: () -> Unit,
    onSavePrinter: () -> Unit,
    onSaveLanguage: () -> Unit,
) {
    when {
        !state.unlocked -> PinGate(
            state = state,
            onPinDigit = onPinDigit,
            onPinBackspace = onPinBackspace,
            onPinSubmit = onPinSubmit,
            onExit = onBack,
        )
        state.editing == null -> SettingsHub(
            state = state,
            onBack = onBack,
            onEditSubdomain = onEditSubdomain,
            onEditPorto = onEditPorto,
            onEditEmbarcacao = onEditEmbarcacao,
            onEditPrinter = onEditPrinter,
            onEditLanguage = onEditLanguage,
            onEditPin = onEditPin,
        )
        else -> SettingsEditor(
            state = state,
            onCancelEdit = onCancelEdit,
            onOpenKeypad = onOpenKeypad,
            onKeypadChar = onKeypadChar,
            onKeypadBackspace = onKeypadBackspace,
            onCloseKeypad = onCloseKeypad,
            onPortoSearchChange = onPortoSearchChange,
            onRetryPortos = onRetryPortos,
            onPortoSelected = onPortoSelected,
            onPortoCleared = onPortoCleared,
            onEmbarcacaoSearchChange = onEmbarcacaoSearchChange,
            onRetryEmbarcacoes = onRetryEmbarcacoes,
            onEmbarcacaoSelected = onEmbarcacaoSelected,
            onEmbarcacaoCleared = onEmbarcacaoCleared,
            onRefreshPrinters = onRefreshPrinters,
            onPrinterSelected = onPrinterSelected,
            onPaperWidthSelected = onPaperWidthSelected,
            onTestPrint = onTestPrint,
            onLanguageSelected = onLanguageSelected,
            onPinDraftDigit = onPinDraftDigit,
            onPinDraftBackspace = onPinDraftBackspace,
            onSaveSubdomain = onSaveSubdomain,
            onSavePorto = onSavePorto,
            onSaveEmbarcacao = onSaveEmbarcacao,
            onSavePrinter = onSavePrinter,
            onSaveLanguage = onSaveLanguage,
            onSavePin = onSavePin,
        )
    }
}

// ---------------------------------------------------------------------------
// PIN gate
// ---------------------------------------------------------------------------

@Composable
private fun PinGate(
    state: SettingsUiState,
    onPinDigit: (Char) -> Unit,
    onPinBackspace: () -> Unit,
    onPinSubmit: () -> Unit,
    onExit: () -> Unit,
) {
    TotemScreenScaffold(
        title = stringResource(R.string.settings_pin_title),
        subtitle = stringResource(R.string.settings_pin_subtitle),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        ) {
            if (state.pinError) {
                Text(
                    text = stringResource(R.string.settings_pin_error),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Box(modifier = Modifier.widthIn(max = 460.dp)) {
                TotemNumericKeypad(
                    label = stringResource(R.string.settings_pin_label),
                    currentValue = "•".repeat(state.pinInput.length),
                    onDigit = onPinDigit,
                    onBackspace = onPinBackspace,
                    onClose = onExit,
                    onDone = onPinSubmit,
                    doneLabel = stringResource(R.string.settings_pin_enter),
                    closeContentDesc = stringResource(R.string.settings_exit),
                    deleteContentDesc = stringResource(R.string.settings_keypad_delete),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Hub
// ---------------------------------------------------------------------------

@Composable
private fun SettingsHub(
    state: SettingsUiState,
    onBack: () -> Unit,
    onEditSubdomain: () -> Unit,
    onEditPorto: () -> Unit,
    onEditEmbarcacao: () -> Unit,
    onEditPrinter: () -> Unit,
    onEditLanguage: () -> Unit,
    onEditPin: () -> Unit,
) {
    TotemScreenScaffold(
        title = stringResource(R.string.settings_title),
        subtitle = stringResource(R.string.settings_subtitle),
        showCancel = false,
        footer = {
            TotemSecondaryButton(text = stringResource(R.string.settings_back), onClick = onBack)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        ) {
            state.savedNotice?.let { field ->
                Text(
                    text = stringResource(R.string.settings_saved, stringResource(sectionTitleRes(field))),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            SettingRow(
                label = stringResource(R.string.settings_section_subdomain),
                value = state.currentSubdomain.ifEmpty { stringResource(R.string.settings_value_empty) },
                onClick = onEditSubdomain,
            )
            SettingRow(
                label = stringResource(R.string.settings_section_porto),
                value = state.currentPortoNome.ifEmpty { stringResource(R.string.settings_value_empty) },
                onClick = onEditPorto,
            )
            SettingRow(
                label = stringResource(R.string.settings_section_embarcacao),
                value = state.currentEmbarcacaoNome.ifEmpty { stringResource(R.string.settings_embarcacao_none) },
                onClick = onEditEmbarcacao,
            )
            SettingRow(
                label = stringResource(R.string.settings_section_printer),
                value = state.currentPrinterLabel.ifEmpty { stringResource(R.string.settings_value_empty) },
                onClick = onEditPrinter,
            )
            SettingRow(
                label = stringResource(R.string.settings_section_language),
                value = stringResource(languageLabelRes(state.currentLanguage)),
                onClick = onEditLanguage,
            )
            SettingRow(
                label = stringResource(R.string.settings_section_pin),
                value = if (state.currentOperatorPin.isEmpty()) {
                    stringResource(R.string.settings_value_empty)
                } else {
                    "•".repeat(state.currentOperatorPin.length)
                },
                onClick = onEditPin,
            )
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    TotemCard(onClick = onClick) {
        Row(
            modifier = Modifier.padding(TotemTheme.dimens.space20),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(R.string.settings_edit),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Editors
// ---------------------------------------------------------------------------

@Composable
private fun SettingsEditor(
    state: SettingsUiState,
    onCancelEdit: () -> Unit,
    onOpenKeypad: (SettingsKeypadField) -> Unit,
    onKeypadChar: (Char) -> Unit,
    onKeypadBackspace: () -> Unit,
    onCloseKeypad: () -> Unit,
    onPortoSearchChange: (String) -> Unit,
    onRetryPortos: () -> Unit,
    onPortoSelected: (Porto) -> Unit,
    onPortoCleared: () -> Unit,
    onEmbarcacaoSearchChange: (String) -> Unit,
    onRetryEmbarcacoes: () -> Unit,
    onEmbarcacaoSelected: (Embarcacao) -> Unit,
    onEmbarcacaoCleared: () -> Unit,
    onRefreshPrinters: () -> Unit,
    onPrinterSelected: (UsbPrinterDevice) -> Unit,
    onPaperWidthSelected: (PaperWidth) -> Unit,
    onTestPrint: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onPinDraftDigit: (Char) -> Unit,
    onPinDraftBackspace: () -> Unit,
    onSaveSubdomain: () -> Unit,
    onSavePorto: () -> Unit,
    onSaveEmbarcacao: () -> Unit,
    onSavePrinter: () -> Unit,
    onSaveLanguage: () -> Unit,
    onSavePin: () -> Unit,
) {
    val field = state.editing ?: return
    val (onSave, canSave) = when (field) {
        SettingsField.Subdomain -> onSaveSubdomain to true
        SettingsField.Porto -> onSavePorto to state.canSavePorto
        SettingsField.Embarcacao -> onSaveEmbarcacao to true
        SettingsField.Printer -> onSavePrinter to state.canSavePrinter
        SettingsField.Language -> onSaveLanguage to true
        SettingsField.Pin -> onSavePin to state.canSavePin
    }

    TotemScreenScaffold(
        title = stringResource(sectionTitleRes(field)),
        subtitle = stringResource(sectionSubtitleRes(field)),
        showCancel = false,
        footer = {
            TotemSecondaryButton(text = stringResource(R.string.settings_cancel), onClick = onCancelEdit)
            Spacer(Modifier.width(TotemTheme.dimens.space12))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TotemPrimaryButton(
                    text = stringResource(R.string.settings_save),
                    onClick = onSave,
                    enabled = canSave,
                )
            }
        },
        stickyBottom = if (state.keypadField != null) ({
            SettingsKeypadSlot(
                state = state,
                onChar = onKeypadChar,
                onBackspace = onKeypadBackspace,
                onClose = onCloseKeypad,
            )
        }) else null,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (field) {
                SettingsField.Subdomain -> SubdomainEditor(
                    value = state.subdomainInput,
                    focused = state.keypadField == SettingsKeypadField.Subdomain,
                    onClick = { onOpenKeypad(SettingsKeypadField.Subdomain) },
                )
                SettingsField.Porto -> PortoEditor(
                    portos = state.portos,
                    filteredPortos = state.filteredPortos,
                    searchQuery = state.portoSearchQuery,
                    selectedPortoId = state.selectedPorto?.id,
                    noneSelected = state.portoCleared,
                    searchFocused = state.keypadField == SettingsKeypadField.PortoSearch,
                    onSearchClick = { onOpenKeypad(SettingsKeypadField.PortoSearch) },
                    onClearSearch = { onPortoSearchChange("") },
                    onPortoSelected = onPortoSelected,
                    onPortoCleared = onPortoCleared,
                    onRetry = onRetryPortos,
                )
                SettingsField.Printer -> PrinterEditor(
                    printers = state.printers,
                    selectedPrinter = state.selectedPrinter,
                    selectedPaperWidth = state.selectedPaperWidth,
                    testState = state.printerTest,
                    onRefresh = onRefreshPrinters,
                    onPrinterSelected = onPrinterSelected,
                    onPaperWidthSelected = onPaperWidthSelected,
                    onTestPrint = onTestPrint,
                )
                SettingsField.Language -> LanguageEditor(
                    selected = state.languageDraft,
                    onLanguageSelected = onLanguageSelected,
                )
                SettingsField.Embarcacao -> EmbarcacaoEditor(
                    embarcacoes = state.embarcacoes,
                    filteredEmbarcacoes = state.filteredEmbarcacoes,
                    searchQuery = state.embarcacaoSearchQuery,
                    selectedEmbarcacaoId = state.selectedEmbarcacao?.id,
                    searchFocused = state.keypadField == SettingsKeypadField.EmbarcacaoSearch,
                    onSearchClick = { onOpenKeypad(SettingsKeypadField.EmbarcacaoSearch) },
                    onClearSearch = { onEmbarcacaoSearchChange("") },
                    onEmbarcacaoSelected = onEmbarcacaoSelected,
                    onEmbarcacaoCleared = onEmbarcacaoCleared,
                    onRetry = onRetryEmbarcacoes,
                )
                SettingsField.Pin -> PinEditor(
                    pinLength = state.pinDraft.length,
                    onDigit = onPinDraftDigit,
                    onBackspace = onPinDraftBackspace,
                    onCancel = onCancelEdit,
                    onDone = onSavePin,
                )
            }
        }
    }
}

@Composable
private fun PinEditor(
    pinLength: Int,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.widthIn(max = 460.dp)) {
            TotemNumericKeypad(
                label = stringResource(R.string.settings_pin_new_label),
                // Operador definindo o PIN: exibido mascarado, mas o botão Salvar valida o tamanho.
                currentValue = "•".repeat(pinLength),
                onDigit = onDigit,
                onBackspace = onBackspace,
                onClose = onCancel,
                onDone = onDone,
                doneLabel = stringResource(R.string.settings_save),
                closeContentDesc = stringResource(R.string.settings_cancel),
                deleteContentDesc = stringResource(R.string.settings_keypad_delete),
            )
        }
    }
}

@Composable
private fun SubdomainEditor(value: String, focused: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
    ) {
        TotemInputField(
            label = stringResource(R.string.settings_subdomain_label),
            value = value,
            placeholder = stringResource(R.string.settings_subdomain_hint),
            focused = focused,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(fraction = 0.6f),
        )
    }
}

@Composable
private fun PortoEditor(
    portos: PortosState,
    filteredPortos: List<Porto>,
    searchQuery: String,
    selectedPortoId: Long?,
    noneSelected: Boolean,
    searchFocused: Boolean,
    onSearchClick: () -> Unit,
    onClearSearch: () -> Unit,
    onPortoSelected: (Porto) -> Unit,
    onPortoCleared: () -> Unit,
    onRetry: () -> Unit,
) {
    when (portos) {
        PortosState.Idle, PortosState.Loading ->
            TotemLoading(label = stringResource(R.string.settings_loading_portos))
        is PortosState.Error -> TotemErrorState(
            title = stringResource(R.string.settings_load_error_title),
            message = portos.message,
            actionLabel = stringResource(R.string.settings_retry),
            onAction = onRetry,
        )
        is PortosState.Loaded -> {
            if (portos.portos.isEmpty()) {
                TotemErrorState(
                    title = stringResource(R.string.settings_no_portos_title),
                    message = stringResource(R.string.settings_no_portos_message),
                    actionLabel = stringResource(R.string.settings_retry),
                    onAction = onRetry,
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
                ) {
                    TotemInputField(
                        label = stringResource(R.string.settings_porto_search_hint),
                        value = searchQuery,
                        placeholder = stringResource(R.string.settings_tap_to_type),
                        focused = searchFocused,
                        onClick = onSearchClick,
                        modifier = Modifier.fillMaxWidth(),
                        trailing = if (searchQuery.isNotEmpty()) ({
                            IconButton(onClick = onClearSearch) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.settings_porto_clear_search),
                                )
                            }
                        }) else ({
                            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                        }),
                    )
                    // Opção de desvincular o porto (totem sem porto fixo — filtra pela embarcação).
                    TotemCard(selected = noneSelected, onClick = onPortoCleared) {
                        Text(
                            text = stringResource(R.string.settings_porto_none),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(TotemTheme.dimens.space20),
                        )
                    }
                    if (filteredPortos.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.settings_porto_search_empty),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 260.dp),
                            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
                            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(filteredPortos, key = { it.id }) { porto ->
                                TotemCard(
                                    selected = selectedPortoId == porto.id,
                                    onClick = { onPortoSelected(porto) },
                                ) {
                                    Column(
                                        modifier = Modifier.padding(TotemTheme.dimens.space20),
                                        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space4),
                                    ) {
                                        Text(
                                            text = porto.nome,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = porto.municipioNome.ifEmpty { porto.slug },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmbarcacaoEditor(
    embarcacoes: EmbarcacoesState,
    filteredEmbarcacoes: List<Embarcacao>,
    selectedEmbarcacaoId: Long?,
    searchQuery: String,
    searchFocused: Boolean,
    onSearchClick: () -> Unit,
    onClearSearch: () -> Unit,
    onEmbarcacaoSelected: (Embarcacao) -> Unit,
    onEmbarcacaoCleared: () -> Unit,
    onRetry: () -> Unit,
) {
    when (embarcacoes) {
        EmbarcacoesState.Idle, EmbarcacoesState.Loading ->
            TotemLoading(label = stringResource(R.string.settings_loading_embarcacoes))
        is EmbarcacoesState.Error -> TotemErrorState(
            title = stringResource(R.string.settings_load_error_title),
            message = embarcacoes.message,
            actionLabel = stringResource(R.string.settings_retry),
            onAction = onRetry,
        )
        is EmbarcacoesState.Loaded -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        ) {
            TotemInputField(
                label = stringResource(R.string.settings_embarcacao_search_hint),
                value = searchQuery,
                placeholder = stringResource(R.string.settings_tap_to_type),
                focused = searchFocused,
                onClick = onSearchClick,
                modifier = Modifier.fillMaxWidth(),
                trailing = if (searchQuery.isNotEmpty()) ({
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.settings_porto_clear_search),
                        )
                    }
                }) else ({
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                }),
            )
            // Opção de desvincular: totem fixo em terra (sem filtro de embarcação).
            TotemCard(selected = selectedEmbarcacaoId == null, onClick = onEmbarcacaoCleared) {
                Text(
                    text = stringResource(R.string.settings_embarcacao_none),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(TotemTheme.dimens.space20),
                )
            }
            if (filteredEmbarcacoes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.settings_embarcacao_search_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 260.dp),
                    horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
                    verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filteredEmbarcacoes, key = { it.id }) { embarcacao ->
                        TotemCard(
                            selected = selectedEmbarcacaoId == embarcacao.id,
                            onClick = { onEmbarcacaoSelected(embarcacao) },
                        ) {
                            Column(
                                modifier = Modifier.padding(TotemTheme.dimens.space20),
                                verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space4),
                            ) {
                                Text(
                                    text = embarcacao.nome,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (embarcacao.empresaNome.isNotEmpty()) {
                                    Text(
                                        text = embarcacao.empresaNome,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaperWidthSelector(
    selected: PaperWidth,
    onSelected: (PaperWidth) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8)) {
        Text(
            text = stringResource(R.string.settings_paper_width_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12)) {
            PaperWidth.entries.forEach { width ->
                TotemCard(
                    modifier = Modifier.weight(1f),
                    selected = width == selected,
                    onClick = { onSelected(width) },
                ) {
                    Column(
                        modifier = Modifier.padding(TotemTheme.dimens.space16),
                        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space4),
                    ) {
                        Text(
                            text = if (width == PaperWidth.MM58) "58 mm" else "80 mm",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.settings_paper_width_columns, width.columns),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrinterEditor(
    printers: List<UsbPrinterDevice>,
    selectedPrinter: UsbPrinterDevice?,
    selectedPaperWidth: PaperWidth,
    testState: PrinterTestState,
    onRefresh: () -> Unit,
    onPrinterSelected: (UsbPrinterDevice) -> Unit,
    onPaperWidthSelected: (PaperWidth) -> Unit,
    onTestPrint: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
    ) {
        PaperWidthSelector(selected = selectedPaperWidth, onSelected = onPaperWidthSelected)
        TotemSecondaryButton(text = stringResource(R.string.settings_printer_refresh), onClick = onRefresh)
        if (printers.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.settings_printer_empty),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 260.dp),
                horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
                verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            ) {
                items(printers, key = { "${it.vendorId}:${it.productId}" }) { device ->
                    val selected = selectedPrinter?.vendorId == device.vendorId &&
                        selectedPrinter.productId == device.productId
                    TotemCard(selected = selected, onClick = { onPrinterSelected(device) }) {
                        Column(
                            modifier = Modifier.padding(TotemTheme.dimens.space20),
                            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space4),
                        ) {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "VID ${device.vendorId}  ·  PID ${device.productId}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            TotemPrimaryButton(
                text = if (testState is PrinterTestState.Testing) {
                    stringResource(R.string.settings_printer_testing)
                } else {
                    stringResource(R.string.settings_printer_test)
                },
                onClick = onTestPrint,
                enabled = selectedPrinter != null && testState !is PrinterTestState.Testing,
                trailingArrow = false,
            )
            when (testState) {
                is PrinterTestState.Success -> Text(
                    text = stringResource(R.string.settings_printer_test_ok),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                is PrinterTestState.Error -> Text(
                    text = testState.message,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun LanguageEditor(selected: AppLanguage, onLanguageSelected: (AppLanguage) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space20),
        modifier = Modifier.fillMaxWidth(),
    ) {
        LanguageCard(
            label = stringResource(R.string.settings_language_pt),
            selected = selected == AppLanguage.PtBr,
            onClick = { onLanguageSelected(AppLanguage.PtBr) },
            modifier = Modifier.weight(1f),
        )
        LanguageCard(
            label = stringResource(R.string.settings_language_en),
            selected = selected == AppLanguage.EnUs,
            onClick = { onLanguageSelected(AppLanguage.EnUs) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LanguageCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TotemCard(modifier = modifier, selected = selected, onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TotemTheme.dimens.space32),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SettingsKeypadSlot(
    state: SettingsUiState,
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClose: () -> Unit,
) {
    val field = state.keypadField ?: return
    val (label, current) = when (field) {
        SettingsKeypadField.Subdomain ->
            stringResource(R.string.settings_subdomain_label) to state.subdomainInput
        SettingsKeypadField.PortoSearch ->
            stringResource(R.string.settings_porto_search_hint) to state.portoSearchQuery
        SettingsKeypadField.EmbarcacaoSearch ->
            stringResource(R.string.settings_embarcacao_search_hint) to state.embarcacaoSearchQuery
    }
    TotemAlphaKeypad(
        label = label,
        currentValue = current,
        onChar = onChar,
        onBackspace = onBackspace,
        onClose = onClose,
        onDone = onClose,
        doneLabel = stringResource(R.string.settings_keypad_done),
        closeContentDesc = stringResource(R.string.settings_keypad_close),
        deleteContentDesc = stringResource(R.string.settings_keypad_delete),
        spaceContentDesc = stringResource(R.string.settings_keypad_space),
        shiftContentDesc = stringResource(R.string.settings_keypad_shift),
    )
}

private fun sectionTitleRes(field: SettingsField): Int = when (field) {
    SettingsField.Subdomain -> R.string.settings_section_subdomain
    SettingsField.Porto -> R.string.settings_section_porto
    SettingsField.Embarcacao -> R.string.settings_section_embarcacao
    SettingsField.Printer -> R.string.settings_section_printer
    SettingsField.Language -> R.string.settings_section_language
    SettingsField.Pin -> R.string.settings_section_pin
}

private fun sectionSubtitleRes(field: SettingsField): Int = when (field) {
    SettingsField.Subdomain -> R.string.settings_subdomain_subtitle
    SettingsField.Porto -> R.string.settings_porto_subtitle
    SettingsField.Embarcacao -> R.string.settings_embarcacao_subtitle
    SettingsField.Printer -> R.string.settings_printer_subtitle
    SettingsField.Language -> R.string.settings_language_subtitle
    SettingsField.Pin -> R.string.settings_pin_change_subtitle
}

private fun languageLabelRes(language: AppLanguage): Int = when (language) {
    AppLanguage.PtBr -> R.string.settings_language_pt
    AppLanguage.EnUs -> R.string.settings_language_en
}

@Preview(name = "Settings hub", widthDp = 1280, heightDp = 800)
@Composable
private fun SettingsHubPreview() {
    TotemTheme {
        SettingsHub(
            state = SettingsUiState(
                unlocked = true,
                currentSubdomain = "empresa-x",
                currentPortoNome = "Porto de Salvador",
                currentLanguage = AppLanguage.PtBr,
            ),
            onBack = {},
            onEditSubdomain = {},
            onEditPorto = {},
            onEditEmbarcacao = {},
            onEditPrinter = {},
            onEditLanguage = {},
            onEditPin = {},
        )
    }
}
