package com.example.passagenexpress.core.data.remote.api

import com.example.passagenexpress.core.data.remote.dto.ComodoLivreDto
import com.example.passagenexpress.core.data.remote.dto.ComodosByTipoDto
import com.example.passagenexpress.core.data.remote.dto.DeletarReservaRequestDto
import com.example.passagenexpress.core.data.remote.dto.IniciarVendaRequestDto
import com.example.passagenexpress.core.data.remote.dto.IniciarVendaResponseDto
import com.example.passagenexpress.core.data.remote.dto.ReservarComodoRequestDto
import com.example.passagenexpress.core.network.envelope.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Query

interface ComodoApi {
    @GET("api/comodos/poltronas")
    suspend fun getPoltronas(
        @Query("trecho_id") trechoId: Long,
        @Query("viagem_id") viagemId: Long,
    ): ApiEnvelope<ComodosByTipoDto>

    @GET("api/comodos/camarotes")
    suspend fun getCamarotes(
        @Query("trecho_id") trechoId: Long,
        @Query("viagem_id") viagemId: Long,
    ): ApiEnvelope<ComodosByTipoDto>

    @GET("api/comodos/livres-por-tipo")
    suspend fun getComodosLivres(
        @Query("trecho_id") trechoId: Long,
        @Query("viagem_id") viagemId: Long,
    ): ApiEnvelope<List<ComodoLivreDto>>

    @POST("api/reservas/comodo")
    suspend fun reservarComodo(@Body body: ReservarComodoRequestDto): ApiEnvelope<Unit?>

    @HTTP(method = "DELETE", path = "api/reservas/comodo", hasBody = true)
    suspend fun deletarReserva(@Body body: DeletarReservaRequestDto): ApiEnvelope<Unit?>

    @POST("api/reservas/comecar-venda")
    suspend fun iniciarVenda(@Body body: IniciarVendaRequestDto): ApiEnvelope<IniciarVendaResponseDto>
}
