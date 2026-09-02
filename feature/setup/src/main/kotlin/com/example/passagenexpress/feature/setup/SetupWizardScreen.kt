package com.example.passagenexpress.feature.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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

@Composable
fun SetupWizardScreen(
    onSetupCompleted: () -> Unit,
    viewModel: SetupWizardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.completed) {
        if (state.completed) onSetupCompleted()
    }

    SetupWizardContent(
        state = state,
        onAdvanceFromSubdomain = viewModel::onAdvanceFromSubdomain,
        onRetryPortos = viewModel::onRetryPortos,
        onPortoSearchChange = viewModel::onPortoSearchChange,
        onPortoSelected = viewModel::onPortoSelected,
        onAdvanceFromPorto = viewModel::onAdvanceFromPorto,
        onRetryEmbarcacoes = viewModel::onRetryEmbarcacoes,
        onEmbarcacaoSearchChange = viewModel::onEmbarcacaoSearchChange,
        onEmbarcacaoSelected = viewModel::onEmbarcacaoSelected,
        onAdvanceFromEmbarcacao = viewModel::onAdvanceFromEmbarcacao,
        onRefreshPrinters = viewModel::refreshPrinters,
        onPrinterSelected = viewModel::onPrinterSelected,
        onPaperWidthSelected = viewModel::onPaperWidthSelected,
        onTestPrint = viewModel::onTestPrint,
        onAdvanceFromPrinter = viewModel::onAdvanceFromPrinter,
        onLanguageSelected = viewModel::onLanguageSelected,
        onAdvanceFromLanguage = viewModel::onAdvanceFromLanguage,
        onBack = viewModel::onBack,
        onFinish = viewModel::onFinish,
        onOpenKeypad = viewModel::onOpenKeypad,
        onKeypadChar = viewModel::onKeypadChar,
        onKeypadBackspace = viewModel::onKeypadBackspace,
        onCloseKeypad = viewModel::onCloseKeypad,
    )
}

