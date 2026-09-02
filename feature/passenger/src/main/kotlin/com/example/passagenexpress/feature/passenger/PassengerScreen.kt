package com.example.passagenexpress.feature.passenger

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passagenexpress.core.designsystem.component.ProvideDialogLocale
import com.example.passagenexpress.core.designsystem.component.TotemAlphaKeypad
import com.example.passagenexpress.core.designsystem.component.TotemInputField
import com.example.passagenexpress.core.designsystem.component.TotemNumericKeypad
import com.example.passagenexpress.core.designsystem.component.TotemPrimaryButton
import com.example.passagenexpress.core.designsystem.component.TotemScreenScaffold
import com.example.passagenexpress.core.designsystem.component.TotemSecondaryButton
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
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
        LaunchedEffect(completed) {
            onPassengersCompleted(completed)
            viewModel.onCompletedHandled()
        }
    }
    PassengerContent(
        state = state,
        onChangeTipoDoc = viewModel::onChangeTipoDocumento,
        onOpenKeypad = viewModel::onOpenKeypad,
        onCloseKeypad = viewModel::onCloseKeypad,
        onDigit = viewModel::onKeypadDigit,
        onChar = viewModel::onKeypadChar,
        onBackspace = viewModel::onKeypadBackspace,
        onInlineDone = viewModel::onKeypadDone,
        onOpenDocModal = viewModel::onOpenDocModal,
        onCloseModal = viewModel::onCloseModal,
        onModalNext = viewModel::onModalNext,
        onEditPassenger = viewModel::onEditPassenger,
        onRemovePassenger = viewModel::onRemovePassenger,
        onRemoveActiveOccupant = viewModel::onRemoveActiveOccupant,
        onRestoreOccupant = viewModel::onRestoreOccupant,
        onAdvance = viewModel::onAdvance,
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
    onInlineDone: () -> Unit,
    onOpenDocModal: () -> Unit,
    onCloseModal: () -> Unit,
    onModalNext: () -> Unit,
    onEditPassenger: (Int) -> Unit,
    onRemovePassenger: (Int) -> Unit,
    onRemoveActiveOccupant: () -> Unit,
    onRestoreOccupant: (Int) -> Unit,
    onAdvance: () -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
) {
    val activeForm = state.form
    val inlineKeypad = state.keypadField
    // Há próximo passageiro quando existe algum assento não removido depois do ativo.
    val hasNext = state.forms.indices.any { it > state.activeIndex && !state.forms[it].skipped }
    TotemScreenScaffold(
        step = 3,
        totalSteps = 4,
        title = stringRes(R.string.passenger_screen_title),
        subtitle = stringRes(R.string.passenger_screen_subtitle),
        footer = {
            TotemSecondaryButton(text = stringRes(R.string.passenger_back), onClick = onBack)
            Spacer(Modifier.width(TotemTheme.dimens.space12))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TotemPrimaryButton(
                    text = when {
                        state.submitting -> stringRes(R.string.passenger_continue_saving)
                        hasNext -> stringRes(R.string.passenger_next_passenger)
                        else -> stringRes(R.string.passenger_continue)
                    },
                    onClick = onAdvance,
                    enabled = !state.submitting,
                )
            }
        },
        // Teclado inline pra editar um campo do assento ativo (quando o modal guiado está fechado).
        stickyBottom = if (inlineKeypad != null && activeForm != null && state.modalStep == null) ({
            val (alpha, label, current) = keypadContentFor(inlineKeypad, activeForm)
            FieldKeypad(
                alpha = alpha,
                label = label,
                currentValue = current,
                doneLabel = stringRes(R.string.passenger_keypad_done),
                onDigit = onDigit,
                onChar = onChar,
                onBackspace = onBackspace,
                onClose = onCloseKeypad,
                onDone = onInlineDone,
            )
        }) else null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space24),
        ) {
            // Assentos agrupados por cômodo (igual à referência): cabeçalho + cards por passageiro.
            state.forms.withIndex()
                .groupBy { it.value.comodo.id }
                .values
                .forEach { entries ->
                    val comodo = entries.first().value.comodo
                    Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12)) {
                        ComodoHeader(
                            nome = comodoLabel(comodo),
                            initial = comodoInitial(comodo),
                            seatCount = entries.size,
                        )
                        entries.forEachIndexed { seatPos, indexed ->
                            val index = indexed.index
                            val form = indexed.value
                            val extra = seatPos > 0
                            SeatCard(
                                form = form,
                                titular = !extra,
                                optional = extra,
                                filled = state.isFilled(form),
                                active = index == state.activeIndex,
                                formIndex = index,
                                cpfLookupInFlight = state.cpfLookupInFlight,
                                activeKeypad = state.keypadField,
                                onFill = { onEditPassenger(index) },
                                onRemove = if (extra) ({ onRemovePassenger(index) }) else null,
                                onRestore = { onRestoreOccupant(index) },
                                onOpenKeypad = onOpenKeypad,
                                onOpenDocModal = onOpenDocModal,
                            )
                        }
                    }
                }
            if (state.erroGeral != null) {
                ErrorBanner(message = stringRes(R.string.passenger_error_generic), onDismiss = onDismissError)
            }
            Spacer(Modifier.height(TotemTheme.dimens.space24))
        }
    }

    if (state.modalStep != null && activeForm != null) {
        DocumentModal(
            step = state.modalStep,
            form = activeForm,
            forms = state.forms,
            position = state.activeIndex + 1,
            isExtra = state.isExtraOccupant(state.activeIndex),
            cpfLookupInFlight = state.cpfLookupInFlight,
            onChangeTipoDoc = onChangeTipoDoc,
            onDigit = onDigit,
            onChar = onChar,
            onBackspace = onBackspace,
            onNext = onModalNext,
            onRemoveOccupant = onRemoveActiveOccupant,
            onClose = onCloseModal,
        )
    }
}

