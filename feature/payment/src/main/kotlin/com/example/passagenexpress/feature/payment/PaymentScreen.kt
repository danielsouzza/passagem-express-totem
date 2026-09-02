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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Pix
import androidx.compose.material.icons.filled.WarningAmber
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
    // Pagamento aprovado: mostra "Compra aprovada! Aguarde seu bilhete" por um instante e
    // segue automaticamente pra etapa de impressão (sem countdown e sem botão aqui — o
    // "Continuar comprando" só aparece depois que o bilhete é impresso).
    LaunchedEffect(approved?.pedidoId) {
        if (approved == null) return@LaunchedEffect
        kotlinx.coroutines.delay(SUCCESS_HOLD_MS)
        onPaid(approved)
    }
    if (state.orderStage is OrderStage.Ready && isWaitingActive(state)) {
        PaymentWaitingScreen(
            state = state,
            onCancel = viewModel::onCancelarPagamento,
            onRetryPix = viewModel::onGerarPix,
            onRetryCard = viewModel::onPagarComCartao,
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
        onSelectCardType = viewModel::onSelectCardType,
        onSelectInstallments = viewModel::onSelectInstallments,
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

/** Tempo que a tela de "Compra aprovada" fica visível antes de ir pra impressão. */
private const val SUCCESS_HOLD_MS = 3_000L

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
    onSelectCardType: (CardType) -> Unit,
    onSelectInstallments: (Int) -> Unit,
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
        step = 4,
        totalSteps = 4,
        title = stringRes(R.string.payment_title),
        subtitle = subtitle,
        footer = {
            TotemSecondaryButton(text = stringRes(R.string.payment_back), onClick = onBack)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                if (trecho != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringRes(R.string.payment_total_label),
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
                    PaymentMethod.Card -> CardSection(
                        options = state.card.options,
                        total = state.total,
                        onSelectCardType = onSelectCardType,
                        onSelectInstallments = onSelectInstallments,
                        onPagarComCartao = onPagarComCartao,
                    )
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
private fun CardSection(
    options: CardOptions,
    total: Double,
    onSelectCardType: (CardType) -> Unit,
    onSelectInstallments: (Int) -> Unit,
    onPagarComCartao: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(TotemTheme.dimens.radiusXl),
        color = TotemPalette.Paper,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, TotemPalette.Hairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(TotemTheme.dimens.space28),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space20),
        ) {
            CardHeader()
            TerminalChargeWarning()
            CardTypeSelector(selected = options.type, onSelect = onSelectCardType)
            if (options.type == CardType.Credit) {
                InstallmentsSelector(
                    selected = options.installments,
                    total = total,
                    onSelect = onSelectInstallments,
                )
            }
            TotemPrimaryButton(
                text = stringRes(R.string.payment_card_pay),
                onClick = onPagarComCartao,
                accent = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Aviso pré-pagamento: a maquininha (terminal externo) pode ficar com uma cobrança pendurada
 * de uma tentativa anterior. Não é o ideal, mas pedimos pro operador/cliente conferir e cancelar
 * no próprio terminal antes de iniciar uma nova cobrança.
 */
/**
 * Aviso (não erro): cor de warning âmbar (`WarningLight`/`WarningDark`), não o vermelho de erro.
 * Título em destaque + corpo para o operador/cliente realmente ler antes de iniciar a cobrança.
 */
@Composable
private fun TerminalChargeWarning() {
    Surface(
        shape = RoundedCornerShape(TotemTheme.dimens.radiusSm),
        color = TotemPalette.WarningLight,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(TotemTheme.dimens.space16),
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = TotemPalette.Warning,
                modifier = Modifier.size(28.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space4),
            ) {
                Text(
                    text = stringRes(R.string.payment_card_terminal_warning_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TotemPalette.WarningDark,
                )
                Text(
                    text = stringRes(R.string.payment_card_terminal_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TotemPalette.WarningDark,
                )
            }
        }
    }
}

@Composable
private fun CardTypeSelector(selected: CardType, onSelect: (CardType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12)) {
        Text(
            text = stringRes(R.string.payment_card_type_section).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
            color = TotemPalette.InkMuted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12)) {
            OptionPill(
                modifier = Modifier.weight(1f),
                label = stringRes(R.string.payment_card_type_credit),
                selected = selected == CardType.Credit,
                onClick = { onSelect(CardType.Credit) },
            )
            OptionPill(
                modifier = Modifier.weight(1f),
                label = stringRes(R.string.payment_card_type_debit),
                selected = selected == CardType.Debit,
                onClick = { onSelect(CardType.Debit) },
            )
        }
    }
}

@Composable
private fun InstallmentsSelector(
    selected: Int,
    total: Double,
    onSelect: (Int) -> Unit,
) {
    val max = PaymentViewModel.MAX_INSTALLMENTS
    val canPrev = selected > 1
    val canNext = selected < max
    val perInstallment = total / selected

    Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12)) {
        Text(
            text = stringRes(R.string.payment_card_installments_section).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
            color = TotemPalette.InkMuted,
        )
        // Stepper: [←] valor central [→]. Decrementa/incrementa entre 1..MAX, sem ciclar
        // (não faz sentido ir de 1x pra 12x num toque acidental).
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            StepperArrow(
                direction = ArrowDirection.Left,
                enabled = canPrev,
                onClick = { if (canPrev) onSelect(selected - 1) },
            )
            InstallmentsValue(
                installments = selected,
                perInstallment = perInstallment,
                total = total,
                modifier = Modifier.weight(1f),
            )
            StepperArrow(
                direction = ArrowDirection.Right,
                enabled = canNext,
                onClick = { if (canNext) onSelect(selected + 1) },
            )
        }
    }
}

