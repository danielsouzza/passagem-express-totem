package com.example.passagenexpress.core.data.remote.api

import com.example.passagenexpress.core.data.remote.dto.CriarPedidoRequestDto
import com.example.passagenexpress.core.data.remote.dto.PedidoDto
import com.example.passagenexpress.core.data.remote.dto.StatusResponseDto
import com.example.passagenexpress.core.network.envelope.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PedidoApi {
    @POST("api/pedidos")
    suspend fun criarPedido(@Body body: CriarPedidoRequestDto): ApiEnvelope<PedidoDto>

    @GET("api/pedidos/ultimo-aberto/dados")
    suspend fun obterUltimoAberto(): ApiEnvelope<PedidoDto?>

    @POST("api/pedidos/{id}/gerar-passagens")
    suspend fun gerarPassagens(@Path("id") pedidoId: Long): ApiEnvelope<PedidoDto>

    @GET("api/pedidos/{id}/status")
    suspend fun obterStatus(@Path("id") pedidoId: Long): ApiEnvelope<StatusResponseDto>
}