// ---------------------------------------------------------------------------
// Lista de passageiros: resumo (preenchido) + card pendente
// ---------------------------------------------------------------------------

/** Passageiro já preenchido — linha compacta com nome/documento; toca pra editar (reabre o modal). */
private val SeatFilledBg = androidx.compose.ui.graphics.Color(0xFFF3FAF4)
private val SeatFilledBorder = androidx.compose.ui.graphics.Color(0xFFCBE3D2)

/** Cabeçalho do cômodo: ícone com inicial + nome + nº de lugares. */
@Composable
private fun ComodoHeader(nome: String, initial: String, seatCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(TotemPalette.Primary100),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TotemPalette.Ink2,
            )
        }
        Text(
            text = nome,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TotemPalette.Ink,
        )
        Text(
            text = if (seatCount > 1) stringResFmt(R.string.passenger_seats_many, seatCount)
            else stringRes(R.string.passenger_seats_one),
            style = MaterialTheme.typography.labelMedium,
            color = TotemPalette.InkMuted,
        )
    }
}

/**
 * Card de assento. Quando é o ativo, expande o **formulário inline** pra preencher/editar
 * (documento abre o modal guiado; demais campos usam o teclado inline). Fora disso:
 * preenchido → resumo + Editar; vazio → "Preencher dados"; removido → placeholder + Adicionar.
 */
