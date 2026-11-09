package com.example.passagenexpress.feature.bus.passengers

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.example.passagenexpress.core.domain.model.onibus.TipoDocOnibus
import com.example.passagenexpress.feature.bus.R
import com.example.passagenexpress.feature.bus.search.TOTAL_STEPS

/**
 * Passageiros do ônibus — mesma tela do fluxo de barco (`PassengerScreen`): cards por poltrona,
 * o ativo expandido com o formulário inline, e o modal guiado para preencher campo a campo.
 */
@Composable
fun BusPassengersScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: BusPassengersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.concluido) {
        if (state.concluido) {
            viewModel.onNavigated()
            onDone()
        }
    }

    val ativo = state.form
    val inline = state.campo
    TotemScreenScaffold(
        step = 4,
        totalSteps = TOTAL_STEPS,
        title = stringResource(R.string.bus_passenger_screen_title),
        subtitle = stringResource(R.string.bus_passenger_screen_subtitle),
        footer = {
            TotemSecondaryButton(text = stringResource(R.string.bus_back), onClick = onBack)
            Spacer(Modifier.width(TotemTheme.dimens.space12))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TotemPrimaryButton(
                    text = stringResource(if (state.temProximo) R.string.bus_next_passenger else R.string.bus_passenger_advance),
                    onClick = viewModel::onAdvance,
                )
            }
        },
        // Teclado inline pra editar um campo do passageiro ativo (quando o modal está fechado).
        stickyBottom = if (inline != null && ativo != null && state.modalStep == null) ({
            FieldKeypad(
                campo = inline,
                form = ativo,
                doneLabel = stringResource(R.string.bus_keypad_done),
                onChar = viewModel::onChar,
                onBackspace = viewModel::onBackspace,
                onClose = viewModel::onCloseKeypad,
                onDone = viewModel::onKeypadDone,
            )
        }) else null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space24),
        ) {
            state.forms.forEachIndexed { index, form ->
                Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12)) {
                    SeatHeader(assento = form.assentoLabel)
                    SeatCard(
                        form = form,
                        titular = index == 0,
                        filled = state.isFilled(index),
                        active = index == state.ativo,
                        contato = state.ehContato(index),
                        buscando = state.buscandoCadastro,
                        campoInline = state.campo,
                        onFill = { viewModel.onEditPassenger(index) },
                        onOpenKeypad = viewModel::onOpenKeypad,
                        onOpenDocModal = viewModel::onOpenDocModal,
                    )
                }
            }
            Spacer(Modifier.height(TotemTheme.dimens.space24))
        }
    }

    val step = state.modalStep
    if (step != null && ativo != null) {
        DocumentModal(
            step = step,
            form = ativo,
            forms = state.forms,
            position = state.ativo + 1,
            buscando = state.buscandoCadastro,
            onChangeTipoDoc = viewModel::onTipoDoc,
            onChar = viewModel::onChar,
            onBackspace = viewModel::onBackspace,
            onNext = viewModel::onModalNext,
            onClose = viewModel::onCloseModal,
            isLast = step == CampoPassageiro.Email || (step == CampoPassageiro.Nascimento && !state.ehContato(state.ativo)),
        )
    }
}

private val SeatFilledBg = Color(0xFFF3FAF4)
private val SeatFilledBorder = Color(0xFFCBE3D2)

/** Cabeçalho da poltrona (mesmo visual do cabeçalho de cômodo do barco). */
@Composable
private fun SeatHeader(assento: String) {
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
                text = assento.take(2),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TotemPalette.Ink2,
            )
        }
        Text(
            text = stringResource(R.string.bus_seat_number, assento),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TotemPalette.Ink,
        )
    }
}

