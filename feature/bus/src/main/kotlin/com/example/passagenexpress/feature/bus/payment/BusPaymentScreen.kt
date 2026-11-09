package com.example.passagenexpress.feature.bus.payment

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passagenexpress.core.designsystem.component.TotemLoading
import com.example.passagenexpress.core.designsystem.component.TotemNumericKeypad
import com.example.passagenexpress.core.designsystem.component.TotemPrimaryButton
import com.example.passagenexpress.core.designsystem.component.TotemScreenScaffold
import com.example.passagenexpress.core.designsystem.component.TotemSecondaryButton
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme
import com.example.passagenexpress.core.domain.model.onibus.PrecoOnibus
import com.example.passagenexpress.core.domain.model.onibus.TipoDocOnibus
import com.example.passagenexpress.feature.bus.BusSaleState
import com.example.passagenexpress.feature.bus.PernaSelecionada
import com.example.passagenexpress.feature.bus.R
import com.example.passagenexpress.feature.bus.passengers.mascaraDocumento
import com.example.passagenexpress.feature.bus.search.TOTAL_STEPS
import com.example.passagenexpress.feature.bus.ui.formatMoney
import com.example.passagenexpress.feature.bus.ui.formatShortDate
import com.example.passagenexpress.feature.bus.ui.formatTime
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
fun BusPaymentScreen(
    onPaid: (pedidoId: Long) -> Unit,
    /** Assento perdido na revalidação: volta pra escolher assentos de novo. */
    onSeatsLost: () -> Unit,
    onBack: () -> Unit,
    viewModel: BusPaymentViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.stage) {
        if (state.stage is PagamentoStage.Pago) state.pedidoId?.let(onPaid)
    }

    val aguardando = state.stage is PagamentoStage.Aguardando
    TotemScreenScaffold(
        step = 5,
        totalSteps = TOTAL_STEPS,
        title = stringResource(R.string.bus_payment_title),
        subtitle = stringResource(R.string.bus_payment_subtitle),
        footer = {
            // Durante a espera do Pix o Voltar fica escondido — evita abandonar um QR já pago.
            if (!aguardando) TotemSecondaryButton(text = stringResource(R.string.bus_back), onClick = onBack)
        },
        stickyBottom = if (state.stage is PagamentoStage.PrecisaCpf) ({
            TotemNumericKeypad(
                label = stringResource(R.string.bus_payer_cpf),
                currentValue = mascaraDocumento(state.cpfPagador, TipoDocOnibus.Cpf),
                onDigit = viewModel::onCpfDigit,
                onBackspace = viewModel::onCpfBackspace,
                onClose = {},
                onDone = viewModel::onCpfConfirm,
                doneLabel = stringResource(R.string.bus_generate_pix),
                closeContentDesc = stringResource(R.string.bus_keypad_close),
                deleteContentDesc = stringResource(R.string.bus_keypad_delete),
            )
        }) else null,
    ) {
        // Um embaixo do outro: resumo compacto em cima, pagamento embaixo.
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space24),
        ) {
            // O total aparece uma vez só: com o QR gerado, é o valor efetivamente cobrado no Pix.
            val total = (state.stage as? PagamentoStage.Aguardando)?.pix?.amount ?: state.resumo.totalExibido
            Resumo(state.resumo, total = total, modifier = Modifier.fillMaxWidth())
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (val stage = state.stage) {
                    PagamentoStage.CriandoPedido -> TotemLoading(label = stringResource(R.string.bus_creating_order))
                    PagamentoStage.GerandoPix -> TotemLoading(label = stringResource(R.string.bus_generating_pix))
                    PagamentoStage.PrecisaCpf -> PrecisaCpf(state.cpfErro)
                    is PagamentoStage.Aguardando -> PixPanel(stage)
                    PagamentoStage.Expirado -> Aviso(
                        icon = { Icon(Icons.Filled.TimerOff, null, tint = TotemPalette.WarningDark, modifier = Modifier.size(56.dp)) },
                        titulo = stringResource(R.string.bus_pix_expired_title),
                        mensagem = stringResource(R.string.bus_pix_expired_message),
                        acao = stringResource(R.string.bus_pix_new_qr),
                        onAcao = viewModel::onRetry,
                    )
                    PagamentoStage.Pago -> TotemLoading(label = stringResource(R.string.bus_payment_confirmed))
                    is PagamentoStage.Erro -> when (stage.falha) {
                        FalhaPagamento.AssentoIndisponivel -> Aviso(
                            icon = { Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp)) },
                            titulo = stringResource(R.string.bus_seat_lost_title),
                            mensagem = stringResource(R.string.bus_seat_lost_message),
                            acao = stringResource(R.string.bus_choose_seats_again),
                            onAcao = onSeatsLost,
                        )
                        // Mensagem técnica do back não aparece pro cliente (vai pro logcat no VM).
                        else -> Aviso(
                            icon = { Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp)) },
                            titulo = stringResource(R.string.bus_payment_error_title),
                            mensagem = stringResource(R.string.bus_payment_error_message),
                            acao = stringResource(R.string.bus_retry),
                            onAcao = viewModel::onRetry,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Resumo(resumo: BusSaleState, total: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = TotemPalette.Paper,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
        border = BorderStroke(1.5.dp, TotemPalette.Hairline),
    ) {
        Column(
            modifier = Modifier.padding(TotemTheme.dimens.space20),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            resumo.ida?.pernas?.forEach { PernaResumo(stringResource(R.string.bus_outbound), it, resumo.passageiros.size) }
            resumo.volta?.pernas?.forEach { PernaResumo(stringResource(R.string.bus_return), it, resumo.passageiros.size) }
            Box(Modifier.fillMaxWidth().background(TotemPalette.Hairline).size(1.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.bus_total),
                        style = MaterialTheme.typography.titleMedium,
                        color = TotemPalette.Ink,
                    )
                    Text(
                        text = stringResource(R.string.bus_fee_included),
                        style = MaterialTheme.typography.labelMedium,
                        color = TotemPalette.InkMuted,
                    )
                }
                Text(
                    text = formatMoney(total),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = TotemPalette.Accent,
                )
            }
        }
    }
}

