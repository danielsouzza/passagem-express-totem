package com.example.passagenexpress.feature.payment

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Pix
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.example.passagenexpress.feature.passenger.ui.NumericKeypad
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PaymentScreen(
    onPaid: (PaymentApproved) -> Unit,
    onBack: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val approved = state.paymentApproved
    // Quando o pagamento é aprovado, a tela fica 5s mostrando "Pagamento aprovado!" e
    // depois auto-volta pro Idle via `onPaid`. O usuário não tem botão de Voltar — é
    // uma transição automática pra reset do totem entre clientes.
    var autoReturnSeconds by remember(approved?.pedidoId) {
        mutableIntStateOf(AUTO_RETURN_SECONDS)
    }
    LaunchedEffect(approved?.pedidoId) {
        if (approved == null) return@LaunchedEffect
        autoReturnSeconds = AUTO_RETURN_SECONDS
        while (autoReturnSeconds > 0) {
            kotlinx.coroutines.delay(1_000)
            autoReturnSeconds -= 1
        }
        onPaid(approved)
    }
    if (state.orderStage is OrderStage.Ready && isWaitingActive(state)) {
        PaymentWaitingScreen(
            state = state,
            autoReturnSeconds = if (approved != null) autoReturnSeconds else null,
            onCancel = viewModel::onCancelarPagamento,
            onRetryPix = viewModel::onGerarPix,
            onRetryCard = viewModel::onPagarComCartao,
            onReturnNow = { approved?.let(onPaid) },
        )
        return
    }
    PaymentContent(
        state = state,
        onRetryCreateOrder = viewModel::onRetryCreateOrder,
        onSelectMethod = viewModel::onSelectMethod,
        onChangeContactNome = viewModel::onChangeContactNome,
        onOpenKeypad = viewModel::onOpenContactKeypad,
        onCloseKeypad = viewModel::onCloseContactKeypad,
        onKeypadDigit = viewModel::onContactKeypadDigit,
        onKeypadBackspace = viewModel::onContactKeypadBackspace,
        onGerarPix = viewModel::onGerarPix,
        onPagarComCartao = viewModel::onPagarComCartao,
        onBack = onBack,
    )
}

/**
 * `true` quando o usuário já disparou um fluxo de pagamento (PIX ou cartão).
 * Nesse caso a tela cede o lugar pro `PaymentWaitingScreen`. Idle volta pro seletor.
 */
private fun isWaitingActive(state: PaymentUiState): Boolean = when (state.selectedMethod) {
    PaymentMethod.Pix -> state.pixStage !is PixStage.Idle
    PaymentMethod.Card -> state.card.stage !is CardStage.Idle
}

private const val AUTO_RETURN_SECONDS = 5

@Composable
private fun PaymentContent(
    state: PaymentUiState,
    onRetryCreateOrder: () -> Unit,
    onSelectMethod: (PaymentMethod) -> Unit,
    onChangeContactNome: (String) -> Unit,
    onOpenKeypad: () -> Unit,
    onCloseKeypad: () -> Unit,
    onKeypadDigit: (Char) -> Unit,
    onKeypadBackspace: () -> Unit,
    onGerarPix: () -> Unit,
    onPagarComCartao: () -> Unit,
    onBack: () -> Unit,
) {
    val trecho = state.trecho
    val subtitle = if (trecho != null) {
        stringResFmt(
            R.string.payment_subtitle_route,
            trecho.municipioOrigem.nome,
            trecho.municipioDestino.nome,
        )
    } else null

    TotemScreenScaffold(
        step = 6,
        title = stringRes(R.string.payment_title),
        subtitle = subtitle,
        footer = {
            TotemSecondaryButton(text = stringRes(R.string.payment_back), onClick = onBack)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                if (trecho != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TOTAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.4.sp,
                            ),
                            color = TotemPalette.InkMuted,
                        )
                        Text(
                            text = formatMoney(state.total),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                            ),
                            color = TotemPalette.Ink,
                        )
                    }
                }
            }
        },
    ) {
        when (val stage = state.orderStage) {
            is OrderStage.Creating -> TotemLoading(label = stringRes(R.string.payment_creating_order))
            is OrderStage.Failed -> TotemErrorState(
                title = stringRes(R.string.payment_error_create_order),
                message = stage.message,
                actionLabel = stringRes(R.string.payment_retry),
                onAction = onRetryCreateOrder,
            )
            is OrderStage.Ready -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space20),
            ) {
                ResumoCard(state = state)
                MethodSelector(
                    selected = state.selectedMethod,
                    onSelect = onSelectMethod,
                )
                when (state.selectedMethod) {
                    PaymentMethod.Pix -> {
                        PixSection(
                            state = state,
                            onChangeContactNome = onChangeContactNome,
                            onOpenKeypad = onOpenKeypad,
                            onGerarPix = onGerarPix,
                        )
                        if (state.contactForm.keypadOpen) {
                            NumericKeypad(
                                label = stringRes(R.string.payment_contact_cpf),
                                currentValue = state.contactForm.cpfDisplay,
                                onDigit = onKeypadDigit,
                                onBackspace = onKeypadBackspace,
                                onClose = onCloseKeypad,
                                onDone = onCloseKeypad,
                                doneLabel = stringRes(R.string.payment_keypad_done),
                                closeContentDesc = stringRes(R.string.payment_keypad_close),
                                deleteContentDesc = stringRes(R.string.payment_keypad_delete),
                            )
                        }
                    }
                    PaymentMethod.Card -> CardSection(onPagarComCartao = onPagarComCartao)
                }
                Spacer(Modifier.height(TotemTheme.dimens.space24))
            }
        }
    }
}

