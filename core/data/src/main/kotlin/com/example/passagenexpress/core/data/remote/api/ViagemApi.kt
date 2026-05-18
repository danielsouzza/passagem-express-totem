package com.example.passagenexpress.core.data.remote.api

import com.example.passagenexpress.core.data.remote.dto.FiltrosResponseDto
import com.example.passagenexpress.core.data.remote.dto.PassageiroDto
import com.example.passagenexpress.core.data.remote.dto.PortoDto
import com.example.passagenexpress.core.data.remote.dto.TrechosViagemResponseDto
import com.example.passagenexpress.core.network.envelope.ApiEnvelope
import retrofit2.http.GET
import retrofit2.http.Query

interface ViagemApi {
    @GET("api/filtros/portos")
    suspend fun getPortos(): ApiEnvelope<List<PortoDto>>

    @GET("api/filtros")
    suspend fun getFiltros(
        @Query("porto_id") portoSlug: String? = null,
        @Query("municipio_origem") municipioOrigemCodigo: String? = null,
    ): ApiEnvelope<FiltrosResponseDto>

    @GET("api/trechos-viagem")
    suspend fun getTrechosViagem(
        @Query("porto") portoSlug: String? = null,
        @Query("municipio_origem") municipioOrigemCodigo: String? = null,
        @Query("destino") destino: String? = null,
        @Query("data_hora") dataHora: String? = null,
        @Query("quantia") quantia: Int = 10,
        @Query("data_irrestrita") dataIrrestrita: Int? = null,
    ): ApiEnvelope<TrechosViagemResponseDto>

    /**
     * **Atenção:** ao contrário dos demais endpoints, este retorna o `PassageiroDto`
     * direto no root da resposta — sem `{success, data, message}`. Veja `FormPassenger.vue`
     * acessando `resp.data.nome` (uma camada), contra `resp.data.data.x` (duas camadas)
     * dos outros calls da `ViagemService`.
     */
    @GET("api/filtros/get-passageiro")
    suspend fun getPassageiro(
        @Query("tipo") tipo: Int,
        @Query("doc") doc: String,
    ): PassageiroDto?
}
