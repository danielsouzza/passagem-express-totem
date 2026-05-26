package com.example.passagenexpress.core.domain.model

/**
 * Order do Mercado Pago Point Tap (API nova). O app cria via backend próprio
 * (`POST /api/payments`) e faz polling em `/api/payments/{id}/status` até receber
 * um status terminal. IDs têm formato `ORD…` e são tratados como string opaca.
 */
data class PointTapOrder(
    val id: String,
    val status: PointTapStatus,
)

/**
 * Status do pagamento na maquininha. `Open` é o estado inicial (order criada,
 * aguardando aproximação do cartão); `Processing` aparece quando o cartão foi
 * lido e o gateway processa; os demais são terminais.
 */
enum class PointTapStatus {
    Open,
    Processing,
    Finished,
    Canceled,
    Error;

    val isTerminal: Boolean
        get() = this == Finished || this == Canceled || this == Error
}

/** Snapshot completo retornado pelo polling de status. */
data class PointTapPaymentResult(
    val status: PointTapStatus,
    val paymentMethod: String?,
    val installments: Int?,
    val amountCents: Long?,
)

/** Input para criar a order — `amount` em centavos, sem decimal. */
data class NovaPointTapOrder(
    val pedidoId: Long,
    val amountCents: Long,
    val description: String,
    val installments: Int,
    val paymentType: PointTapPaymentType,
    val externalReference: String,
)

enum class PointTapPaymentType { CreditCard, DebitCard }
