package com.example.passagenexpress.feature.bus.payment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.onibus.ItemPedidoOnibus
import com.example.passagenexpress.core.domain.model.onibus.NovoPedidoOnibus
import com.example.passagenexpress.core.domain.model.onibus.PassageiroOnibus
import com.example.passagenexpress.core.domain.model.onibus.PixOnibus
import com.example.passagenexpress.core.domain.model.onibus.StatusPedidoOnibus
import com.example.passagenexpress.core.domain.model.onibus.TipoDocOnibus
import com.example.passagenexpress.core.domain.usecase.onibus.CriarPedidoOnibusUseCase
import com.example.passagenexpress.core.domain.usecase.onibus.GerarPixOnibusUseCase
import com.example.passagenexpress.core.domain.usecase.onibus.ObterStatusPedidoOnibusUseCase
import com.example.passagenexpress.feature.bus.BusSaleSession
import com.example.passagenexpress.feature.bus.BusSaleState
import com.example.passagenexpress.feature.bus.PernaSelecionada
import com.example.passagenexpress.feature.bus.passengers.cpfValido
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.math.abs

/** Motivo de falha mapeado das mensagens do back (todas vêm HTTP 400, diferenciadas pelo texto). */
enum class FalhaPagamento { AssentoIndisponivel, Recusado, Generica }

sealed interface PagamentoStage {
    data object CriandoPedido : PagamentoStage
    /** Nenhum passageiro tem CPF — o Pix de visitante exige o CPF do pagador. */
    data object PrecisaCpf : PagamentoStage
    data object GerandoPix : PagamentoStage
    data class Aguardando(val pix: PixOnibus, val segundosRestantes: Long) : PagamentoStage
    data object Expirado : PagamentoStage
    data object Pago : PagamentoStage
    data class Erro(val falha: FalhaPagamento, val mensagem: String) : PagamentoStage
}

data class BusPaymentUiState(
    val resumo: BusSaleState = BusSaleState(),
    val stage: PagamentoStage = PagamentoStage.CriandoPedido,
    val cpfPagador: String = "",
    val cpfErro: Boolean = false,
    val pedidoId: Long? = null,
)

