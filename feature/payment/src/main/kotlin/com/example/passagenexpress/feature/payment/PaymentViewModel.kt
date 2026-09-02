package com.example.passagenexpress.feature.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.ItemPedido
import com.example.passagenexpress.core.domain.model.NovaPointTapOrder
import com.example.passagenexpress.core.domain.model.Pedido
import com.example.passagenexpress.core.domain.model.PedidoStatus
import com.example.passagenexpress.core.domain.model.PointTapPaymentType
import com.example.passagenexpress.core.domain.model.PointTapStatus
import com.example.passagenexpress.core.domain.repository.NovoPedido
import com.example.passagenexpress.core.domain.usecase.CancelarPointTapOrderUseCase
import com.example.passagenexpress.core.domain.usecase.CriarPedidoUseCase
import com.example.passagenexpress.core.domain.usecase.CriarPointTapOrderUseCase
import com.example.passagenexpress.core.domain.usecase.GerarPagamentoPixUseCase
import com.example.passagenexpress.core.domain.usecase.ObterPointTapStatusUseCase
import com.example.passagenexpress.core.domain.usecase.ObterStatusPedidoUseCase
import com.example.passagenexpress.core.domain.model.TipoDocumento
import com.example.passagenexpress.core.domain.model.precoBaseComDesconto
import com.example.passagenexpress.feature.passenger.format.DocumentoMask
import com.example.passagenexpress.feature.passenger.format.PhoneMask
import com.example.passagenexpress.feature.payment.navigation.decodeSaleArg
import com.example.passagenexpress.feature.payment.navigation.decodeTrechoArg
import com.example.passagenexpress.feature.payment.navigation.toPassageiroDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.roundToLong

