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
 * Backend wrapper do Mercado Pago Point Tap (API de orders). O app nunca fala direto com a
 * API do MP. Respostas vêm no envelope padrão do projeto `{success, data, message}`, inclusive
 * nas de erro (4xx/5xx) — daí o `httpExceptionToAppError` aproveita o `message`.
 *
 * `POST /api/payments` cria a order (body com `pedido_id`; data `{order_id, status: OPEN}`).
 * As demais rotas são chaveadas pelo **id do pedido** (não pelo id da order/intent): status,
 * cancel e refund usam `/api/payments/{pedidoId}/…`. O status devolve o **pedido** com `status`
 * em PT (`Pago`/`Negado`/`Solicitado`).
 */
interface PointTapApi {
    @POST("api/payments")
    suspend fun criarOrder(
        @Body body: PointTapOrderRequestDto,
    ): ApiEnvelope<PointTapOrderResponseDto>

    @GET("api/payments/{pedidoId}/status")
    suspend fun obterStatus(
        @Path("pedidoId") pedidoId: Long,
    ): ApiEnvelope<PointTapStatusResponseDto>

    @POST("api/payments/{pedidoId}/cancel")
    suspend fun cancelarOrder(
        @Path("pedidoId") pedidoId: Long,
        @Body body: Map<String, String> = emptyMap(),
    ): ApiEnvelope<Unit?>

    @POST("api/payments/{pedidoId}/refund")
    suspend fun refundOrder(
        @Path("pedidoId") pedidoId: Long,
        @Body body: Map<String, String> = emptyMap(),
    ): ApiEnvelope<Unit?>
}
