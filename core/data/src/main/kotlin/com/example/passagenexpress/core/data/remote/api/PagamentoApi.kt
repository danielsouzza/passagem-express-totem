package com.example.passagenexpress.core.data.remote.api

import com.example.passagenexpress.core.data.remote.dto.PagamentoCartaoRequestDto
import com.example.passagenexpress.core.data.remote.dto.PagamentoPixRequestDto
import com.example.passagenexpress.core.data.remote.dto.PagamentoPixResponseDto
import com.example.passagenexpress.core.data.remote.dto.StatusPagamentoResponseDto
import com.example.passagenexpress.core.network.envelope.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.POST

interface PagamentoApi {
    @POST("api/pagamentos/pix")
    suspend fun criarPix(@Body body: PagamentoPixRequestDto): ApiEnvelope<PagamentoPixResponseDto>

    @POST("api/pagamentos/credito")
    suspend fun criarCredito(@Body body: PagamentoCartaoRequestDto): ApiEnvelope<Unit?>
}