@Composable
private fun SetupWizardContent(
    state: SetupWizardUiState,
    onAdvanceFromSubdomain: () -> Unit,
    onRetryPortos: () -> Unit,
    onPortoSearchChange: (String) -> Unit,
    onPortoSelected: (Porto) -> Unit,
    onAdvanceFromPorto: () -> Unit,
    onRetryEmbarcacoes: () -> Unit,
    onEmbarcacaoSearchChange: (String) -> Unit,
    onEmbarcacaoSelected: (Embarcacao) -> Unit,
    onAdvanceFromEmbarcacao: () -> Unit,
    onRefreshPrinters: () -> Unit,
    onPrinterSelected: (com.example.passagenexpress.core.domain.printer.UsbPrinterDevice) -> Unit,
    onPaperWidthSelected: (PaperWidth) -> Unit,
    onTestPrint: () -> Unit,
    onAdvanceFromPrinter: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onAdvanceFromLanguage: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onOpenKeypad: (SetupKeypadField) -> Unit,
    onKeypadChar: (Char) -> Unit,
    onKeypadBackspace: () -> Unit,
    onCloseKeypad: () -> Unit,
) {
    val titleRes = when (state.step) {
        SetupStep.Subdomain -> R.string.setup_step_subdomain_title
        SetupStep.Porto -> R.string.setup_step_porto_title
        SetupStep.Embarcacao -> R.string.setup_step_embarcacao_title
        SetupStep.Printer -> R.string.setup_step_printer_title
        SetupStep.Language -> R.string.setup_step_language_title
        SetupStep.Pin -> R.string.setup_step_pin_title
    }
    val subtitleRes = when (state.step) {
        SetupStep.Subdomain -> R.string.setup_step_subdomain_subtitle
        SetupStep.Porto -> R.string.setup_step_porto_subtitle
        SetupStep.Embarcacao -> R.string.setup_step_embarcacao_subtitle
        SetupStep.Printer -> R.string.setup_step_printer_subtitle
        SetupStep.Language -> R.string.setup_step_language_subtitle
        SetupStep.Pin -> R.string.setup_step_pin_subtitle
    }

    TotemScreenScaffold(
        title = stringRes(titleRes),
        subtitle = stringRes(subtitleRes),
        footer = {
            WizardFooter(
                state = state,
                onBack = onBack,
                onAdvanceFromSubdomain = onAdvanceFromSubdomain,
                onAdvanceFromPorto = onAdvanceFromPorto,
                onAdvanceFromEmbarcacao = onAdvanceFromEmbarcacao,
                onAdvanceFromPrinter = onAdvanceFromPrinter,
                onAdvanceFromLanguage = onAdvanceFromLanguage,
                onFinish = onFinish,
            )
        },
        stickyBottom = if (state.keypadField != null) ({
            SetupKeypadSlot(
                state = state,
                onChar = onKeypadChar,
                onBackspace = onKeypadBackspace,
                onClose = onCloseKeypad,
                onDone = onCloseKeypad,
            )
        }) else null,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StepIndicator(currentStep = state.step)
            Spacer(Modifier.size(TotemTheme.dimens.space24))
            AnimatedContent(
                targetState = state.step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "setupStepContent",
            ) { step ->
                when (step) {
                    SetupStep.Subdomain -> SubdomainStep(
                        value = state.subdomainInput,
                        focused = state.keypadField == SetupKeypadField.Subdomain,
                        onClick = { onOpenKeypad(SetupKeypadField.Subdomain) },
                    )
                    SetupStep.Porto -> PortoStep(
                        portos = state.portos,
                        filteredPortos = state.filteredPortos,
                        searchQuery = state.portoSearchQuery,
                        selectedPortoId = state.selectedPorto?.id,
                        searchFocused = state.keypadField == SetupKeypadField.PortoSearch,
                        onSearchClick = { onOpenKeypad(SetupKeypadField.PortoSearch) },
                        onClearSearch = { onPortoSearchChange("") },
                        onPortoSelected = onPortoSelected,
                        onRetry = onRetryPortos,
                    )
                    SetupStep.Embarcacao -> EmbarcacaoStep(
                        embarcacoes = state.embarcacoes,
                        filteredEmbarcacoes = state.filteredEmbarcacoes,
                        searchQuery = state.embarcacaoSearchQuery,
                        selectedEmbarcacaoId = state.selectedEmbarcacao?.id,
                        searchFocused = state.keypadField == SetupKeypadField.EmbarcacaoSearch,
                        onSearchClick = { onOpenKeypad(SetupKeypadField.EmbarcacaoSearch) },
                        onClearSearch = { onEmbarcacaoSearchChange("") },
                        onEmbarcacaoSelected = onEmbarcacaoSelected,
                        onRetry = onRetryEmbarcacoes,
                    )
                    SetupStep.Printer -> PrinterStep(
                        printers = state.printers,
                        selectedPrinter = state.selectedPrinter,
                        selectedPaperWidth = state.selectedPaperWidth,
                        testState = state.printerTest,
                        onRefresh = onRefreshPrinters,
                        onPrinterSelected = onPrinterSelected,
                        onPaperWidthSelected = onPaperWidthSelected,
                        onTestPrint = onTestPrint,
                    )
                    SetupStep.Language -> LanguageStep(
                        selected = state.selectedLanguage,
                        onLanguageSelected = onLanguageSelected,
                    )
                    SetupStep.Pin -> PinStep(
                        pinLength = state.pinInput.length,
                        focused = state.keypadField == SetupKeypadField.Pin,
                        onClick = { onOpenKeypad(SetupKeypadField.Pin) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WizardFooter(
    state: SetupWizardUiState,
    onBack: () -> Unit,
    onAdvanceFromSubdomain: () -> Unit,
    onAdvanceFromPorto: () -> Unit,
    onAdvanceFromEmbarcacao: () -> Unit,
    onAdvanceFromPrinter: () -> Unit,
    onAdvanceFromLanguage: () -> Unit,
    onFinish: () -> Unit,
) {
    if (state.step != SetupStep.Subdomain) {
        TotemSecondaryButton(text = stringRes(R.string.setup_back), onClick = onBack)
        Spacer(Modifier.width(TotemTheme.dimens.space12))
    }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        when (state.step) {
            SetupStep.Subdomain -> TotemPrimaryButton(
                text = stringRes(R.string.setup_continue),
                onClick = onAdvanceFromSubdomain,
                enabled = state.canAdvanceFromSubdomain,
            )
            SetupStep.Porto -> TotemPrimaryButton(
                text = if (state.selectedPorto == null) {
                    stringRes(R.string.setup_porto_skip)
                } else {
                    stringRes(R.string.setup_continue)
                },
                onClick = onAdvanceFromPorto,
                enabled = state.canAdvanceFromPorto,
            )
            SetupStep.Embarcacao -> TotemPrimaryButton(
                text = if (state.selectedEmbarcacao == null) {
                    stringRes(R.string.setup_embarcacao_skip)
                } else {
                    stringRes(R.string.setup_continue)
                },
                onClick = onAdvanceFromEmbarcacao,
                enabled = state.canAdvanceFromEmbarcacao,
            )
            SetupStep.Printer -> TotemPrimaryButton(
                text = if (state.selectedPrinter == null) {
                    stringRes(R.string.setup_printer_skip)
                } else {
                    stringRes(R.string.setup_continue)
                },
                onClick = onAdvanceFromPrinter,
                enabled = state.canAdvanceFromPrinter,
            )
            SetupStep.Language -> TotemPrimaryButton(
                text = stringRes(R.string.setup_continue),
                onClick = onAdvanceFromLanguage,
                enabled = state.canAdvanceFromLanguage,
            )
            SetupStep.Pin -> TotemPrimaryButton(
                text = stringRes(R.string.setup_finish),
                onClick = onFinish,
                enabled = state.canFinish && !state.isSaving,
            )
        }
    }
}

@Composable
private fun PrinterStep(
    printers: List<com.example.passagenexpress.core.domain.printer.UsbPrinterDevice>,
    selectedPrinter: com.example.passagenexpress.core.domain.printer.UsbPrinterDevice?,
    selectedPaperWidth: PaperWidth,
    testState: PrinterTestState,
    onRefresh: () -> Unit,
    onPrinterSelected: (com.example.passagenexpress.core.domain.printer.UsbPrinterDevice) -> Unit,
    onPaperWidthSelected: (PaperWidth) -> Unit,
    onTestPrint: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
    ) {
        PaperWidthSelector(selected = selectedPaperWidth, onSelected = onPaperWidthSelected)
        TotemSecondaryButton(text = stringRes(R.string.setup_printer_refresh), onClick = onRefresh)
        if (printers.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringRes(R.string.setup_printer_empty),
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
                    stringRes(R.string.setup_printer_testing)
                } else {
                    stringRes(R.string.setup_printer_test)
                },
                onClick = onTestPrint,
                enabled = selectedPrinter != null && testState !is PrinterTestState.Testing,
                trailingArrow = false,
            )
            when (testState) {
                is PrinterTestState.Success -> Text(
                    text = stringRes(R.string.setup_printer_test_ok),
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
private fun PaperWidthSelector(
    selected: PaperWidth,
    onSelected: (PaperWidth) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8)) {
        Text(
            text = stringRes(R.string.setup_paper_width_title),
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
                            text = width.mmLabel(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResFmt(R.string.setup_paper_width_columns, width.columns),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun PaperWidth.mmLabel(): String = when (this) {
    PaperWidth.MM58 -> "58 mm"
    PaperWidth.MM80 -> "80 mm"
}

@Composable
private fun StepIndicator(currentStep: SetupStep) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SetupStep.entries.forEach { step ->
            val active = step.ordinal <= currentStep.ordinal
            Surface(
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
                modifier = Modifier.size(if (step == currentStep) 16.dp else 12.dp),
            ) {}
        }
    }
}

@Composable
private fun SubdomainStep(
    value: String,
    focused: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
    ) {
        TotemInputField(
            label = stringRes(R.string.setup_subdomain_label),
            value = value,
            placeholder = stringRes(R.string.setup_subdomain_hint),
            focused = focused,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(fraction = 0.6f),
        )
    }
}

@Composable
private fun PortoStep(
    portos: PortosState,
    filteredPortos: List<Porto>,
    searchQuery: String,
    selectedPortoId: Long?,
    searchFocused: Boolean,
    onSearchClick: () -> Unit,
    onClearSearch: () -> Unit,
    onPortoSelected: (Porto) -> Unit,
    onRetry: () -> Unit,
) {
    when (portos) {
        PortosState.Idle, PortosState.Loading -> TotemLoading(label = stringRes(R.string.setup_loading_portos))
        is PortosState.Error -> TotemErrorState(
            title = stringRes(R.string.setup_load_error_title),
            message = portos.message,
            actionLabel = stringRes(R.string.setup_retry),
            onAction = onRetry,
        )
        is PortosState.Loaded -> {
            if (portos.portos.isEmpty()) {
                TotemErrorState(
                    title = stringRes(R.string.setup_no_portos_title),
                    message = stringRes(R.string.setup_no_portos_message),
                    actionLabel = stringRes(R.string.setup_retry),
                    onAction = onRetry,
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
                ) {
                    PortoSearchField(
                        query = searchQuery,
                        focused = searchFocused,
                        onClick = onSearchClick,
                        onClear = onClearSearch,
                    )
                    if (filteredPortos.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringRes(R.string.setup_porto_search_empty),
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
                                PortoCard(
                                    porto = porto,
                                    selected = selectedPortoId == porto.id,
                                    onClick = { onPortoSelected(porto) },
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
private fun PortoSearchField(
    query: String,
    focused: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    TotemInputField(
        label = stringRes(R.string.setup_porto_search_hint),
        value = query,
        placeholder = stringRes(R.string.setup_tap_to_type),
        focused = focused,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        trailing = if (query.isNotEmpty()) ({
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringRes(R.string.setup_porto_clear_search),
                )
            }
        }) else ({
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        }),
    )
}

@Composable
private fun PortoCard(porto: Porto, selected: Boolean, onClick: () -> Unit) {
    TotemCard(selected = selected, onClick = onClick) {
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

@Composable
private fun EmbarcacaoStep(
    embarcacoes: EmbarcacoesState,
    filteredEmbarcacoes: List<Embarcacao>,
    searchQuery: String,
    selectedEmbarcacaoId: Long?,
    searchFocused: Boolean,
    onSearchClick: () -> Unit,
    onClearSearch: () -> Unit,
    onEmbarcacaoSelected: (Embarcacao) -> Unit,
    onRetry: () -> Unit,
) {
    when (embarcacoes) {
        EmbarcacoesState.Idle, EmbarcacoesState.Loading ->
            TotemLoading(label = stringRes(R.string.setup_loading_embarcacoes))
        is EmbarcacoesState.Error -> TotemErrorState(
            title = stringRes(R.string.setup_load_error_title),
            message = embarcacoes.message,
            actionLabel = stringRes(R.string.setup_retry),
            onAction = onRetry,
        )
        is EmbarcacoesState.Loaded -> {
            if (embarcacoes.embarcacoes.isEmpty()) {
                TotemErrorState(
                    title = stringRes(R.string.setup_no_embarcacoes_title),
                    message = stringRes(R.string.setup_no_embarcacoes_message),
                    actionLabel = stringRes(R.string.setup_retry),
                    onAction = onRetry,
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
                ) {
                    EmbarcacaoSearchField(
                        query = searchQuery,
                        focused = searchFocused,
                        onClick = onSearchClick,
                        onClear = onClearSearch,
                    )
                    if (filteredEmbarcacoes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringRes(R.string.setup_embarcacao_search_empty),
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
                                EmbarcacaoCard(
                                    embarcacao = embarcacao,
                                    selected = selectedEmbarcacaoId == embarcacao.id,
                                    onClick = { onEmbarcacaoSelected(embarcacao) },
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
private fun EmbarcacaoSearchField(
    query: String,
    focused: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    TotemInputField(
        label = stringRes(R.string.setup_embarcacao_search_hint),
        value = query,
        placeholder = stringRes(R.string.setup_tap_to_type),
        focused = focused,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        trailing = if (query.isNotEmpty()) ({
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringRes(R.string.setup_porto_clear_search),
                )
            }
        }) else ({
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        }),
    )
}

@Composable
private fun EmbarcacaoCard(embarcacao: Embarcacao, selected: Boolean, onClick: () -> Unit) {
    TotemCard(selected = selected, onClick = onClick) {
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

@Composable
private fun LanguageStep(
    selected: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space20),
        modifier = Modifier.fillMaxWidth(),
    ) {
        LanguageCard(
            label = stringRes(R.string.setup_language_pt),
            selected = selected == AppLanguage.PtBr,
            onClick = { onLanguageSelected(AppLanguage.PtBr) },
            modifier = Modifier.weight(1f),
        )
        LanguageCard(
            label = stringRes(R.string.setup_language_en),
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
private fun PinStep(pinLength: Int, focused: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
    ) {
        TotemInputField(
            label = stringRes(R.string.setup_pin_label),
            value = "•".repeat(pinLength),
            placeholder = stringRes(R.string.setup_pin_hint),
            focused = focused,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(fraction = 0.4f),
        )
    }
}

@Composable
private fun SetupKeypadSlot(
    state: SetupWizardUiState,
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    val field = state.keypadField ?: return
    // PIN usa teclado numérico; subdomínio e busca de porto usam o alfanumérico.
    if (field == SetupKeypadField.Pin) {
        TotemNumericKeypad(
            label = stringRes(R.string.setup_pin_label),
            currentValue = "•".repeat(state.pinInput.length),
            onDigit = onChar,
            onBackspace = onBackspace,
            onClose = onClose,
            onDone = onDone,
            doneLabel = stringRes(R.string.setup_keypad_done),
            closeContentDesc = stringRes(R.string.setup_keypad_close),
            deleteContentDesc = stringRes(R.string.setup_keypad_delete),
        )
        return
    }
    val (label, current) = when (field) {
        SetupKeypadField.Subdomain ->
            stringRes(R.string.setup_subdomain_label) to state.subdomainInput
        SetupKeypadField.PortoSearch ->
            stringRes(R.string.setup_porto_search_hint) to state.portoSearchQuery
        SetupKeypadField.EmbarcacaoSearch ->
            stringRes(R.string.setup_embarcacao_search_hint) to state.embarcacaoSearchQuery
        SetupKeypadField.Pin -> return
    }
    TotemAlphaKeypad(
        label = label,
        currentValue = current,
        onChar = onChar,
        onBackspace = onBackspace,
        onClose = onClose,
        onDone = onDone,
        doneLabel = stringRes(R.string.setup_keypad_done),
        closeContentDesc = stringRes(R.string.setup_keypad_close),
        deleteContentDesc = stringRes(R.string.setup_keypad_delete),
        spaceContentDesc = stringRes(R.string.setup_keypad_space),
        shiftContentDesc = stringRes(R.string.setup_keypad_shift),
    )
}

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)

@Composable
private fun stringResFmt(id: Int, vararg args: Any): String =
    androidx.compose.ui.res.stringResource(id, *args)

@Preview(name = "Setup wizard", widthDp = 1280, heightDp = 800)
@Composable
private fun SetupWizardPreview() {
    TotemTheme {
        SetupWizardContent(
            state = SetupWizardUiState(
                step = SetupStep.Porto,
                subdomainInput = "demo",
                portos = PortosState.Loaded(
                    listOf(
                        Porto(1, "porto-salvador", "Porto de Salvador", "29270", "Salvador"),
                        Porto(2, "porto-mar-grande", "Mar Grande", "29280", "Vera Cruz"),
                    )
                ),
                selectedPorto = Porto(1, "porto-salvador", "Porto de Salvador", "29270", "Salvador"),
            ),
            onAdvanceFromSubdomain = {},
            onRetryPortos = {},
            onPortoSearchChange = {},
            onPortoSelected = {},
            onAdvanceFromPorto = {},
            onRetryEmbarcacoes = {},
            onEmbarcacaoSearchChange = {},
            onEmbarcacaoSelected = {},
            onAdvanceFromEmbarcacao = {},
            onRefreshPrinters = {},
            onPrinterSelected = {},
            onPaperWidthSelected = {},
            onTestPrint = {},
            onAdvanceFromPrinter = {},
            onLanguageSelected = {},
            onAdvanceFromLanguage = {},
            onBack = {},
            onFinish = {},
            onOpenKeypad = {},
            onKeypadChar = {},
            onKeypadBackspace = {},
            onCloseKeypad = {},
        )
    }
}

