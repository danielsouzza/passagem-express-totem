package com.example.passagenexpress.feature.passenger

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passagenexpress.core.designsystem.component.TotemAlphaKeypad
import com.example.passagenexpress.core.designsystem.component.TotemInputField
import com.example.passagenexpress.core.designsystem.component.TotemNumericKeypad
import com.example.passagenexpress.core.designsystem.component.TotemPrimaryButton
import com.example.passagenexpress.core.designsystem.component.TotemScreenScaffold
import com.example.passagenexpress.core.designsystem.component.TotemSecondaryButton
import com.example.passagenexpress.core.designsystem.theme.TotemTheme
import com.example.passagenexpress.core.domain.model.TipoDocumento

@Composable
fun PassengerScreen(
    onPassengersCompleted: (PassengerCompleted) -> Unit,
    onBack: () -> Unit,
    viewModel: PassengerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val completed = state.completed
    if (completed != null) {
        LaunchedEffect(completed) { onPassengersCompleted(completed) }
    }
    PassengerContent(
        state = state,
        onChangeTipoDoc = viewModel::onChangeTipoDocumento,
        onOpenKeypad = viewModel::onOpenKeypad,
        onCloseKeypad = viewModel::onCloseKeypad,
        onDigit = viewModel::onKeypadDigit,
        onChar = viewModel::onKeypadChar,
        onBackspace = viewModel::onKeypadBackspace,
        onDone = viewModel::onKeypadDone,
        onPrev = viewModel::onPrev,
        onNext = viewModel::onNext,
        onSubmit = viewModel::onSubmit,
        onBack = onBack,
        onDismissError = viewModel::dismissError,
    )
}

@Composable
private fun PassengerContent(
    state: PassengerUiState,
    onChangeTipoDoc: (TipoDocumento) -> Unit,
    onOpenKeypad: (KeypadField) -> Unit,
    onCloseKeypad: () -> Unit,
    onDigit: (Char) -> Unit,
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onDone: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
) {
    val activeForm = state.form
    TotemScreenScaffold(
        step = 5,
        title = stringRes(R.string.passenger_title),
        subtitle = if (state.forms.isEmpty()) null else stringResFmt(
            R.string.passenger_subtitle,
            state.activeIndex + 1,
            state.forms.size,
        ),
        footer = {
            TotemSecondaryButton(text = stringRes(R.string.passenger_back), onClick = onBack)
            Spacer(Modifier.width(TotemTheme.dimens.space12))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TotemPrimaryButton(
                    text = if (state.submitting) stringRes(R.string.passenger_continue_saving)
                    else stringRes(R.string.passenger_continue),
                    onClick = onSubmit,
                    enabled = !state.submitting,
                )
            }
        },
        stickyBottom = if (state.keypadField != null) ({
            KeypadSlot(
                state = state,
                onDigit = onDigit,
                onChar = onChar,
                onBackspace = onBackspace,
                onClose = onCloseKeypad,
                onDone = onDone,
            )
        }) else null,
    ) {
        if (activeForm == null) return@TotemScreenScaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        ) {
            FormFields(
                form = activeForm,
                cpfLookupInFlight = state.cpfLookupInFlight,
                formIndex = state.activeIndex,
                totalForms = state.forms.size,
                activeKeypad = state.keypadField,
                onChangeTipoDoc = onChangeTipoDoc,
                onOpenKeypad = onOpenKeypad,
                onRemove = { /* TODO: wire when remover semantics is defined */ },
            )
            if (state.forms.size > 1) {
                PrevNextRow(
                    canPrev = state.canGoPrev,
                    canNext = state.canGoNext,
                    onPrev = onPrev,
                    onNext = onNext,
                )
            }
            if (state.erroGeral != null) {
                ErrorBanner(message = stringRes(R.string.passenger_error_generic), onDismiss = onDismissError)
            }
            Spacer(Modifier.height(TotemTheme.dimens.space24))
        }
    }
}