@Composable
private fun PernaResumo(sentido: String, perna: PernaSelecionada, passageiros: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = sentido.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = TotemPalette.Accent,
        )
        Text(
            text = "${perna.origemNome} → ${perna.destinoNome}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TotemPalette.Ink,
        )
        Text(
            text = "${formatShortDate(perna.saida.toLocalDate())} · ${formatTime(perna.saida)} · " +
                stringResource(R.string.bus_seats_list, perna.assentos.joinToString { it.label }) + " · " +
                stringResource(R.string.bus_passengers_count, passageiros),
            style = MaterialTheme.typography.bodyMedium,
            color = TotemPalette.InkMuted,
        )
    }
}

@Composable
private fun PixPanel(stage: PagamentoStage.Aguardando) {
    val qr: ImageBitmap? = remember(stage.pix.copiaECola, stage.pix.qrCodeDataUri) {
        gerarQr(stage.pix.copiaECola) ?: decodeDataUri(stage.pix.qrCodeDataUri)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
    ) {
        Text(
            text = stringResource(R.string.bus_pix_scan),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TotemPalette.Ink,
            textAlign = TextAlign.Center,
        )
        Surface(
            color = TotemPalette.Paper,
            shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
            border = BorderStroke(2.dp, TotemPalette.Hairline),
        ) {
            Box(modifier = Modifier.padding(TotemTheme.dimens.space16).size(320.dp), contentAlignment = Alignment.Center) {
                if (qr != null) {
                    Image(bitmap = qr, contentDescription = stringResource(R.string.bus_pix_qr_desc), modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.QrCode2, null, tint = TotemPalette.InkSoft, modifier = Modifier.size(120.dp))
                }
            }
        }
        val min = stage.segundosRestantes / 60
        val seg = stage.segundosRestantes % 60
        Text(
            text = stringResource(R.string.bus_pix_expires_in, "%02d:%02d".format(min, seg)),
            style = MaterialTheme.typography.titleMedium,
            color = if (stage.segundosRestantes < 120) TotemPalette.WarningDark else TotemPalette.InkMuted,
        )
        Text(
            text = stringResource(R.string.bus_pix_waiting),
            style = MaterialTheme.typography.bodyLarge,
            color = TotemPalette.InkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp),
        )
    }
}

@Composable
private fun PrecisaCpf(erro: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
    ) {
        Text(
            text = stringResource(R.string.bus_payer_cpf_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TotemPalette.Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.bus_payer_cpf_message),
            style = MaterialTheme.typography.bodyLarge,
            color = TotemPalette.InkMuted,
            textAlign = TextAlign.Center,
        )
        if (erro) {
            Text(
                text = stringResource(R.string.bus_error_cpf),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun Aviso(
    icon: @Composable () -> Unit,
    titulo: String,
    mensagem: String,
    acao: String,
    onAcao: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        modifier = Modifier.widthIn(max = 520.dp),
    ) {
        Box(
            modifier = Modifier.size(110.dp).background(TotemPalette.PaperDim, CircleShape),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = TotemPalette.Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = mensagem,
            style = MaterialTheme.typography.bodyLarge,
            color = TotemPalette.InkMuted,
            textAlign = TextAlign.Center,
        )
        TotemPrimaryButton(text = acao, onClick = onAcao, trailingArrow = false)
    }
}

/** QR a partir do "copia e cola" (mesma técnica do pagamento de barco). */
private fun gerarQr(content: String, sizePx: Int = 640): ImageBitmap? {
    if (content.isBlank()) return null
    return runCatching {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
        )
        val m = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(m.width * m.height) { i ->
            if (m.get(i % m.width, i / m.width)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
        android.graphics.Bitmap.createBitmap(pixels, m.width, m.height, android.graphics.Bitmap.Config.ARGB_8888)
            .asImageBitmap()
    }.getOrNull()
}

/** Fallback: o PNG que o back manda em `qr_code` (data URI). */
private fun decodeDataUri(dataUri: String?): ImageBitmap? {
    if (dataUri.isNullOrBlank()) return null
    return runCatching {
        val raw = Base64.decode(dataUri.substringAfter(",").filterNot { it.isWhitespace() }, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(raw, 0, raw.size)?.asImageBitmap()
    }.getOrNull()
}
