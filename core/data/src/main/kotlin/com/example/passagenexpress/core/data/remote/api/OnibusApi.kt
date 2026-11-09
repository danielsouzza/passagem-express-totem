package com.example.passagenexpress.core.data.remote.api

import com.example.passagenexpress.core.data.remote.dto.onibus.BloquearAssentosRequestDto
import com.example.passagenexpress.core.data.remote.dto.onibus.BoardingPassResponseDto
import com.example.passagenexpress.core.data.remote.dto.onibus.CriarPedidoOnibusRequestDto
import com.example.passagenexpress.core.data.remote.dto.onibus.DesbloquearAssentosRequestDto
import com.example.passagenexpress.core.data.remote.dto.onibus.PedidoOnibusResponseDto
import com.example.passagenexpress.core.data.remote.dto.onibus.PixOnibusRequestDto
import com.example.passagenexpress.core.data.remote.dto.onibus.PixOnibusResponseDto
import com.example.passagenexpress.core.data.remote.dto.onibus.TerminalDto
import com.example.passagenexpress.core.data.remote.dto.onibus.ViagemOnibusDto
import com.example.passagenexpress.core.data.remote.dto.onibus.ViagensOnibusResponseDto
import com.example.passagenexpress.core.network.envelope.ApiEnvelope
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints de ônibus (`api/onibus/...`) — mesma base, `X-API-KEY` e `?subdomain=` do resto
 * (AuthInterceptor). Contrato: `site-yjara/src/services/fetch.js` e `docs/api-onibus-news.md`.
 */
interface OnibusApi {
    @GET("api/onibus/terminais")
    suspend fun getTerminais(
        @Query("search") search: String,
        @Query("quantidade") quantidade: Int? = null,
    ): ApiEnvelope<List<TerminalDto>>

    @GET("api/onibus/viagens")
    suspend fun getViagens(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("departureDate") departureDate: String,
    ): ApiEnvelope<ViagensOnibusResponseDto>

    @GET("api/onibus/viagens/{tripUuid}/trecho")
    suspend fun getTrechoDaPerna(@Path("tripUuid") tripUuid: String): ApiEnvelope<ViagemOnibusDto>

    /** `data` = `{"1": [assentos]}` — lido como JsonElement (mapa PHP). */
    @GET("api/onibus/viagens/{tripUuid}")
    suspend fun getAssentos(@Path("tripUuid") tripUuid: String): ApiEnvelope<JsonElement>

    /** `data` = `{ data: { comodos, taxa_embarque, trecho }, formas_pagamento }`. */
    @POST("api/onibus/reservas/bloquear-assentos")
    suspend fun bloquearAssentos(@Body body: BloquearAssentosRequestDto): ApiEnvelope<JsonElement>

    @HTTP(method = "DELETE", path = "api/onibus/reservas/desbloquear-assentos", hasBody = true)
    suspend fun desbloquearAssentos(@Body body: DesbloquearAssentosRequestDto): ApiEnvelope<JsonElement?>

    @POST("api/onibus/pedidos")
    suspend fun criarPedido(@Body body: CriarPedidoOnibusRequestDto): ApiEnvelope<PedidoOnibusResponseDto>

    @POST("api/onibus/pagamentos/pix")
    suspend fun gerarPix(@Body body: PixOnibusRequestDto): ApiEnvelope<PixOnibusResponseDto>

    @GET("api/onibus/pedidos/{pedidoId}/boarding-pass")
    suspend fun getBoardingPass(@Path("pedidoId") pedidoId: Long): ApiEnvelope<BoardingPassResponseDto>
}