@Composable
private fun FormFields(
    form: PassageiroForm,
    cpfLookupInFlight: Boolean,
    formIndex: Int,
    totalForms: Int,
    activeKeypad: KeypadField?,
    onChangeTipoDoc: (TipoDocumento) -> Unit,
    onOpenKeypad: (KeypadField) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
    ) {
        FormHeader(
            formIndex = formIndex,
            canRemove = totalForms > 1,
            onRemove = onRemove,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            DocTypeSelect(
                selected = form.tipoDocumento,
                onSelect = onChangeTipoDoc,
                modifier = Modifier.weight(1f),
            )
            TotemInputField(
                label = docLabelFor(form.tipoDocumento),
                value = form.documentoDisplay,
                error = errorMessageFor(form.errors.documento),
                focused = activeKeypad == KeypadField.Documento(formIndex),
                trailing = if (cpfLookupInFlight) ({ LoadingDot() }) else null,
                onClick = { onOpenKeypad(KeypadField.Documento(formIndex)) },
                modifier = Modifier.weight(1f),
            )
        }
        TotemInputField(
            label = stringRes(R.string.passenger_field_name),
            value = form.nome,
            error = errorMessageFor(form.errors.nome),
            focused = activeKeypad == KeypadField.Nome(formIndex),
            onClick = { onOpenKeypad(KeypadField.Nome(formIndex)) },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            TotemInputField(
                label = stringRes(R.string.passenger_field_phone),
                value = form.telefoneDisplay,
                error = errorMessageFor(form.errors.telefone),
                focused = activeKeypad == KeypadField.Telefone(formIndex),
                onClick = { onOpenKeypad(KeypadField.Telefone(formIndex)) },
                modifier = Modifier.weight(1f),
            )
            TotemInputField(
                label = stringRes(R.string.passenger_field_birthdate),
                placeholder = stringRes(R.string.passenger_field_birthdate_placeholder),
                value = form.nascimentoDisplay,
                error = errorMessageFor(form.errors.nascimento),
                focused = activeKeypad == KeypadField.Nascimento(formIndex),
                onClick = { onOpenKeypad(KeypadField.Nascimento(formIndex)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FormHeader(
    formIndex: Int,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResFmt(R.string.passenger_form_header, formIndex + 1),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringRes(R.string.passenger_form_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (canRemove) {
            RemoveButton(onClick = onRemove)
        }
    }
}

@Composable
private fun RemoveButton(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = TotemTheme.dimens.hairline,
            color = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = TotemTheme.dimens.space12,
                vertical = TotemTheme.dimens.space8,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space4),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringRes(R.string.passenger_remove),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DocTypeSelect(
    selected: TipoDocumento,
    onSelect: (TipoDocumento) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space4),
    ) {
        Box {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = TotemTheme.dimens.hairline,
                    color = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TotemTheme.dimens.touchTarget)
                    .clickable { expanded = true },
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = TotemTheme.dimens.space16,
                        vertical = TotemTheme.dimens.space12,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringRes(R.string.passenger_field_doc_type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = docLabelFor(selected),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                TipoDocumento.entries.forEach { tipo ->
                    DropdownMenuItem(
                        text = { Text(docLabelFor(tipo)) },
                        onClick = {
                            onSelect(tipo)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PrevNextRow(
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StepIconButton(
            enabled = canPrev,
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            label = stringRes(R.string.passenger_prev),
            onClick = onPrev,
        )
        StepIconButton(
            enabled = canNext,
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            label = stringRes(R.string.passenger_next),
            onClick = onNext,
            trailingIcon = true,
        )
    }
}

@Composable
private fun StepIconButton(
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    trailingIcon: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = Modifier
            .heightIn(min = TotemTheme.dimens.touchTarget)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = TotemTheme.dimens.space20,
                vertical = TotemTheme.dimens.space12,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
        ) {
            val tint = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
            if (!trailingIcon) Icon(imageVector = icon, contentDescription = null, tint = tint)
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = tint)
            if (trailingIcon) Icon(imageVector = icon, contentDescription = null, tint = tint)
        }
    }
}

@Composable
private fun KeypadSlot(
    state: PassengerUiState,
    onDigit: (Char) -> Unit,
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    val field = state.keypadField ?: return
    val form = state.forms.getOrNull(passengerIndexOf(field)) ?: return
    val doneLabel = stringRes(R.string.passenger_keypad_done)
    val closeDesc = stringRes(R.string.passenger_keypad_close)
    val deleteDesc = stringRes(R.string.passenger_keypad_delete)
    when (field) {
        is KeypadField.Nome -> TotemAlphaKeypad(
            label = stringRes(R.string.passenger_field_name),
            currentValue = form.nome,
            onChar = onChar,
            onBackspace = onBackspace,
            onClose = onClose,
            onDone = onDone,
            doneLabel = doneLabel,
            closeContentDesc = closeDesc,
            deleteContentDesc = deleteDesc,
            spaceContentDesc = stringRes(R.string.passenger_keypad_space),
            shiftContentDesc = stringRes(R.string.passenger_keypad_shift),
        )
        else -> {
            val (label, current) = when (field) {
                is KeypadField.Documento -> docLabelFor(form.tipoDocumento) to form.documentoDisplay
                is KeypadField.Telefone -> stringRes(R.string.passenger_field_phone) to form.telefoneDisplay
                is KeypadField.Nascimento -> stringRes(R.string.passenger_field_birthdate) to form.nascimentoDisplay
                is KeypadField.Nome -> return // unreachable
            }
            TotemNumericKeypad(
                label = label,
                currentValue = current,
                onDigit = onDigit,
                onBackspace = onBackspace,
                onClose = onClose,
                onDone = onDone,
                doneLabel = doneLabel,
                closeContentDesc = closeDesc,
                deleteContentDesc = deleteDesc,
            )
        }
    }
}

private fun passengerIndexOf(field: KeypadField): Int = when (field) {
    is KeypadField.Documento -> field.formIndex
    is KeypadField.Telefone -> field.formIndex
    is KeypadField.Nascimento -> field.formIndex
    is KeypadField.Nome -> field.formIndex
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
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

@Composable
private fun LoadingDot() {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun docLabelFor(tipo: TipoDocumento): String = when (tipo) {
    TipoDocumento.CPF -> stringRes(R.string.passenger_field_cpf)
    TipoDocumento.RG -> stringRes(R.string.passenger_field_document)
}

@Composable
private fun errorMessageFor(code: String?): String? = when (code) {
    PassengerViewModel.ERROR_DOC_REQUIRED -> stringRes(R.string.passenger_error_doc_required)
    PassengerViewModel.ERROR_DOC_INVALID -> stringRes(R.string.passenger_error_doc_invalid)
    PassengerViewModel.ERROR_NAME_REQUIRED -> stringRes(R.string.passenger_error_name_required)
    PassengerViewModel.ERROR_PHONE_REQUIRED -> stringRes(R.string.passenger_error_phone_required)
    PassengerViewModel.ERROR_PHONE_INVALID -> stringRes(R.string.passenger_error_phone_invalid)
    PassengerViewModel.ERROR_BIRTH_REQUIRED -> stringRes(R.string.passenger_error_birthdate_required)
    PassengerViewModel.ERROR_BIRTH_INVALID -> stringRes(R.string.passenger_error_birthdate_invalid)
    null -> null
    else -> null
}

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)

@Composable
private fun stringResFmt(id: Int, vararg args: Any): String =
    androidx.compose.ui.res.stringResource(id, *args)