@Composable
private fun MethodSelector(
    selected: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12)) {
        Text(
            text = stringRes(R.string.payment_method_section).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
            color = TotemPalette.InkMuted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12)) {
            MethodChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Pix,
                title = stringRes(R.string.payment_method_pix),
                hint = stringRes(R.string.payment_method_pix_hint),
                selected = selected == PaymentMethod.Pix,
                onClick = { onSelect(PaymentMethod.Pix) },
            )
            MethodChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CreditCard,
                title = stringRes(R.string.payment_method_card),
                hint = stringRes(R.string.payment_method_card_hint),
                selected = selected == PaymentMethod.Card,
                onClick = { onSelect(PaymentMethod.Card) },
            )
        }
    }
}

@Composable
private fun MethodChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    hint: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) TotemPalette.Accent else TotemPalette.Hairline
    val bgColor = if (selected) TotemPalette.AccentTint else TotemPalette.Paper
    Surface(
        modifier = modifier
            .heightIn(min = TotemTheme.dimens.touchTarget)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(TotemTheme.dimens.radiusXl),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.5.dp,
            borderColor,
        ),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = TotemTheme.dimens.space20,
                vertical = TotemTheme.dimens.space16,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) TotemPalette.Accent else TotemPalette.Ink,
                modifier = Modifier.size(28.dp),
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TotemPalette.Ink,
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = TotemPalette.InkMuted,
                )
            }
        }
    }
}

@Composable
private fun ResumoCard(state: PaymentUiState) {
    val trecho = state.trecho ?: return
    Surface(
        shape = RoundedCornerShape(TotemTheme.dimens.radiusXl),
        color = TotemPalette.Paper,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, TotemPalette.Hairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = TotemTheme.dimens.space28,
                vertical = TotemTheme.dimens.space24,
            ),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            Text(
                text = stringRes(R.string.payment_summary_title).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                ),
                color = TotemPalette.InkMuted,
            )
            ResumoRow(
                label = stringRes(R.string.payment_summary_route),
                value = "${trecho.municipioOrigem.nome} → ${trecho.municipioDestino.nome}",
            )
            ResumoRow(
                label = stringRes(R.string.payment_summary_date),
                value = trecho.dataEmbarque.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                    " · " + trecho.horario.format(DateTimeFormatter.ofPattern("HH:mm")),
            )
            ResumoRow(
                label = stringRes(R.string.payment_summary_passengers),
                value = state.totalPassageiros.toString(),
            )
            ResumoRow(
                label = stringRes(R.string.payment_summary_total_passages),
                value = formatMoney(state.totalPassagens),
            )
            ResumoRow(
                label = stringRes(R.string.payment_summary_total_fees),
                value = formatMoney(state.totalTaxas),
            )
        }
    }
}

@Composable
private fun ResumoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TotemPalette.InkMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TotemPalette.Ink,
        )
    }
}

