package com.example.passagenexpress.feature.payment

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.passagenexpress.core.designsystem.component.TotemPrimaryButton
import com.example.passagenexpress.core.designsystem.component.TotemSecondaryButton
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme
import com.example.passagenexpress.feature.payment.ui.generatePixQrCode

/**
 * Tela full-screen de espera de pagamento. Sem scaffold/status-bar/step header — visual
 * objetivo: fundo animado, conteúdo principal centralizado, ação no rodapé.
 *
 *  ┌──────────────────────────────────┐
 *  │ [animated ambient gradients]     │  fundo animado pulsando
 *  │                                  │
 *  │       ⏱ Expira em XX:XX          │  timer (só nos estados Active)
 *  │                                  │
 *  │       ┌─────────────┐            │
 *  │       │   QR / POS  │            │  visual central (QR PIX ou
 *  │       │             │            │  maquininha cartão)
 *  │       └─────────────┘            │
 *  │       <subtítulo de ação>        │
 *  │                                  │
 *  │           R$ 1.234,56            │  valor destacado
 *  │             ● ● ●                │  3 dots pulsando
 *  │     [Cancelar pagamento]         │  ação no rodapé
 *  └──────────────────────────────────┘
 *
 * Em sucesso: troca pelo ✓ verde, "Voltando em Xs…" e botão "Comprar passagem".
 */
