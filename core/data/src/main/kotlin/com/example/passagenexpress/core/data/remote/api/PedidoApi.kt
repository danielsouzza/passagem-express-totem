package com.example.passagenexpress.core.data.remote.api

import com.example.passagenexpress.core.data.remote.dto.BilheteMapeadoDto
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

    // Quando status == Pago, a resposta já traz `passagens_agrupadas` com os passageiro_viagem_id.
    @GET("api/pedidos/{id}/status")
    suspend fun obterStatus(@Path("id") pedidoId: Long): ApiEnvelope<StatusResponseDto>

    // Retorna o map do bilhete "cru" (sem o envelope {success,data,message}).
    @GET("api/passagens/{id}/bilhete-mapeado")
    suspend fun obterBilheteMapeado(
        @Path("id") passageiroViagemId: Long,
    ): BilheteMapeadoDto
}
