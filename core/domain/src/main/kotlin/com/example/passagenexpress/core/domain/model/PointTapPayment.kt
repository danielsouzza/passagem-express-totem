package com.example.passagenexpress.core.domain.model

/**
 * Order do Mercado Pago Point Tap. O app cria via backend próprio (`POST /api/point/store`)
 * e faz polling em `/api/point/status/{id}` até o pedido ficar `Pago` ou `Negado`.
 * IDs têm formato `ORD…` e são tratados como string opaca.
 */
data class PointTapOrder(
    val id: String,
    val status: PointTapStatus,
)

/**
 * Status do pagamento, derivado do `status` do pedido no backend.
 * `Pending` cobre os estados não-terminais (`Solicitado`/`Em venda`/`OPEN`) — o totem segue
 * aguardando a aproximação do cartão; `Paid` e `Denied` são terminais.
 */
enum class PointTapStatus {
    Pending,
    Paid,
    Denied;

    val isTerminal: Boolean
        get() = this == Paid || this == Denied
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