@Composable
internal fun PaymentWaitingScreen(
    state: PaymentUiState,
    autoReturnSeconds: Int?,
    onCancel: () -> Unit,
    onRetryPix: () -> Unit,
    onRetryCard: () -> Unit,
    onReturnNow: () -> Unit,
) {
    val mode = state.selectedMethod
    val totalMoney = remember(state.total) { formatMoneyValue(state.total) }
    val phase = resolveWaitingPhase(state)
    val isSuccess = phase is WaitingPhase.Terminal && phase.isSuccess
    val isTerminal = phase is WaitingPhase.Terminal

    Surface(modifier = Modifier.fillMaxSize(), color = TotemPalette.Paper) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fundo animado: ambient só quando aguardando; sucesso ganha um wash verde leve.
            if (!isTerminal) {
                AnimatedAmbientBackground()
            } else if (isSuccess) {
                SuccessAmbientBackground()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = TotemTheme.dimens.stagePaddingH,
                        vertical = TotemTheme.dimens.space32,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Topo: timer (Active com showsTimer) — fica num slot fixo pra não saltar.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TimerBanner(phase = phase)
                }

                Spacer(Modifier.weight(1f))

                // Centro: visual + subtítulo de ação.
                CenterContent(
                    mode = mode,
                    phase = phase,
                    pixCopiaCola = state.pix?.copiaECola,
                )

                Spacer(Modifier.height(TotemTheme.dimens.space24))

                // Logo abaixo do centro: valor destacado OU countdown de auto-return.
                if (isSuccess && autoReturnSeconds != null) {
                    AutoReturnLabel(seconds = autoReturnSeconds)
                } else if (!isSuccess) {
                    AmountBlock(totalMoney = totalMoney)
                }

                Spacer(Modifier.weight(1f))

                // Dots animados quando aguardando (não terminal).
                if (!isTerminal) {
                    WaitingDots()
                    Spacer(Modifier.height(TotemTheme.dimens.space24))
                }

                ActionButton(
                    phase = phase,
                    isSuccess = isSuccess,
                    mode = mode,
                    onCancel = onCancel,
                    onReturnNow = onReturnNow,
                    onRetryPix = onRetryPix,
                    onRetryCard = onRetryCard,
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    phase: WaitingPhase,
    isSuccess: Boolean,
    mode: PaymentMethod,
    onCancel: () -> Unit,
    onReturnNow: () -> Unit,
    onRetryPix: () -> Unit,
    onRetryCard: () -> Unit,
) {
    when {
        isSuccess -> TotemPrimaryButton(
            text = stringRes(R.string.payment_waiting_buy_another),
            onClick = onReturnNow,
            accent = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // Terminal retryable (Failed/Canceled/Timeout/Expired) — duas opções: tentar
        // novamente o mesmo método OU voltar pro seletor pra escolher outro.
        phase is WaitingPhase.Terminal && phase.retryable -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            TotemSecondaryButton(
                text = stringRes(R.string.payment_waiting_back_to_methods),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            TotemPrimaryButton(
                text = stringRes(R.string.payment_waiting_retry),
                onClick = if (mode == PaymentMethod.Pix) onRetryPix else onRetryCard,
                accent = true,
                modifier = Modifier.weight(1f),
            )
        }
        phase is WaitingPhase.Terminal -> TotemSecondaryButton(
            text = stringRes(R.string.payment_waiting_back_to_methods),
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        )
        else -> TotemSecondaryButton(
            text = stringRes(R.string.payment_waiting_cancel),
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private sealed interface WaitingPhase {
    data object Preparing : WaitingPhase
    data class Active(val secondsRemaining: Int, val showsTimer: Boolean) : WaitingPhase
    data object Processing : WaitingPhase
    data class Terminal(
        val icon: ImageVector,
        val tint: Color,
        val title: String,
        val retryable: Boolean,
        val isSuccess: Boolean = false,
    ) : WaitingPhase
}

@Composable
private fun resolveWaitingPhase(state: PaymentUiState): WaitingPhase = when (state.selectedMethod) {
    PaymentMethod.Pix -> when (state.pixStage) {
        PixStage.Generating -> WaitingPhase.Preparing
        PixStage.Active -> if (state.paymentStatus == PaymentStatus.Approved) {
            WaitingPhase.Terminal(
                icon = Icons.Filled.CheckCircle,
                tint = TotemPalette.Success,
                title = stringRes(R.string.payment_status_paid),
                retryable = false,
                isSuccess = true,
            )
        } else {
            WaitingPhase.Active(state.secondsRemaining, showsTimer = true)
        }
        PixStage.Expired -> WaitingPhase.Terminal(
            icon = Icons.Filled.TimerOff,
            tint = TotemPalette.Ink,
            title = stringRes(R.string.payment_status_expired),
            retryable = true,
        )
        is PixStage.Failed -> WaitingPhase.Terminal(
            icon = Icons.Filled.Error,
            tint = TotemPalette.Error,
            title = state.pixStage.message,
            retryable = true,
        )
        PixStage.Idle -> WaitingPhase.Preparing
    }
    PaymentMethod.Card -> when (val s = state.card.stage) {
        CardStage.Creating -> WaitingPhase.Preparing
        CardStage.Waiting -> WaitingPhase.Active(state.card.secondsRemaining, showsTimer = true)
        CardStage.Processing -> WaitingPhase.Processing
        CardStage.Success -> WaitingPhase.Terminal(
            icon = Icons.Filled.CheckCircle,
            tint = TotemPalette.Success,
            title = stringRes(R.string.payment_card_success),
            retryable = false,
            isSuccess = true,
        )
        CardStage.Canceled -> WaitingPhase.Terminal(
            icon = Icons.Filled.Cancel,
            tint = TotemPalette.Ink,
            title = stringRes(R.string.payment_card_canceled),
            retryable = true,
        )
        is CardStage.Failed -> WaitingPhase.Terminal(
            icon = Icons.Filled.Error,
            tint = TotemPalette.Error,
            title = cardErrorFor(s.message),
            retryable = true,
        )
        CardStage.Timeout -> WaitingPhase.Terminal(
            icon = Icons.Filled.TimerOff,
            tint = TotemPalette.Ink,
            title = stringRes(R.string.payment_card_timeout),
            retryable = true,
        )
        CardStage.Idle -> WaitingPhase.Preparing
    }
}

@Composable
private fun TimerBanner(phase: WaitingPhase) {
    val seconds = (phase as? WaitingPhase.Active)
        ?.takeIf { it.showsTimer }
        ?.secondsRemaining
        ?: return
    Surface(
        shape = RoundedCornerShape(TotemTheme.dimens.radiusPill),
        color = TotemPalette.PaperWarm,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, TotemPalette.Hairline),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = TotemTheme.dimens.space20,
                vertical = TotemTheme.dimens.space12,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                tint = TotemPalette.Accent,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResFmt(R.string.payment_waiting_expires_in, formatTimer(seconds)),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TotemPalette.Ink,
            )
        }
    }
}

@Composable
private fun CenterContent(
    mode: PaymentMethod,
    phase: WaitingPhase,
    pixCopiaCola: String?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space20),
    ) {
        when (phase) {
            is WaitingPhase.Terminal -> TerminalCenter(phase)
            WaitingPhase.Preparing -> PreparingCenter()
            WaitingPhase.Processing -> ProcessingCenter()
            is WaitingPhase.Active -> when (mode) {
                PaymentMethod.Pix -> PixCenter(content = pixCopiaCola.orEmpty())
                PaymentMethod.Card -> CardCenter()
            }
        }
        SubtitleText(text = waitingSubtitle(mode = mode, phase = phase))
    }
}