@Composable
private fun SeatCard(
    form: PassageiroForm,
    titular: Boolean,
    optional: Boolean,
    filled: Boolean,
    active: Boolean,
    formIndex: Int,
    cpfLookupInFlight: Boolean,
    activeKeypad: KeypadField?,
    onFill: () -> Unit,
    onRemove: (() -> Unit)?,
    onRestore: () -> Unit,
    onOpenKeypad: (KeypadField) -> Unit,
    onOpenDocModal: () -> Unit,
) {
    val skipped = form.skipped
    val expanded = active && !skipped
    val border = when {
        skipped -> TotemPalette.Hairline
        expanded -> TotemPalette.Accent
        filled -> SeatFilledBorder
        titular -> TotemPalette.Primary100
        else -> TotemPalette.Hairline
    }
    val bg = when {
        skipped -> TotemPalette.PaperWarm
        filled && !expanded -> SeatFilledBg
        else -> TotemPalette.Paper
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(if (expanded) 2.dp else 1.5.dp, border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(TotemTheme.dimens.space16),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        ) {
            // Cabeçalho: papel do assento (título maior) + selo grátis + check quando preenchido.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
            ) {
                Text(
                    text = if (titular) stringRes(R.string.passenger_role_titular)
                    else stringRes(R.string.passenger_role_companion),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TotemPalette.Ink,
                )
                if (optional) {
                    Surface(shape = RoundedCornerShape(6.dp), color = TotemPalette.SuccessLight) {
                        Text(
                            text = stringRes(R.string.passenger_included_free).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TotemPalette.Success,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (filled && !expanded && !skipped) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = TotemPalette.Success,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            when {
                expanded -> {
                    FormFields(
                        form = form,
                        formIndex = formIndex,
                        cpfLookupInFlight = cpfLookupInFlight,
                        activeKeypad = activeKeypad,
                        onOpenKeypad = onOpenKeypad,
                        onOpenDocModal = onOpenDocModal,
                    )
                    if (optional && onRemove != null) {
                        OutlinedPillButton(text = stringRes(R.string.passenger_alone), onClick = onRemove)
                    }
                }

                skipped -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
                ) {
                    Text(
                        text = stringRes(R.string.passenger_removed_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TotemPalette.InkMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringRes(R.string.passenger_add_back),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TotemPalette.Accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(TotemTheme.dimens.radiusPill))
                            .clickable(onClick = onRestore)
                            .padding(horizontal = TotemTheme.dimens.space12, vertical = TotemTheme.dimens.space8),
                    )
                }

                filled -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = form.nome,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TotemPalette.Ink,
                            maxLines = 1,
                        )
                        Text(
                            text = "${docLabelFor(form.tipoDocumento)} ${form.documentoDisplay}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TotemPalette.InkMuted,
                        )
                    }
                    Text(
                        text = stringRes(R.string.passenger_edit),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TotemPalette.Accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(TotemTheme.dimens.radiusPill))
                            .clickable(onClick = onFill)
                            .padding(horizontal = TotemTheme.dimens.space16, vertical = TotemTheme.dimens.space8),
                    )
                }

                else -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
                ) {
                    TotemPrimaryButton(
                        text = stringRes(R.string.passenger_fill_data),
                        onClick = onFill,
                        trailingArrow = false,
                        leading = {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    Spacer(Modifier.weight(1f))
                    if (optional && onRemove != null) {
                        OutlinedPillButton(
                            text = stringRes(R.string.passenger_alone),
                            onClick = onRemove,
                        )
                    }
                }
            }
        }
    }
}

