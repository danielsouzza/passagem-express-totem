package com.example.passagenexpress.feature.payment

import com.example.passagenexpress.core.domain.model.Pagamento
import com.example.passagenexpress.core.domain.model.Pedido
import com.example.passagenexpress.core.domain.model.Trecho

data class PaymentUiState(
    val trecho: Trecho? = null,
    val totalPassageiros: Int = 0,
    val totalPassagens: Double = 0.0,
    val totalTaxas: Double = 0.0,
    val total: Double = 0.0,
    val orderStage: OrderStage = OrderStage.Creating,
    val pedido: Pedido? = null,
    val selectedMethod: PaymentMethod = PaymentMethod.Pix,
    val contactForm: ContactForm = ContactForm(),
    val pix: Pagamento.Pix? = null,
    val pixStage: PixStage = PixStage.Idle,
    val secondsRemaining: Int = 0,
    val paymentStatus: PaymentStatus = PaymentStatus.Pending,
    val paymentApproved: PaymentApproved? = null,
    val card: CardState = CardState(),
)

/**
 * Formulário do contato exibido no card de PIX — pré-preenchido com o passageiro #1
 * (que vira o contato por convenção no totem) mas editável.
 */
data class ContactForm(
    val nome: String = "",
    val cpfDisplay: String = "",
    val nomeError: String? = null,
    val cpfError: String? = null,
    val keypadOpen: Boolean = false,
)

/** Estado inicial: criando o pedido após chegar na tela. */
sealed interface OrderStage {
    data object Creating : OrderStage
    data class Failed(val message: String) : OrderStage
    data object Ready : OrderStage
}

/** Estado do PIX em si (botão "Gerar" vira QR + polling). */
sealed interface PixStage {
    data object Idle : PixStage
    data object Generating : PixStage
    data object Active : PixStage
    data class Failed(val message: String) : PixStage
    data object Expired : PixStage
}

enum class PaymentStatus { Pending, Approved }

enum class PaymentMethod { Pix, Card }

enum class CardType { Credit, Debit }

/**
 * Opções escolhidas no seletor de cartão antes de disparar o intent no Point Tap.
 * `installments` só é usado quando `type == Credit` (débito é sempre 1x).
 */
data class CardOptions(
    val type: CardType = CardType.Credit,
    val installments: Int = 1,
)

/** Estado do fluxo de cartão via Mercado Pago Point Tap. */
data class CardState(
    val options: CardOptions = CardOptions(),
    val stage: CardStage = CardStage.Idle,
    val orderId: String? = null,
    val paymentMethod: String? = null,
    val installments: Int? = null,
    val amountCents: Long? = null,
    val secondsRemaining: Int = 0,
)

sealed interface CardStage {
    /** Botão "Pagar com cartão" disponível. */
    data object Idle : CardStage

    /** Chamando `POST /api/payments`. */
    data object Creating : CardStage

    /** Intent OPEN — totem mostra animação NFC, aguarda aproximação do cartão. */
    data object Waiting : CardStage

    /** Status PROCESSING — cartão foi lido, gateway processando. */
    data object Processing : CardStage

    /** Status FINISHED — pagamento aprovado. */
    data object Success : CardStage

    /** Status CANCELED — vindo do gateway/maquininha. */
    data object Canceled : CardStage

    /** Status ERROR ou falha persistente de rede. */
    data class Failed(val message: String) : CardStage

    /** 120s sem status terminal. */
    data object Timeout : CardStage
}

data class PaymentApproved(val pedidoId: Long)
