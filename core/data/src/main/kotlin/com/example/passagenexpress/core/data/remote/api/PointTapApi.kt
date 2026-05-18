package com.example.passagenexpress.core.data.remote.api

import com.example.passagenexpress.core.data.remote.dto.PointTapIntentRequestDto
import com.example.passagenexpress.core.data.remote.dto.PointTapIntentResponseDto
import com.example.passagenexpress.core.data.remote.dto.PointTapStatusResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Backend wrapper do Mercado Pago Point Tap. O app nunca fala direto com a API do MP —
 * tudo passa por esses 3 endpoints.
 */
interface PointTapApi {
    @POST("api/payments")
    suspend fun criarIntent(@Body body: PointTapIntentRequestDto): PointTapIntentResponseDto

    @GET("api/payments/{paymentIntentId}/status")
    suspend fun obterStatus(@Path("paymentIntentId") paymentIntentId: String): PointTapStatusResponseDto

    @DELETE("api/payments/{paymentIntentId}")
    suspend fun cancelarIntent(@Path("paymentIntentId") paymentIntentId: String)
}