/** Formulário inline do assento ativo: campos tocáveis (documento abre o modal; resto, teclado). */
@Composable
private fun FormFields(
    form: PassageiroForm,
    formIndex: Int,
    cpfLookupInFlight: Boolean,
    activeKeypad: KeypadField?,
    onOpenKeypad: (KeypadField) -> Unit,
    onOpenDocModal: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
    ) {
        TotemInputField(
            label = docLabelFor(form.tipoDocumento),
            value = form.documentoDisplay,
            error = errorMessageFor(form.errors.documento),
            focused = activeKeypad == KeypadField.Documento(formIndex),
            trailing = if (cpfLookupInFlight) ({ LoadingDot() }) else null,
            onClick = onOpenDocModal,
        )
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

private fun comodoLabel(comodo: com.example.passagenexpress.core.domain.model.Comodo): String =
    comodo.nome?.takeIf { it.isNotBlank() }
        ?: comodo.numeracao?.let { "Assento $it" }
        ?: "Cômodo"

private fun comodoInitial(comodo: com.example.passagenexpress.core.domain.model.Comodo): String =
    comodoLabel(comodo).trim().firstOrNull()?.uppercase() ?: "•"


// ---------------------------------------------------------------------------
// Modal guiado (documento → nome → telefone → nascimento)
// ---------------------------------------------------------------------------

@Composable
private fun DocumentModal(
    step: PassengerModalStep,
    form: PassageiroForm,
    forms: List<PassageiroForm>,
    position: Int,
    isExtra: Boolean,
    cpfLookupInFlight: Boolean,
    onChangeTipoDoc: (TipoDocumento) -> Unit,
    onDigit: (Char) -> Unit,
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onNext: () -> Unit,
    onRemoveOccupant: () -> Unit,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        // Sem isso o Compose aplica a largura-padrão de telefone e o modal fica estreito no totem,
        // espremendo o teclado. Desligamos e controlamos a largura explicitamente.
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ProvideDialogLocale {
            Surface(
                color = TotemPalette.Paper,
                shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
                modifier = Modifier.fillMaxWidth(0.85f),
            ) {
                Column(
                    modifier = Modifier.padding(TotemTheme.dimens.space24),
                    verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Título dinâmico: diz o campo E de quem — "Informe o documento do passageiro 1".
                            Text(
                                text = modalTitleDynamic(step, position, isExtra),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = TotemPalette.Ink,
                            )
                            if (step == PassengerModalStep.Documento) {
                                Text(
                                    text = stringRes(R.string.passenger_doc_modal_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TotemPalette.InkMuted,
                                )
                            }
                        }
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringRes(R.string.passenger_keypad_close),
                                tint = TotemPalette.Ink,
                            )
                        }
                    }

                    AnimatedContent(
                        targetState = position to step,
                        transitionSpec = {
                            if (initialState.first != targetState.first) {
                                // Troca de passageiro: slide lateral cheio — o anterior sai, o novo entra.
                                (slideInHorizontally(tween(340)) { it } + fadeIn(tween(240))) togetherWith
                                    (slideOutHorizontally(tween(340)) { -it } + fadeOut(tween(200)))
                            } else {
                                // Passo dentro do mesmo passageiro: slide sutil.
                                (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 5 }) togetherWith
                                    (fadeOut(tween(140)) + slideOutHorizontally(tween(200)) { -it / 5 })
                            }
                        },
                        label = "passengerModalStep",
                    ) { target ->
                        val (pos, s) = target
                        // Form da posição animada: o passageiro que sai aparece preenchido e o que
                        // entra aparece vazio — fica claro que é OUTRO passageiro, não os dados limpos.
                        val f = forms.getOrNull(pos - 1) ?: form
                        Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16)) {
                            if (s == PassengerModalStep.Documento) {
                                DocTypeSelect(selected = f.tipoDocumento, onSelect = onChangeTipoDoc)
                            }
                            val (alpha, label, current) = keypadContentFor(fieldFor(s, 0), f)
                            FieldKeypad(
                                alpha = alpha,
                                label = label,
                                currentValue = current,
                                doneLabel = if (s == PassengerModalStep.Nascimento) {
                                    stringRes(R.string.passenger_modal_finish)
                                } else {
                                    stringRes(R.string.passenger_modal_next)
                                },
                                onDigit = onDigit,
                                onChar = onChar,
                                onBackspace = onBackspace,
                                onClose = onClose,
                                onDone = onNext,
                            )
                            val stepError = errorMessageFor(errorForStep(s, f))
                            if (stepError != null) {
                                Text(
                                    text = stepError,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    if (cpfLookupInFlight) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                                color = TotemPalette.Accent,
                            )
                            Text(
                                text = stringRes(R.string.passenger_doc_searching),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TotemPalette.InkMuted,
                            )
                        }
                    }

                    // Ocupante extra de camarote pode optar por não ir (viajante sozinho).
                    if (isExtra) {
                        OutlinedPillButton(
                            text = stringRes(R.string.passenger_remove_occupant),
                            onClick = onRemoveOccupant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/** Botão com contorno (outline) — usado para ações secundárias visíveis (remover ocupante). */
@Composable
private fun OutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(TotemTheme.dimens.radiusPill),
        color = TotemPalette.Paper,
        contentColor = TotemPalette.Ink,
        border = BorderStroke(1.5.dp, TotemPalette.Ink),
        modifier = modifier
            .heightIn(min = TotemTheme.dimens.touchTarget)
            .clip(RoundedCornerShape(TotemTheme.dimens.radiusPill))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = TotemTheme.dimens.space24,
                vertical = TotemTheme.dimens.space12,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

/** Select (dropdown) do tipo de documento, com todos os tipos. */
@Composable
private fun DocTypeSelect(selected: TipoDocumento, onSelect: (TipoDocumento) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = TotemPalette.Paper,
            border = BorderStroke(
                width = if (expanded) 2.dp else 1.5.dp,
                color = if (expanded) TotemPalette.Accent else TotemPalette.Hairline,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TotemTheme.dimens.touchTarget)
                .clip(RoundedCornerShape(16.dp))
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
                        color = TotemPalette.InkMuted,
                    )
                    Text(
                        text = docLabelFor(selected),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TotemPalette.Ink,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = TotemPalette.InkMuted,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            // Reaplica o idioma dentro do popup (subcomposição própria, igual à Dialog).
            ProvideDialogLocale {
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

// ---------------------------------------------------------------------------
// Teclado compartilhado (inline + modal)
// ---------------------------------------------------------------------------

@Composable
private fun FieldKeypad(
    alpha: Boolean,
    label: String,
    currentValue: String,
    doneLabel: String,
    onDigit: (Char) -> Unit,
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    val closeDesc = stringRes(R.string.passenger_keypad_close)
    val deleteDesc = stringRes(R.string.passenger_keypad_delete)
    if (alpha) {
        TotemAlphaKeypad(
            label = label,
            currentValue = currentValue,
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
    } else {
        TotemNumericKeypad(
            label = label,
            currentValue = currentValue,
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

/** (alpha?, label, valorAtual) para renderizar o teclado de um campo. */
@Composable
private fun keypadContentFor(field: KeypadField, form: PassageiroForm): Triple<Boolean, String, String> =
    when (field) {
        is KeypadField.Documento -> Triple(false, docLabelFor(form.tipoDocumento), form.documentoDisplay)
        is KeypadField.Nome -> Triple(true, stringRes(R.string.passenger_field_name), form.nome)
        is KeypadField.Telefone -> Triple(false, stringRes(R.string.passenger_field_phone), form.telefoneDisplay)
        is KeypadField.Nascimento -> Triple(false, stringRes(R.string.passenger_field_birthdate), form.nascimentoDisplay)
    }

private fun fieldFor(step: PassengerModalStep, index: Int): KeypadField = when (step) {
    PassengerModalStep.Documento -> KeypadField.Documento(index)
    PassengerModalStep.Nome -> KeypadField.Nome(index)
    PassengerModalStep.Telefone -> KeypadField.Telefone(index)
    PassengerModalStep.Nascimento -> KeypadField.Nascimento(index)
}

private fun errorForStep(step: PassengerModalStep, form: PassageiroForm): String? = when (step) {
    PassengerModalStep.Documento -> form.errors.documento
    PassengerModalStep.Nome -> form.errors.nome
    PassengerModalStep.Telefone -> form.errors.telefone
    PassengerModalStep.Nascimento -> form.errors.nascimento
}

/** Título dinâmico do modal: "Informe o documento do passageiro 1" / "… do ocupante 2". */
@Composable
private fun modalTitleDynamic(step: PassengerModalStep, position: Int, isExtra: Boolean): String {
    val person = if (isExtra) stringResFmt(R.string.passenger_person_extra, position)
    else stringResFmt(R.string.passenger_person, position)
    val template = when (step) {
        PassengerModalStep.Documento -> R.string.passenger_modal_doc_of
        PassengerModalStep.Nome -> R.string.passenger_modal_name_of
        PassengerModalStep.Telefone -> R.string.passenger_modal_phone_of
        PassengerModalStep.Nascimento -> R.string.passenger_modal_birth_of
    }
    return stringResFmt(template, person)
}

// ---------------------------------------------------------------------------
// Auxiliares
// ---------------------------------------------------------------------------

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
    TipoDocumento.RG -> stringRes(R.string.passenger_field_rg)
    TipoDocumento.TituloEleitor -> stringRes(R.string.passenger_field_titulo)
    TipoDocumento.Passaporte -> stringRes(R.string.passenger_field_passaporte)
    TipoDocumento.CNH -> stringRes(R.string.passenger_field_cnh)
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