@Composable
private fun PixCenter(content: String) {
    val bitmap = remember(content) { generatePixQrCode(content) }
    Surface(
        shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
        color = TotemPalette.Paper,
        border = androidx.compose.foundation.BorderStroke(2.dp, TotemPalette.Ink),
        modifier = Modifier.size(360.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(20.dp)) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                CircularProgressIndicator(color = TotemPalette.Accent)
            }
        }
    }
}

@Composable
private fun CardCenter() {
    LottieTile(rawRes = R.raw.payment_waiting_card)
}

@Composable
private fun PreparingCenter() {
    Box(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = TotemPalette.Accent,
            strokeWidth = 6.dp,
            modifier = Modifier.size(96.dp),
        )
    }
}

@Composable
private fun ProcessingCenter() {
    LottieTile(rawRes = R.raw.payment_waiting_card, showSpinner = true)
}

@Composable
private fun TerminalCenter(phase: WaitingPhase.Terminal) {
    val bg = when (phase.tint) {
        TotemPalette.Success -> TotemPalette.SuccessLight
        TotemPalette.Error -> TotemPalette.ErrorLight
        else -> TotemPalette.PaperWarm
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space20),
    ) {
        // Sucesso: Lottie de pagamento aprovado (halo + circle + check). Outros terminais
        // mantêm o ícone estático com fundo tonal.
        if (phase.isSuccess) {
            ApprovedLottie()
        } else {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .background(bg, RoundedCornerShape(TotemTheme.dimens.radiusLg)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = phase.icon,
                    contentDescription = null,
                    tint = phase.tint,
                    modifier = Modifier.size(120.dp),
                )
            }
        }
        Text(
            text = phase.title,
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
            // Lottie de sucesso usa azul como cor principal — título acompanha.
            color = if (phase.isSuccess) TotemPalette.Accent else TotemPalette.Ink,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Tile padronizado pro centro do waiting de cartão: Lottie em loop infinito dentro de
 * um container `AccentTint`. Os JSONs em `res/raw/` são placeholders — pra trocar
 * por animações finais, basta substituir os arquivos mantendo o mesmo nome.
 */
@Composable
private fun LottieTile(
    rawRes: Int,
    showSpinner: Boolean = false,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    Box(
        modifier = Modifier.size(320.dp),
        contentAlignment = Alignment.Center,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(240.dp),
        )
        if (showSpinner) {
            CircularProgressIndicator(
                color = TotemPalette.Accent,
                strokeWidth = 5.dp,
                modifier = Modifier.size(280.dp),
            )
        }
    }
}

/** Lottie de pagamento aprovado — toca uma vez (halo + circle + check + estrelas). */
@Composable
private fun ApprovedLottie() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.payment_approved)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.size(440.dp),
    )
}

@Composable
private fun SubtitleText(text: String?) {
    if (text == null) return
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
        color = TotemPalette.Ink2,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun waitingSubtitle(mode: PaymentMethod, phase: WaitingPhase): String? = when {
    phase is WaitingPhase.Terminal -> null
    mode == PaymentMethod.Pix && phase is WaitingPhase.Preparing ->
        stringRes(R.string.payment_waiting_pix_generating_title)
    mode == PaymentMethod.Pix -> stringRes(R.string.payment_waiting_pix_subtitle)
    mode == PaymentMethod.Card && phase is WaitingPhase.Preparing ->
        stringRes(R.string.payment_waiting_card_sending_title)
    mode == PaymentMethod.Card && phase is WaitingPhase.Processing ->
        stringRes(R.string.payment_waiting_card_processing_subtitle)
    else -> stringRes(R.string.payment_waiting_card_subtitle)
}

@Composable
private fun AmountBlock(totalMoney: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space4),
    ) {
        Text(
            text = stringRes(R.string.payment_waiting_amount).uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
            ),
            color = TotemPalette.InkMuted,
        )
        Text(
            text = "R$ $totalMoney",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
            ),
            color = TotemPalette.Ink,
        )
    }
}

