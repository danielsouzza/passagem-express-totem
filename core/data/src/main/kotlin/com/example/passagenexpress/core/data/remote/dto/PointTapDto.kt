package com.example.passagenexpress.core.data.remote.dto

import com.example.passagenexpress.core.domain.model.NovaPointTapOrder
import com.example.passagenexpress.core.domain.model.PointTapOrder
import com.example.passagenexpress.core.domain.model.PointTapPaymentResult
import com.example.passagenexpress.core.domain.model.PointTapPaymentType
import com.example.passagenexpress.core.domain.model.PointTapStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Body do `POST /api/payments`. `amount` é integer em centavos (R$ 15,00 = 1500).
 * `pedido_id` é o vínculo principal com o backend de pedidos; `external_reference`
 * é repassado pro Mercado Pago e é uma string opaca.
 */
@Serializable
data class PointTapOrderRequestDto(
    @SerialName("pedido_id") val pedidoId: Long,
    val amount: Long,
    val description: String,
    val installments: Int,
    @SerialName("payment_type") val paymentType: String,
    @SerialName("external_reference") val externalReference: String,
)

/**
 * Resposta do `POST /api/payments`. `order_id` tem formato `ORD…` (string opaca) e `status`
 * vem `OPEN` (order recém-criada). O app trata o id como string qualquer.
 */
@Serializable
data class PointTapOrderResponseDto(
    @SerialName("order_id") val orderId: String? = null,
    val id: String? = null,
    val status: String? = null,
)

/**
 * Resposta do `GET /api/point/status/{orderId}`. O backend devolve o **pedido**: `status` em
 * português (`Pago`/`Negado`/`Solicitado`/`Em venda`) e `forma_pagamento` (ex.: "Cartão de
 * Crédito"). Os demais campos (`passagens_agrupadas` etc.) são ignorados aqui — a impressão
 * relê o status pago via `PedidoApi`.
 */
@Serializable
data class PointTapStatusResponseDto(
    val status: String? = null,
    @SerialName("forma_pagamento") val formaPagamento: String? = null,
    val total: Double? = null,
)

fun PointTapPaymentType.toApiValue(): String = when (this) {
    PointTapPaymentType.CreditCard -> "credit_card"
    PointTapPaymentType.DebitCard -> "debit_card"
}

fun NovaPointTapOrder.toDto(): PointTapOrderRequestDto = PointTapOrderRequestDto(
    pedidoId = pedidoId,
    amount = amountCents,
    description = description,
    installments = installments,
    paymentType = paymentType.toApiValue(),
    externalReference = externalReference,
)

fun PointTapOrderResponseDto.toDomain(): PointTapOrder? {
    val id = orderId ?: id ?: return null
    return PointTapOrder(
        id = id,
        status = parsePointTapStatus(status),
    )
}

fun PointTapStatusResponseDto.toDomain(): PointTapPaymentResult = PointTapPaymentResult(
    status = parsePointTapStatus(status),
    paymentMethod = formaPagamento,
    installments = null,
    amountCents = total?.let { (it * 100).toLong() },
)

/**
 * Normaliza o `status` do pedido (PT) para o domínio. `Pago` = aprovado; `Negado` = recusado;
 * qualquer outro (`Solicitado`, `Em venda`, `OPEN`, …) é tratado como ainda pendente — segue
 * aguardando no polling.
 */
fun parsePointTapStatus(raw: String?): PointTapStatus = when (raw?.trim()?.lowercase()) {
    "pago" -> PointTapStatus.Paid
    "negado" -> PointTapStatus.Denied
    else -> PointTapStatus.Pending
}
