package com.example.passagenexpress.core.data.remote.api

import com.example.passagenexpress.core.data.remote.dto.TemaDto
import com.example.passagenexpress.core.network.envelope.ApiEnvelope
import retrofit2.http.GET

interface EmpresaApi {
    /** `subdomain` vai no query via `AuthInterceptor`, igual todo o resto da API. */
    @GET("api/empresa/tema")
    suspend fun getTema(): ApiEnvelope<TemaDto>
}