@Composable
private fun SeatCard(
    form: PassageiroOnibusForm,
    titular: Boolean,
    filled: Boolean,
    active: Boolean,
    contato: Boolean,
    buscando: Boolean,
    campoInline: CampoPassageiro?,
    onFill: () -> Unit,
    onOpenKeypad: (CampoPassageiro) -> Unit,
    onOpenDocModal: () -> Unit,
) {
    val border = when {
        active -> TotemPalette.Accent
        filled -> SeatFilledBorder
        titular -> TotemPalette.Primary100
        else -> TotemPalette.Hairline
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (filled && !active) SeatFilledBg else TotemPalette.Paper,
        border = BorderStroke(if (active) 2.dp else 1.5.dp, border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(TotemTheme.dimens.space16),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
            ) {
                Text(
                    text = stringResource(if (titular) R.string.bus_role_titular else R.string.bus_role_passenger),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TotemPalette.Ink,
                )
                Spacer(Modifier.weight(1f))
                if (filled && !active) {
                    Icon(Icons.Filled.CheckCircle, null, tint = TotemPalette.Success, modifier = Modifier.size(22.dp))
                }
            }
            when {
                active -> FormFields(form, contato, buscando, campoInline, onOpenKeypad, onOpenDocModal)
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
                            text = "${stringResource(docLabel(form.tipoDoc))} ${mascaraDocumento(form.documento, form.tipoDoc)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TotemPalette.InkMuted,
                        )
                    }
                    Text(
                        text = stringResource(R.string.bus_edit),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TotemPalette.Accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(TotemTheme.dimens.radiusPill))
                            .clickable(onClick = onFill)
                            .padding(horizontal = TotemTheme.dimens.space16, vertical = TotemTheme.dimens.space8),
                    )
                }
                else -> TotemPrimaryButton(
                    text = stringResource(R.string.bus_fill_data),
                    onClick = onFill,
                    trailingArrow = false,
                    leading = { Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp)) },
                )
            }
        }
    }
}

