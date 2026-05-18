package com.example.passagenexpress.core.data.remote.dto

import com.example.passagenexpress.core.domain.model.NovaPointTapIntent
import com.example.passagenexpress.core.domain.model.PointTapIntent
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
data class PointTapIntentRequestDto(
    @SerialName("pedido_id") val pedidoId: Long,
    val amount: Long,
    val description: String,
    val installments: Int,
    @SerialName("payment_type") val paymentType: String,
    @SerialName("external_reference") val externalReference: String,
)

@Serializable
data class PointTapIntentResponseDto(
    @SerialName("payment_intent_id") val paymentIntentId: String? = null,
    val id: String? = null,
    val status: String? = null,
)

@Serializable
data class PointTapStatusResponseDto(
    val status: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val installments: Int? = null,
    val amount: Long? = null,
)

fun PointTapPaymentType.toApiValue(): String = when (this) {
    PointTapPaymentType.CreditCard -> "credit_card"
    PointTapPaymentType.DebitCard -> "debit_card"
}

fun NovaPointTapIntent.toDto(): PointTapIntentRequestDto = PointTapIntentRequestDto(
    pedidoId = pedidoId,
    amount = amountCents,
    description = description,
    installments = installments,
    paymentType = paymentType.toApiValue(),
    externalReference = externalReference,
)

fun PointTapIntentResponseDto.toDomain(): PointTapIntent? {
    val id = paymentIntentId ?: id ?: return null
    return PointTapIntent(
        id = id,
        status = parsePointTapStatus(status),
    )
}

fun PointTapStatusResponseDto.toDomain(): PointTapPaymentResult = PointTapPaymentResult(
    status = parsePointTapStatus(status),
    paymentMethod = paymentMethod,
    installments = installments,
    amountCents = amount,
)

fun parsePointTapStatus(raw: String?): PointTapStatus = when (raw?.uppercase()) {
    "FINISHED" -> PointTapStatus.Finished
    "CANCELED", "CANCELLED" -> PointTapStatus.Canceled
    "ERROR" -> PointTapStatus.Error
    "PROCESSING" -> PointTapStatus.Processing
    else -> PointTapStatus.Open
}