@HiltViewModel
class BusPaymentViewModel @Inject constructor(
    private val session: BusSaleSession,
    private val criarPedido: CriarPedidoOnibusUseCase,
    private val gerarPix: GerarPixOnibusUseCase,
    private val obterStatus: ObterStatusPedidoOnibusUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusPaymentUiState(resumo = session.value))
    val uiState: StateFlow<BusPaymentUiState> = _uiState.asStateFlow()

    private var pixJob: Job? = null

    init {
        iniciar()
    }

    private fun iniciar() {
        viewModelScope.launch {
            val pedidoId = session.value.pedidoId ?: run {
                _uiState.update { it.copy(stage = PagamentoStage.CriandoPedido) }
                // Sessão incompleta (não deveria acontecer) vira erro na tela, nunca crash.
                val novo = runCatching { montarPedido(session.value) }.getOrElse { e ->
                    falhar(e.message.orEmpty())
                    return@launch
                }
                when (val r = criarPedido(novo)) {
                    is AppResult.Success -> {
                        session.setPedido(r.value.id)
                        r.value.id
                    }
                    is AppResult.Failure -> {
                        falhar(r.error.message.orEmpty())
                        return@launch
                    }
                }
            }
            _uiState.update { it.copy(pedidoId = pedidoId) }
            val cpf = cpfDoPagador()
            if (cpf == null) {
                _uiState.update { it.copy(stage = PagamentoStage.PrecisaCpf) }
            } else {
                gerarQr(pedidoId, cpf)
            }
        }
    }

    /** 1º passageiro com CPF; se nenhum tiver, o CPF digitado na tela. */
    private fun cpfDoPagador(): String? =
        session.value.passageiros.firstOrNull { it.tipoDoc == TipoDocOnibus.Cpf }?.documento
            ?: _uiState.value.cpfPagador.takeIf { cpfValido(it) }

    // --- CPF do pagador (só quando nenhum passageiro usa CPF) -------------

    fun onCpfDigit(c: Char) = _uiState.update {
        if (!c.isDigit() || it.cpfPagador.length >= 11) it else it.copy(cpfPagador = it.cpfPagador + c, cpfErro = false)
    }

    fun onCpfBackspace() = _uiState.update { it.copy(cpfPagador = it.cpfPagador.dropLast(1), cpfErro = false) }

    fun onCpfConfirm() {
        val s = _uiState.value
        if (!cpfValido(s.cpfPagador)) {
            _uiState.update { it.copy(cpfErro = true) }
            return
        }
        s.pedidoId?.let { gerarQr(it, s.cpfPagador) }
    }

    // --- Pix --------------------------------------------------------------

    /** "Gerar novo QR" depois de expirar, ou "Tentar de novo" depois de erro genérico. */
    fun onRetry() {
        val pedidoId = _uiState.value.pedidoId
        if (pedidoId == null) {
            iniciar()
            return
        }
        val cpf = cpfDoPagador()
        if (cpf == null) _uiState.update { it.copy(stage = PagamentoStage.PrecisaCpf) } else gerarQr(pedidoId, cpf)
    }

    private fun gerarQr(pedidoId: Long, cpf: String) {
        pixJob?.cancel()
        pixJob = viewModelScope.launch {
            _uiState.update { it.copy(stage = PagamentoStage.GerandoPix) }
            val nome = session.value.contato?.nome.orEmpty()
            val pix = when (val r = gerarPix(pedidoId, nome, cpf)) {
                is AppResult.Success -> r.value
                is AppResult.Failure -> {
                    val msg = r.error.message.orEmpty()
                    // Pedido já pago (ex.: cliente pagou um QR anterior) → segue pra impressão.
                    if (msg.contains("já foi pago", ignoreCase = true)) {
                        confirmarPago()
                    } else {
                        falhar(msg)
                    }
                    return@launch
                }
            }
            val exibido = session.value.totalExibido
            if (abs(pix.amount - exibido) > 0.01) {
                // O valor cobrado é o do back; a divergência é só registrada (ver plano, D2).
                Log.w(TAG, "Pix de ${pix.amount} diverge do total exibido $exibido (pedido $pedidoId)")
            }
            // Expiração do back; sem ela (ou absurda), 30 min como no site.
            val expira = pix.expiraEm?.takeIf { it.isAfter(Instant.now()) && Duration.between(Instant.now(), it).toHours() < 2 }
                ?: Instant.now().plus(Duration.ofMinutes(PIX_TTL_MIN))
            aguardar(pedidoId, pix, expira)
        }
    }

    private suspend fun aguardar(pedidoId: Long, pix: PixOnibus, expira: Instant) {
        var ultimoPoll = Instant.EPOCH
        while (viewModelScope.isActive) {
            val restante = Duration.between(Instant.now(), expira).seconds
            if (restante <= 0) {
                _uiState.update { it.copy(stage = PagamentoStage.Expirado) }
                return
            }
            _uiState.update { it.copy(stage = PagamentoStage.Aguardando(pix, restante)) }
            if (Duration.between(ultimoPoll, Instant.now()).seconds >= POLL_SECONDS) {
                ultimoPoll = Instant.now()
                when ((obterStatus(pedidoId) as? AppResult.Success)?.value) {
                    StatusPedidoOnibus.Pago -> {
                        confirmarPago()
                        return
                    }
                    StatusPedidoOnibus.Negado, StatusPedidoOnibus.Cancelado -> {
                        _uiState.update { it.copy(stage = PagamentoStage.Expirado) }
                        return
                    }
                    // Pendente ou falha de rede: segue tentando no próximo ciclo.
                    else -> Unit
                }
            }
            delay(1_000)
        }
    }

    private fun confirmarPago() {
        session.marcarPago()
        _uiState.update { it.copy(stage = PagamentoStage.Pago) }
    }

    private fun falhar(mensagem: String) {
        val falha = when {
            mensagem.contains("indisponível", ignoreCase = true) -> FalhaPagamento.AssentoIndisponivel
            mensagem.contains("recusado", ignoreCase = true) || mensagem.contains("antifraude", ignoreCase = true) ->
                FalhaPagamento.Recusado
            else -> FalhaPagamento.Generica
        }
        Log.w(TAG, "Falha no pagamento de ônibus: $mensagem")
        _uiState.update { it.copy(stage = PagamentoStage.Erro(falha, mensagem)) }
    }

    override fun onCleared() {
        pixJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val TAG = "BusPayment"
        private const val POLL_SECONDS = 10L
        private const val PIX_TTL_MIN = 30L

        /**
         * Monta o carrinho a partir da sessão: um item por passageiro em cada perna (os passageiros
         * são espelhados por índice nas conexões e na volta, como `espelharEscalas()` do site).
         */
        internal fun montarPedido(s: BusSaleState): NovoPedidoOnibus {
            fun itens(pernas: List<PernaSelecionada>, passageiros: List<PassageiroOnibus>) =
                pernas.flatMap { perna ->
                    perna.assentos.mapIndexedNotNull { i, assento ->
                        val passageiro = passageiros.getOrNull(i) ?: return@mapIndexedNotNull null
                        ItemPedidoOnibus(
                            assento = assento,
                            tripUuid = perna.tripUuid,
                            tripLeg = perna.tripLeg,
                            origemSlug = perna.origemSlug,
                            destinoSlug = perna.destinoSlug,
                            dataEmbarque = perna.dataEmbarque,
                            passageiro = passageiro,
                        )
                    }
                }
            val ida = s.ida ?: error("Venda sem viagem de ida")
            val contato = s.contato ?: error("Venda sem contato")
            return NovoPedidoOnibus(
                tripUuidIda = ida.viagem.tripUuid,
                itensIda = itens(ida.pernas, s.passageiros),
                tripUuidVolta = s.volta?.viagem?.tripUuid,
                itensVolta = itens(s.volta?.pernas.orEmpty(), s.passageiros),
                contato = contato,
            )
        }
    }
}