@Composable
private fun PixSection(
    state: PaymentUiState,
    onChangeContactNome: (String) -> Unit,
    onOpenKeypad: () -> Unit,
    onGerarPix: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(TotemTheme.dimens.radiusXl),
        color = TotemPalette.Paper,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, TotemPalette.Hairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(TotemTheme.dimens.space28),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        ) {
            PixHeader()
            ContactFormBlock(
                form = state.contactForm,
                onChangeNome = onChangeContactNome,
                onOpenKeypad = onOpenKeypad,
            )
            TotemPrimaryButton(
                text = stringRes(R.string.payment_pix_generate),
                onClick = onGerarPix,
                accent = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CardSection(onPagarComCartao: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(TotemTheme.dimens.radiusXl),
        color = TotemPalette.Paper,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, TotemPalette.Hairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(TotemTheme.dimens.space28),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        ) {
            CardHeader()
            TotemPrimaryButton(
                text = stringRes(R.string.payment_card_pay),
                onClick = onPagarComCartao,
                accent = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CardHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
    ) {
        Surface(
            color = TotemPalette.AccentTint,
            shape = RoundedCornerShape(TotemTheme.dimens.radiusSm),
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.CreditCard,
                    contentDescription = null,
                    tint = TotemPalette.Accent,
                )
            }
        }
        Column {
            Text(
                text = stringRes(R.string.payment_method_card),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TotemPalette.Ink,
            )
            Text(
                text = stringRes(R.string.payment_method_card_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = TotemPalette.InkMuted,
            )
        }
    }
}

@Composable
private fun ContactFormBlock(
    form: ContactForm,
    onChangeNome: (String) -> Unit,
    onOpenKeypad: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12)) {
        Text(
            text = stringRes(R.string.payment_contact_section).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
            color = TotemPalette.InkMuted,
        )
        ContactNameField(
            value = form.nome,
            error = nameErrorFor(form.nomeError),
            onChange = onChangeNome,
        )
        ContactCpfField(
            value = form.cpfDisplay,
            error = cpfErrorFor(form.cpfError),
            onClick = onOpenKeypad,
        )
    }
}

@Composable
private fun ContactNameField(value: String, error: String?, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space4)) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(stringRes(R.string.payment_contact_name)) },
            singleLine = true,
            shape = RoundedCornerShape(TotemTheme.dimens.radius),
            isError = error != null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TotemPalette.Ink,
                unfocusedBorderColor = TotemPalette.Hairline,
                focusedLabelColor = TotemPalette.Accent,
                unfocusedLabelColor = TotemPalette.InkMuted,
            ),
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = TotemPalette.Error,
            )
        }
    }
}

@Composable
private fun ContactCpfField(value: String, error: String?, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space4)) {
        Text(
            text = stringRes(R.string.payment_contact_cpf),
            style = MaterialTheme.typography.labelLarge,
            color = TotemPalette.InkMuted,
        )
        Surface(
            shape = RoundedCornerShape(TotemTheme.dimens.radius),
            color = TotemPalette.Paper,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, TotemPalette.Hairline),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TotemTheme.dimens.touchTarget)
                .clickable(onClick = onClick),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = TotemTheme.dimens.space20,
                    vertical = TotemTheme.dimens.space14,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value.ifEmpty { "—" },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (value.isEmpty()) TotemPalette.InkMuted else TotemPalette.Ink,
                )
            }
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = TotemPalette.Error,
            )
        }
    }
}

@Composable
private fun nameErrorFor(code: String?): String? = when (code) {
    PaymentViewModel.ERROR_NAME_REQUIRED -> stringRes(R.string.payment_contact_name_error)
    null -> null
    else -> null
}

@Composable
private fun cpfErrorFor(code: String?): String? = when (code) {
    PaymentViewModel.ERROR_CPF_INVALID -> stringRes(R.string.payment_contact_cpf_error)
    null -> null
    else -> null
}

@Composable
private fun PixHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
    ) {
        Surface(
            color = TotemPalette.AccentTint,
            shape = RoundedCornerShape(TotemTheme.dimens.radiusSm),
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Pix,
                    contentDescription = null,
                    tint = TotemPalette.Accent,
                )
            }
        }
        Column {
            Text(
                text = stringRes(R.string.payment_method_pix),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TotemPalette.Ink,
            )
            Text(
                text = stringRes(R.string.payment_method_pix_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = TotemPalette.InkMuted,
            )
        }
    }
}

private fun formatMoney(value: Double): String = "R$ ${formatMoneyValue(value)}"

private fun formatMoneyValue(value: Double): String =
    String.format(Locale.forLanguageTag("pt-BR"), "%,.2f", value)

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)

@Composable
private fun stringResFmt(id: Int, vararg args: Any): String =
    androidx.compose.ui.res.stringResource(id, *args)