@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val criarPedido: CriarPedidoUseCase,
    private val gerarPagamentoPix: GerarPagamentoPixUseCase,
    private val obterStatusPedido: ObterStatusPedidoUseCase,
    private val criarPointTapOrder: CriarPointTapOrderUseCase,
    private val obterPointTapStatus: ObterPointTapStatusUseCase,
    private val cancelarPointTapOrder: CancelarPointTapOrderUseCase,
) : ViewModel() {

    private val trecho = decodeTrechoArg(
        savedStateHandle.get<String>(ARG_TRECHO).orEmpty()
    ).toDomain()

    private val sale = decodeSaleArg(
        savedStateHandle.get<String>(ARG_SALE).orEmpty()
    )
    private val passageiros = sale.passageiros
    private val contatoSnapshot = sale.contato

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var pollingJob: Job? = null
    private var cardPollingJob: Job? = null
    private var cardTimerJob: Job? = null

    init {
        startCriarPedido()
    }

    fun onRetryCreateOrder() {
        if (_uiState.value.orderStage !is OrderStage.Failed) return
        _uiState.update { it.copy(orderStage = OrderStage.Creating) }
        startCriarPedido()
    }

    fun onSelectMethod(method: PaymentMethod) {
        if (_uiState.value.selectedMethod == method) return
        _uiState.update { it.copy(selectedMethod = method) }
    }

    fun onChangeContactNome(value: String) {
        _uiState.update {
            it.copy(contactForm = it.contactForm.copy(nome = value, nomeError = null))
        }
    }

    fun onOpenContactKeypad() {
        _uiState.update { it.copy(contactForm = it.contactForm.copy(keypadOpen = true)) }
    }

    fun onCloseContactKeypad() {
        _uiState.update { it.copy(contactForm = it.contactForm.copy(keypadOpen = false)) }
    }

    fun onContactKeypadDigit(digit: Char) {
        if (!digit.isDigit()) return
        applyCpfEdit { current -> current + digit }
    }

    fun onContactKeypadBackspace() = applyCpfEdit { current -> current.dropLast(1) }

    fun onGerarPix() {
        val pedidoId = _uiState.value.pedido?.id ?: return
        val current = _uiState.value.pixStage
        if (current is PixStage.Generating || current is PixStage.Active) return
        val form = _uiState.value.contactForm
        val nomeTrim = form.nome.trim()
        val cpfDigits = DocumentoMask.digitsOnly(form.cpfDisplay, TipoDocumento.CPF)
        val nomeError = if (nomeTrim.split(" ").size < 2) ERROR_NAME_REQUIRED else null
        val cpfError = if (cpfDigits.length != 11) ERROR_CPF_INVALID else null
        if (nomeError != null || cpfError != null) {
            _uiState.update {
                it.copy(
                    contactForm = it.contactForm.copy(nomeError = nomeError, cpfError = cpfError),
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                pixStage = PixStage.Generating,
                contactForm = it.contactForm.copy(keypadOpen = false),
            )
        }
        viewModelScope.launch {
            when (val r = gerarPagamentoPix(
                pedidoId = pedidoId,
                cpf = cpfDigits,
                nome = nomeTrim,
            )) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            pix = r.value,
                            pixStage = PixStage.Active,
                            secondsRemaining = PIX_TIMEOUT_SECONDS,
                            paymentStatus = PaymentStatus.Pending,
                        )
                    }
                    startTimer()
                    startStatusPolling(pedidoId)
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(pixStage = PixStage.Failed(r.error.message ?: "Erro ao gerar PIX"))
                }
            }
        }
    }

    /** Dispara a criação da intent Point Tap (botão "Pagar com cartão"). */
    fun onPagarComCartao() {
        val pedido = _uiState.value.pedido ?: return
        val current = _uiState.value.card.stage
        if (current is CardStage.Creating || current is CardStage.Waiting ||
            current is CardStage.Processing || current is CardStage.Success
        ) return
        val options = _uiState.value.card.options
        _uiState.update {
            it.copy(
                card = it.card.copy(
                    stage = CardStage.Creating,
                    secondsRemaining = CARD_TIMEOUT_SECONDS,
                ),
            )
        }
        viewModelScope.launch {
            val input = NovaPointTapOrder(
                pedidoId = pedido.id,
                amountCents = (_uiState.value.total * 100).roundToLong(),
                description = "Pedido #${pedido.id}",
                // Débito é sempre 1x; crédito usa o que o usuário escolheu.
                installments = if (options.type == CardType.Credit) options.installments else 1,
                paymentType = when (options.type) {
                    CardType.Credit -> PointTapPaymentType.CreditCard
                    CardType.Debit -> PointTapPaymentType.DebitCard
                },
                externalReference = pedido.id.toString(),
            )
            when (val r = criarPointTapOrder(input)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            card = it.card.copy(
                                stage = mapCardStage(r.value.status),
                                orderId = r.value.id,
                            ),
                        )
                    }
                    startCardTimer()
                    // Polling/cancel são chaveados pelo id do pedido (não pelo order_id).
                    startCardPolling(pedido.id)
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(
                        card = CardState(
                            stage = CardStage.Failed(r.error.message ?: "Erro ao iniciar pagamento")
                        )
                    )
                }
            }
        }
    }

    fun onSelectCardType(type: CardType) {
        _uiState.update {
            // Voltar pra débito reseta o parcelamento (débito não parcela).
            val newInstallments = if (type == CardType.Debit) 1 else it.card.options.installments
            it.copy(
                card = it.card.copy(
                    options = it.card.options.copy(type = type, installments = newInstallments),
                ),
            )
        }
    }

    fun onSelectInstallments(installments: Int) {
        if (installments < 1 || installments > MAX_INSTALLMENTS) return
        _uiState.update {
            it.copy(card = it.card.copy(options = it.card.options.copy(installments = installments)))
        }
    }

    /**
     * Reset do estado do cartão pro Idle (volta pro seletor). Usado pelo botão
     * "Voltar" nos terminais retryable — nesses casos o intent já foi resolvido
     * pelo backend (Canceled/Error/Timeout), então não precisamos disparar DELETE.
     */
    fun onRetryCartao() {
        cardPollingJob?.cancel()
        cardTimerJob?.cancel()
        _uiState.update { it.copy(card = CardState()) }
    }

    /**
     * Cancela qualquer fluxo (PIX ou cartão) em andamento e volta pra tela do seletor.
     * No cartão: captura o `orderId` ANTES de resetar e dispara o POST /cancel no backend
     * fire-and-forget — caso contrário a maquininha fica com a order OPEN pendurada e
     * a próxima criação retorna 409 ("queued order for the device").
     */
    fun onCancelarPagamento() {
        when (_uiState.value.selectedMethod) {
            PaymentMethod.Pix -> {
                timerJob?.cancel()
                pollingJob?.cancel()
                _uiState.update {
                    it.copy(
                        pixStage = PixStage.Idle,
                        pix = null,
                        secondsRemaining = 0,
                    )
                }
            }
            PaymentMethod.Card -> {
                // Só cancela se já houve order criada (orderId != null); a chamada usa o id do pedido.
                val hadOrder = _uiState.value.card.orderId != null
                val pedidoId = _uiState.value.pedido?.id
                cardPollingJob?.cancel()
                cardTimerJob?.cancel()
                _uiState.update { it.copy(card = CardState()) }
                if (hadOrder && pedidoId != null) fireAndForgetCancel(pedidoId)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        pollingJob?.cancel()
        cardPollingJob?.cancel()
        cardTimerJob?.cancel()
        // Se o usuário saiu da tela com order ainda aberta, cancela do lado MP via backend.
        val card = _uiState.value.card
        val stillOpen = card.stage is CardStage.Waiting ||
            card.stage is CardStage.Processing ||
            card.stage is CardStage.Creating
        // Só cancela se a order já foi criada (orderId != null); cancela pelo id do pedido.
        if (stillOpen && card.orderId != null) {
            _uiState.value.pedido?.id?.let { fireAndForgetCancel(it) }
        }
    }

    private fun startCriarPedido() {
        viewModelScope.launch {
            val novo = buildNovoPedido()
            when (val r = criarPedido(novo)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(pedido = r.value, orderStage = OrderStage.Ready)
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(orderStage = OrderStage.Failed(r.error.message ?: "Erro ao criar pedido"))
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.secondsRemaining > 0 &&
                _uiState.value.pixStage is PixStage.Active &&
                _uiState.value.paymentStatus == PaymentStatus.Pending
            ) {
                delay(1_000)
                _uiState.update { it.copy(secondsRemaining = it.secondsRemaining - 1) }
            }
            if (_uiState.value.secondsRemaining == 0 &&
                _uiState.value.paymentStatus == PaymentStatus.Pending
            ) {
                pollingJob?.cancel()
                _uiState.update { it.copy(pixStage = PixStage.Expired, pix = null) }
            }
        }
    }

    private fun startStatusPolling(pedidoId: Long) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (_uiState.value.pixStage is PixStage.Active &&
                _uiState.value.paymentStatus == PaymentStatus.Pending
            ) {
                delay(POLLING_INTERVAL_MS)
                when (val r = obterStatusPedido(pedidoId)) {
                    is AppResult.Success -> if (r.value == PedidoStatus.Pago) {
                        timerJob?.cancel()
                        _uiState.update {
                            it.copy(
                                paymentStatus = PaymentStatus.Approved,
                                paymentApproved = PaymentApproved(pedidoId),
                            )
                        }
                        return@launch
                    }
                    is AppResult.Failure -> Unit
                }
            }
        }
    }

    /**
     * Polling do status Point Tap: a cada 3s; tolera até 3 falhas de rede consecutivas
     * antes de desistir; encerra ao atingir um status terminal ou ao ser cancelado pelo
     * timeout job.
     */
    private fun startCardPolling(pedidoId: Long) {
        cardPollingJob?.cancel()
        cardPollingJob = viewModelScope.launch {
            var consecutiveFailures = 0
            while (true) {
                delay(CARD_POLLING_INTERVAL_MS)
                when (val r = obterPointTapStatus(pedidoId)) {
                    is AppResult.Success -> {
                        consecutiveFailures = 0
                        val result = r.value
                        val stage = mapCardStage(result.status)
                        _uiState.update {
                            it.copy(
                                card = it.card.copy(
                                    stage = stage,
                                    paymentMethod = result.paymentMethod ?: it.card.paymentMethod,
                                    installments = result.installments ?: it.card.installments,
                                    amountCents = result.amountCents ?: it.card.amountCents,
                                ),
                            )
                        }
                        if (result.status.isTerminal) {
                            cardTimerJob?.cancel()
                            if (result.status == PointTapStatus.Paid) {
                                val pedidoId = _uiState.value.pedido?.id
                                if (pedidoId != null) {
                                    _uiState.update {
                                        it.copy(
                                            paymentStatus = PaymentStatus.Approved,
                                            paymentApproved = PaymentApproved(pedidoId),
                                        )
                                    }
                                }
                            }
                            return@launch
                        }
                    }
                    is AppResult.Failure -> {
                        consecutiveFailures += 1
                        if (consecutiveFailures >= CARD_POLLING_MAX_FAILURES) {
                            cardTimerJob?.cancel()
                            _uiState.update {
                                it.copy(
                                    card = it.card.copy(
                                        stage = CardStage.Failed(
                                            r.error.message ?: "Falha de rede no pagamento"
                                        ),
                                    ),
                                )
                            }
                            return@launch
                        }
                    }
                }
            }
        }
    }

    /**
     * Tick por segundo no countdown do cartão; ao chegar a zero, cancela o polling,
     * marca Timeout e dispara o POST /cancel no backend pra liberar a maquininha.
     */
    private fun startCardTimer() {
        cardTimerJob?.cancel()
        cardTimerJob = viewModelScope.launch {
            while (_uiState.value.card.secondsRemaining > 0 &&
                _uiState.value.card.stage.let { it !is CardStage.Success } &&
                _uiState.value.card.stage.let { it !is CardStage.Canceled } &&
                _uiState.value.card.stage.let { it !is CardStage.Failed }
            ) {
                delay(1_000)
                _uiState.update {
                    it.copy(card = it.card.copy(secondsRemaining = it.card.secondsRemaining - 1))
                }
            }
            if (_uiState.value.card.secondsRemaining == 0 &&
                _uiState.value.card.stage.let { it !is CardStage.Success }
            ) {
                cardPollingJob?.cancel()
                val pedidoId = _uiState.value.pedido?.id
                _uiState.update { it.copy(card = it.card.copy(stage = CardStage.Timeout)) }
                if (pedidoId != null) fireAndForgetCancel(pedidoId)
            }
        }
    }

    private fun fireAndForgetCancel(pedidoId: Long) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                cancelarPointTapOrder(pedidoId)
            }
        }
    }

    private fun initialState(): PaymentUiState {
        val taxa = trecho.taxaDeEmbarque
        // A diária de cada cômodo é cobrada 1× (camarote de 2 conta uma vez só, o 2º ocupante
        // não paga passagem); a taxa de embarque é por ocupante. Espelha `calculateTotal` do site.
        val totalPassagens = passageiros
            .distinctBy { it.comodo.id }
            .sumOf { p -> trecho.precoBaseComDesconto(p.comodo.valor) }
        val totalTaxas = passageiros.size * taxa
        // Totem coleta menos campos que a web: não há mais o bloco "Dados para Contato" nem o
        // checkbox `isContact` — o passageiro #1 vira o contato por convenção.
        val contatoPassageiro = passageiros.firstOrNull()
        val cpfDigits = contatoPassageiro?.toPassageiroDomain()
            ?.takeIf { it.tipoDocumento == TipoDocumento.CPF }
            ?.cpf
            .orEmpty()
        return PaymentUiState(
            trecho = trecho,
            totalPassageiros = passageiros.size,
            totalPassagens = totalPassagens,
            totalTaxas = totalTaxas,
            total = totalPassagens + totalTaxas,
            contactForm = ContactForm(
                nome = contatoSnapshot.nome,
                cpfDisplay = DocumentoMask.applyMask(cpfDigits, TipoDocumento.CPF),
            ),
        )
    }

    private fun applyCpfEdit(transform: (String) -> String) {
        _uiState.update { state ->
            val current = DocumentoMask.digitsOnly(state.contactForm.cpfDisplay, TipoDocumento.CPF)
            val updated = transform(current)
            state.copy(
                contactForm = state.contactForm.copy(
                    cpfDisplay = DocumentoMask.applyMask(updated, TipoDocumento.CPF),
                    cpfError = null,
                ),
            )
        }
    }

    /**
     * Monta o `NovoPedido` no shape esperado pelo backend (1 ItemPedido por passageiro/cômodo).
     * `data_hora` = data do embarque no formato `dd/MM/yyyy` (web envia `formatDate(formSale.contato.nascimento)`
     * mas para `data_hora` o backend aceita a data simples).
     */
    private fun buildNovoPedido(): NovoPedido {
        // Totem: contato é derivado do passageiro #1 (ver `PassengerViewModel.deriveContato`).
        // `contatoSnapshot` é o snapshot desse #1 — usamos pra sobrepor nome/telefone/email
        // caso o usuário tenha editado no card de PIX. Document/tipo/nascimento vêm do passageiro
        // #1 direto.
        val base = passageiros.first().toPassageiroDomain()
        val contato = base.copy(
            nome = contatoSnapshot.nome.ifBlank { base.nome },
            email = contatoSnapshot.email.takeIf { it.isNotBlank() },
            telefone = PhoneMask.digitsOnly(contatoSnapshot.telefoneDisplay)
                .ifBlank { base.telefone },
        )
        // Web monta `desconto_id: trecho.desconto?.id ?? null` em cada item de `dataComodos`
        // (home.vue#populateComodos:431). Se o trecho não tem desconto, manda null.
        val descontoId = trecho.desconto?.id
        // Ocupantes efetivos por camarote = nº de passageiros que compartilham o mesmo `comodo.id`.
        val ocupantesPorComodo = passageiros.groupingBy { it.comodo.id }.eachCount()
        val comodosVistos = mutableSetOf<Long>()
        val itens = passageiros.mapIndexed { index, p ->
            val domain = p.toPassageiroDomain()
            // Primeiro passageiro de cada camarote = pivô; os demais são filhos ligados a ele.
            val isPivo = comodosVistos.add(p.comodo.id)
            ItemPedido(
                comodoId = p.comodo.id,
                tipoComodidadeId = p.comodo.tipoComodidadeId,
                passageiro = domain,
                valor = trecho.precoBaseComDesconto(p.comodo.valor),
                taxaEmbarque = trecho.taxaDeEmbarque,
                descontoId = descontoId,
                isContact = index == 0,
                comodoRelacionado = if (isPivo) null else p.comodo.id,
                comodosFilhos = if (isPivo) p.comodo.quantidade.coerceAtLeast(1) else 1,
                qtdComodosFilhos = if (isPivo) (ocupantesPorComodo[p.comodo.id] ?: 1) else 1,
            )
        }
        return NovoPedido(
            trechoId = trecho.id,
            viagemId = trecho.idViagem,
            dataHora = trecho.dataEmbarque.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            // Passagem cobrada 1× por cômodo (pivô); taxa por ocupante. Cada item ainda leva
            // `valor` = diária no payload (igual ao site), mas o total soma a diária uma vez só.
            totalPassagens = itens.distinctBy { it.comodoId }.sumOf { it.valor },
            totalTaxas = itens.sumOf { it.taxaEmbarque },
            total = itens.distinctBy { it.comodoId }.sumOf { it.valor } + itens.sumOf { it.taxaEmbarque },
            contato = contato,
            itens = itens,
        )
    }

    private fun mapCardStage(status: PointTapStatus): CardStage = when (status) {
        // Pending = order OPEN / pedido Solicitado/Em venda — aguardando o cartão na maquininha.
        PointTapStatus.Pending -> CardStage.Waiting
        PointTapStatus.Paid -> CardStage.Success
        // Negado pelo gateway. Stage.Failed carrega um marker que a tela troca por uma copy
        // amigável ("Pagamento recusado…").
        PointTapStatus.Denied -> CardStage.Failed(CARD_PAYMENT_DENIED)
    }

    companion object {
        const val ARG_TRECHO = "trechoJson"
        const val ARG_SALE = "saleJson"

        const val ERROR_NAME_REQUIRED = "name_required"
        const val ERROR_CPF_INVALID = "cpf_invalid"

        /** Marker para a tela trocar pela string `payment_card_failed`. */
        const val CARD_ERROR_GENERIC = "card_error_generic"

        /** Marker para a tela trocar pela string `payment_card_denied` (pagamento recusado). */
        const val CARD_PAYMENT_DENIED = "card_payment_denied"

        private const val PIX_TIMEOUT_SECONDS = 30 * 60
        private const val POLLING_INTERVAL_MS = 10_000L

        private const val CARD_POLLING_INTERVAL_MS = 3_000L
        private const val CARD_TIMEOUT_SECONDS = 120
        private const val CARD_POLLING_MAX_FAILURES = 3
        const val MAX_INSTALLMENTS = 12
    }
}
