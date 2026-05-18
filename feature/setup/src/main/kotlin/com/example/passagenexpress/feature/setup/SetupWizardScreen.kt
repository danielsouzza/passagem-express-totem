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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passagenexpress.core.designsystem.component.TotemCard
import com.example.passagenexpress.core.designsystem.component.TotemErrorState
import com.example.passagenexpress.core.designsystem.component.TotemLoading
import com.example.passagenexpress.core.designsystem.component.TotemPrimaryButton
import com.example.passagenexpress.core.designsystem.component.TotemScreenScaffold
import com.example.passagenexpress.core.designsystem.component.TotemSecondaryButton
import com.example.passagenexpress.core.designsystem.theme.TotemTheme
import com.example.passagenexpress.core.domain.model.AppLanguage
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
        onSubdomainChange = viewModel::onSubdomainChange,
        onAdvanceFromSubdomain = viewModel::onAdvanceFromSubdomain,
        onRetryPortos = viewModel::onRetryPortos,
        onPortoSearchChange = viewModel::onPortoSearchChange,
        onPortoSelected = viewModel::onPortoSelected,
        onAdvanceFromPorto = viewModel::onAdvanceFromPorto,
        onLanguageSelected = viewModel::onLanguageSelected,
        onBack = viewModel::onBack,
        onFinish = viewModel::onFinish,
    )
}

@Composable
private fun SetupWizardContent(
    state: SetupWizardUiState,
    onSubdomainChange: (String) -> Unit,
    onAdvanceFromSubdomain: () -> Unit,
    onRetryPortos: () -> Unit,
    onPortoSearchChange: (String) -> Unit,
    onPortoSelected: (Porto) -> Unit,
    onAdvanceFromPorto: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val titleRes = when (state.step) {
        SetupStep.Subdomain -> R.string.setup_step_subdomain_title
        SetupStep.Porto -> R.string.setup_step_porto_title
        SetupStep.Language -> R.string.setup_step_language_title
    }
    val subtitleRes = when (state.step) {
        SetupStep.Subdomain -> R.string.setup_step_subdomain_subtitle
        SetupStep.Porto -> R.string.setup_step_porto_subtitle
        SetupStep.Language -> R.string.setup_step_language_subtitle
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
                onFinish = onFinish,
            )
        },
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
                        onValueChange = onSubdomainChange,
                        onSubmit = onAdvanceFromSubdomain,
                    )
                    SetupStep.Porto -> PortoStep(
                        portos = state.portos,
                        filteredPortos = state.filteredPortos,
                        searchQuery = state.portoSearchQuery,
                        selectedPortoId = state.selectedPorto?.id,
                        onSearchChange = onPortoSearchChange,
                        onPortoSelected = onPortoSelected,
                        onRetry = onRetryPortos,
                    )
                    SetupStep.Language -> LanguageStep(
                        selected = state.selectedLanguage,
                        onLanguageSelected = onLanguageSelected,
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
                text = stringRes(R.string.setup_continue),
                onClick = onAdvanceFromPorto,
                enabled = state.canAdvanceFromPorto,
            )
            SetupStep.Language -> TotemPrimaryButton(
                text = stringRes(R.string.setup_finish),
                onClick = onFinish,
                enabled = !state.isSaving,
            )
        }
    }
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
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
    ) {
        Text(
            text = stringRes(R.string.setup_subdomain_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(fraction = 0.6f),
            singleLine = true,
            placeholder = { Text(stringRes(R.string.setup_subdomain_hint)) },
            textStyle = MaterialTheme.typography.headlineSmall,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { onSubmit() },
            ),
        )
    }
}

@Composable
private fun PortoStep(
    portos: PortosState,
    filteredPortos: List<Porto>,
    searchQuery: String,
    selectedPortoId: Long?,
    onSearchChange: (String) -> Unit,
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
                        onQueryChange = onSearchChange,
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
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(stringRes(R.string.setup_porto_search_hint)) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringRes(R.string.setup_porto_clear_search),
                    )
                }
            }
        },
        textStyle = MaterialTheme.typography.titleMedium,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.None,
            imeAction = ImeAction.Search,
        ),
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
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)

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
            onSubdomainChange = {},
            onAdvanceFromSubdomain = {},
            onRetryPortos = {},
            onPortoSearchChange = {},
            onPortoSelected = {},
            onAdvanceFromPorto = {},
            onLanguageSelected = {},
            onBack = {},
            onFinish = {},
        )
    }
}