private enum class ArrowDirection { Left, Right }

@Composable
private fun StepperArrow(direction: ArrowDirection, enabled: Boolean, onClick: () -> Unit) {
    // Visual neutro: sem cor de "selecionado". Apenas borda fina e ícone em Ink quando
    // pode ser tocado, opacificado quando no limite (1× ou 12×).
    val tint = if (enabled) TotemPalette.Ink else TotemPalette.InkSoft
    Surface(
        modifier = Modifier
            .size(TotemTheme.dimens.touchTarget)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(TotemTheme.dimens.radius),
        color = TotemPalette.Paper,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, TotemPalette.Hairline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = when (direction) {
                    ArrowDirection.Left -> Icons.AutoMirrored.Filled.ArrowBack
                    ArrowDirection.Right -> Icons.AutoMirrored.Filled.ArrowForward
                },
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun InstallmentsValue(
    installments: Int,
    perInstallment: Double,
    total: Double,
    modifier: Modifier = Modifier,
) {
    // Valor central sem container — só texto centralizado. Quantidade de parcelas em
    // titleLarge (vs headlineMedium antes) e a moeda como sub-info; mais clean.
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space4),
    ) {
        Text(
            text = if (installments == 1) {
                stringRes(R.string.payment_card_installments_single)
            } else {
                stringResFmt(R.string.payment_card_installments_chip, installments)
            },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = TotemPalette.Ink,
        )
        Text(
            text = if (installments == 1) {
                formatMoney(total)
            } else {
                stringResFmt(
                    R.string.payment_card_installments_preview,
                    installments,
                    formatMoney(perInstallment),
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TotemPalette.InkMuted,
        )
    }
}

@Composable
private fun OptionPill(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) TotemPalette.Accent else TotemPalette.Hairline
    val bgColor = if (selected) TotemPalette.AccentTint else TotemPalette.Paper
    val textColor = if (selected) TotemPalette.Accent else TotemPalette.Ink
    Surface(
        modifier = modifier
            .heightIn(min = TotemTheme.dimens.touchTarget)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(TotemTheme.dimens.radiusSm),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.5.dp,
            borderColor,
        ),
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = TotemTheme.dimens.space16,
                vertical = TotemTheme.dimens.space12,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textColor,
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