@Composable
private fun AutoReturnLabel(seconds: Int) {
    Text(
        text = stringResFmt(R.string.payment_waiting_auto_return, seconds),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
        color = TotemPalette.InkMuted,
    )
}

@Composable
private fun WaitingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { i ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, delayMillis = i * 220, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot-$i",
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer(alpha = alpha)
                    .background(TotemPalette.Accent, CircleShape),
            )
        }
    }
}

/**
 * Ambient: 3 soft radial gradients pulsando devagar em cantos diferentes. Sem blur
 * (minSdk=30 não tem `Modifier.blur` — só API 31+). Cada gradiente tem fase e velocidade
 * próprias pra evitar movimento sincronizado.
 */
@Composable
private fun BoxScope.AnimatedAmbientBackground() {
    val transition = rememberInfiniteTransition(label = "ambient")
    val s1 by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "s1",
    )
    val s2 by transition.animateFloat(
        initialValue = 1.12f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(4600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "s2",
    )
    val s3 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "s3",
    )
    AmbientBlob(
        modifier = Modifier
            .align(Alignment.TopStart)
            .size(width = 600.dp, height = 600.dp)
            .graphicsLayer(scaleX = s1, scaleY = s1, alpha = 0.6f),
        color = TotemPalette.AccentTint,
    )
    AmbientBlob(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .size(width = 640.dp, height = 640.dp)
            .graphicsLayer(scaleX = s2, scaleY = s2, alpha = 0.55f),
        color = TotemPalette.AccentTint,
    )
    AmbientBlob(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .size(width = 420.dp, height = 420.dp)
            .graphicsLayer(scaleX = s3, scaleY = s3, alpha = 0.4f),
        color = TotemPalette.Primary100,
    )
}

@Composable
private fun BoxScope.SuccessAmbientBackground() {
    val transition = rememberInfiniteTransition(label = "success-ambient")
    val s1 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "s1",
    )
    AmbientBlob(
        modifier = Modifier
            .align(Alignment.Center)
            .size(900.dp)
            .graphicsLayer(scaleX = s1, scaleY = s1, alpha = 0.7f),
        color = TotemPalette.SuccessLight,
    )
}

@Composable
private fun AmbientBlob(modifier: Modifier, color: Color) {
    Box(
        modifier = modifier.background(
            brush = Brush.radialGradient(
                colors = listOf(color, Color.Transparent),
                center = Offset.Unspecified,
            ),
        ),
    )
}

@Composable
private fun cardErrorFor(message: String): String = when (message) {
    PaymentViewModel.CARD_ERROR_GENERIC -> stringRes(R.string.payment_card_failed)
    else -> message
}

private fun formatTimer(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val m = safe / 60
    val s = safe % 60
    return "%02d:%02d".format(m, s)
}

private fun formatMoneyValue(value: Double): String =
    String.format(java.util.Locale.forLanguageTag("pt-BR"), "%,.2f", value)

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)

@Composable
private fun stringResFmt(id: Int, vararg args: Any): String =
    androidx.compose.ui.res.stringResource(id, *args)

/**
 * Preview do estado terminal de sucesso — Lottie de pagamento aprovado + título azul +
 * countdown de auto-return. Renderiza só a parte visual do centro (Lottie + frase) num
 * fundo `Paper` pra mostrar o resultado final no Android Studio.
 */
@Preview(name = "Approved (success terminal)", widthDp = 800, heightDp = 720, showBackground = true)
@Composable
private fun ApprovedSuccessPreview() {
    com.example.passagenexpress.core.designsystem.theme.TotemTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TotemPalette.Paper),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space20),
            ) {
                ApprovedLottie()
                Text(
                    text = "Pagamento aprovado!",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = TotemPalette.Accent,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Voltando em 5s…",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = TotemPalette.InkMuted,
                )
            }
        }
    }
}