/** Formulário inline do passageiro ativo: documento reabre o modal; o resto usa o teclado. */
@Composable
private fun FormFields(
    form: PassageiroOnibusForm,
    contato: Boolean,
    buscando: Boolean,
    campoInline: CampoPassageiro?,
    onOpenKeypad: (CampoPassageiro) -> Unit,
    onOpenDocModal: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
    ) {
        TotemInputField(
            label = stringResource(docLabel(form.tipoDoc)),
            value = mascaraDocumento(form.documento, form.tipoDoc),
            error = form.erros[CampoPassageiro.Documento]?.let { stringResource(erroLabel(it)) },
            trailing = if (buscando) ({ LoadingDot() }) else null,
            onClick = onOpenDocModal,
        )
        TotemInputField(
            label = stringResource(R.string.bus_passenger_name),
            value = form.nome,
            error = form.erros[CampoPassageiro.Nome]?.let { stringResource(erroLabel(it)) },
            focused = campoInline == CampoPassageiro.Nome,
            onClick = { onOpenKeypad(CampoPassageiro.Nome) },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            TotemInputField(
                label = stringResource(R.string.bus_passenger_phone_required),
                value = mascaraTelefone(form.telefone),
                error = form.erros[CampoPassageiro.Telefone]?.let { stringResource(erroLabel(it)) },
                focused = campoInline == CampoPassageiro.Telefone,
                onClick = { onOpenKeypad(CampoPassageiro.Telefone) },
                modifier = Modifier.weight(1f),
            )
            TotemInputField(
                label = stringResource(R.string.bus_passenger_birth),
                placeholder = stringResource(R.string.bus_birth_placeholder),
                value = mascaraData(form.nascimento),
                error = form.erros[CampoPassageiro.Nascimento]?.let { stringResource(erroLabel(it)) },
                focused = campoInline == CampoPassageiro.Nascimento,
                onClick = { onOpenKeypad(CampoPassageiro.Nascimento) },
                modifier = Modifier.weight(1f),
            )
        }
        if (contato) {
            TotemInputField(
                label = stringResource(R.string.bus_passenger_email),
                value = form.email,
                error = form.erros[CampoPassageiro.Email]?.let { stringResource(erroLabel(it)) },
                focused = campoInline == CampoPassageiro.Email,
                onClick = { onOpenKeypad(CampoPassageiro.Email) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Modal guiado (documento → nome → telefone → nascimento → e-mail do contato)
// ---------------------------------------------------------------------------

@Composable
private fun DocumentModal(
    step: CampoPassageiro,
    form: PassageiroOnibusForm,
    forms: List<PassageiroOnibusForm>,
    position: Int,
    buscando: Boolean,
    isLast: Boolean,
    onChangeTipoDoc: (TipoDocOnibus) -> Unit,
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
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
                            Text(
                                text = modalTitle(step, position),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = TotemPalette.Ink,
                            )
                            val sub = when (step) {
                                CampoPassageiro.Documento -> R.string.bus_doc_modal_subtitle
                                CampoPassageiro.Email -> R.string.bus_passenger_contact_hint
                                else -> null
                            }
                            if (sub != null) {
                                Text(
                                    text = stringResource(sub),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TotemPalette.InkMuted,
                                )
                            }
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.bus_keypad_close), tint = TotemPalette.Ink)
                        }
                    }

                    AnimatedContent(
                        targetState = position to step,
                        transitionSpec = {
                            if (initialState.first != targetState.first) {
                                (slideInHorizontally(tween(340)) { it } + fadeIn(tween(240))) togetherWith
                                    (slideOutHorizontally(tween(340)) { -it } + fadeOut(tween(200)))
                            } else {
                                (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 5 }) togetherWith
                                    (fadeOut(tween(140)) + slideOutHorizontally(tween(200)) { -it / 5 })
                            }
                        },
                        label = "busPassengerModalStep",
                    ) { (pos, s) ->
                        val f = forms.getOrNull(pos - 1) ?: form
                        Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16)) {
                            if (s == CampoPassageiro.Documento) {
                                DocTypeSelect(selected = f.tipoDoc, onSelect = onChangeTipoDoc)
                            }
                            FieldKeypad(
                                campo = s,
                                form = f,
                                doneLabel = stringResource(if (isLast) R.string.bus_modal_finish else R.string.bus_continue),
                                onChar = onChar,
                                onBackspace = onBackspace,
                                onClose = onClose,
                                onDone = onNext,
                            )
                            f.erros[s]?.let { erro ->
                                Text(
                                    text = stringResource(erroLabel(erro)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    if (buscando) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = TotemPalette.Accent)
                            Text(
                                text = stringResource(R.string.bus_doc_searching),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TotemPalette.InkMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Select (dropdown) do tipo de documento — mesmo componente do barco, com os tipos do ônibus. */
@Composable
private fun DocTypeSelect(selected: TipoDocOnibus, onSelect: (TipoDocOnibus) -> Unit) {
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
                modifier = Modifier.padding(horizontal = TotemTheme.dimens.space16, vertical = TotemTheme.dimens.space12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.bus_doc_type),
                        style = MaterialTheme.typography.labelSmall,
                        color = TotemPalette.InkMuted,
                    )
                    Text(
                        text = stringResource(docLabel(selected)),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TotemPalette.Ink,
                    )
                }
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = TotemPalette.InkMuted)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ProvideDialogLocale {
                TipoDocOnibus.entries.forEach { tipo ->
                    DropdownMenuItem(
                        text = { Text(stringResource(docLabel(tipo))) },
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

/** Teclado do campo: alfanumérico pra nome/e-mail/passaporte; numérico pro resto. */
@Composable
private fun FieldKeypad(
    campo: CampoPassageiro,
    form: PassageiroOnibusForm,
    doneLabel: String,
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    val label = when (campo) {
        CampoPassageiro.Documento -> stringResource(docLabel(form.tipoDoc))
        CampoPassageiro.Nome -> stringResource(R.string.bus_passenger_name)
        CampoPassageiro.Telefone -> stringResource(R.string.bus_passenger_phone_required)
        CampoPassageiro.Nascimento -> stringResource(R.string.bus_passenger_birth)
        CampoPassageiro.Email -> stringResource(R.string.bus_passenger_email)
    }
    val valor = when (campo) {
        CampoPassageiro.Documento -> mascaraDocumento(form.documento, form.tipoDoc)
        CampoPassageiro.Nome -> form.nome
        CampoPassageiro.Telefone -> mascaraTelefone(form.telefone)
        CampoPassageiro.Nascimento -> mascaraData(form.nascimento)
        CampoPassageiro.Email -> form.email
    }
    val alpha = when (campo) {
        CampoPassageiro.Nome, CampoPassageiro.Email -> true
        CampoPassageiro.Documento -> form.tipoDoc == TipoDocOnibus.Passaporte
        else -> false
    }
    val closeDesc = stringResource(R.string.bus_keypad_close)
    val deleteDesc = stringResource(R.string.bus_keypad_delete)
    if (alpha) {
        TotemAlphaKeypad(
            label = label,
            currentValue = valor,
            onChar = onChar,
            onBackspace = onBackspace,
            onClose = onClose,
            onDone = onDone,
            doneLabel = doneLabel,
            closeContentDesc = closeDesc,
            deleteContentDesc = deleteDesc,
            spaceContentDesc = stringResource(R.string.bus_keypad_space),
            shiftContentDesc = stringResource(R.string.bus_keypad_shift),
        )
    } else {
        TotemNumericKeypad(
            label = label,
            currentValue = valor,
            onDigit = onChar,
            onBackspace = onBackspace,
            onClose = onClose,
            onDone = onDone,
            doneLabel = doneLabel,
            closeContentDesc = closeDesc,
            deleteContentDesc = deleteDesc,
        )
    }
}

@Composable
private fun modalTitle(step: CampoPassageiro, position: Int): String {
    val person = stringResource(R.string.bus_person, position)
    val template = when (step) {
        CampoPassageiro.Documento -> R.string.bus_modal_doc_of
        CampoPassageiro.Nome -> R.string.bus_modal_name_of
        CampoPassageiro.Telefone -> R.string.bus_modal_phone_of
        CampoPassageiro.Nascimento -> R.string.bus_modal_birth_of
        CampoPassageiro.Email -> R.string.bus_modal_email_of
    }
    return stringResource(template, person)
}

@Composable
private fun LoadingDot() {
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
    }
}

private fun docLabel(tipo: TipoDocOnibus): Int = when (tipo) {
    TipoDocOnibus.Cpf -> R.string.bus_doc_cpf
    TipoDocOnibus.Rg -> R.string.bus_doc_rg
    TipoDocOnibus.Passaporte -> R.string.bus_doc_passport
    TipoDocOnibus.Cnh -> R.string.bus_doc_cnh
}

private fun erroLabel(erro: ErroCampo): Int = when (erro) {
    ErroCampo.Obrigatorio -> R.string.bus_error_required
    ErroCampo.CpfInvalido -> R.string.bus_error_cpf
    ErroCampo.DocumentoInvalido -> R.string.bus_error_document
    ErroCampo.NomeInvalido -> R.string.bus_error_name
    ErroCampo.DataInvalida -> R.string.bus_error_date
    ErroCampo.TelefoneInvalido -> R.string.bus_error_phone
    ErroCampo.EmailInvalido -> R.string.bus_error_email
}

internal fun mascaraDocumento(doc: String, tipo: TipoDocOnibus): String {
    if (tipo != TipoDocOnibus.Cpf) return doc
    val sb = StringBuilder()
    doc.forEachIndexed { i, c ->
        when (i) {
            3, 6 -> sb.append('.')
            9 -> sb.append('-')
        }
        sb.append(c)
    }
    return sb.toString()
}

internal fun mascaraData(digits: String): String {
    val sb = StringBuilder()
    digits.forEachIndexed { i, c ->
        if (i == 2 || i == 4) sb.append('/')
        sb.append(c)
    }
    return sb.toString()
}

internal fun mascaraTelefone(digits: String): String {
    if (digits.isEmpty()) return ""
    val sb = StringBuilder()
    digits.forEachIndexed { i, c ->
        when (i) {
            0 -> sb.append('(')
            2 -> sb.append(") ")
            7 -> if (digits.length == 11) sb.append('-')
            6 -> if (digits.length <= 10) sb.append('-')
        }
        sb.append(c)
    }
    return sb.toString()
}
