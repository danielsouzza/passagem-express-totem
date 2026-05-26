package com.example.passagenexpress.core.data.remote.api

import com.example.passagenexpress.core.data.remote.dto.PointTapOrderRequestDto
import com.example.passagenexpress.core.data.remote.dto.PointTapOrderResponseDto
import com.example.passagenexpress.core.data.remote.dto.PointTapStatusResponseDto
import com.example.passagenexpress.core.network.envelope.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Backend wrapper do Mercado Pago Point Tap (nova API de orders). O app nunca fala
 * direto com a API do MP — tudo passa por esses 3 endpoints. Respostas vêm no envelope
 * padrão do projeto `{success, data, message}`.
 *
 * IDs de order têm formato `ORD…` (string opaca, comprimento variável).
 */
interface PointTapApi {
    @POST("api/payments")
    suspend fun criarOrder(
        @Body body: PointTapOrderRequestDto,
    ): ApiEnvelope<PointTapOrderResponseDto>

    @GET("api/payments/{orderId}/status")
    suspend fun obterStatus(
        @Path("orderId") orderId: String,
    ): ApiEnvelope<PointTapStatusResponseDto>

    @POST("api/payments/{orderId}/cancel")
    suspend fun cancelarOrder(
        @Path("orderId") orderId: String,
        @Body body: Map<String, String> = emptyMap(),
    ): ApiEnvelope<Unit?>
}
