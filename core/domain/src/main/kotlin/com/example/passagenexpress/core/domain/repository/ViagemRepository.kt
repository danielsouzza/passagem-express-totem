package com.example.passagenexpress.core.domain.repository

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.BuscaViagensFiltros
import com.example.passagenexpress.core.domain.model.Embarcacao
import com.example.passagenexpress.core.domain.model.Municipio
import com.example.passagenexpress.core.domain.model.Passageiro
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.model.ResultadoBuscaViagens
import com.example.passagenexpress.core.domain.model.TipoDocumento

interface ViagemRepository {
    suspend fun buscarPortos(): AppResult<List<Porto>>
    suspend fun buscarEmbarcacoes(): AppResult<List<Embarcacao>>
    suspend fun buscarMunicipiosOrigem(portoSlug: String?): AppResult<List<Municipio>>
    suspend fun buscarMunicipiosDestino(
        portoSlug: String?,
        municipioOrigemCodigo: String?,
    ): AppResult<List<Municipio>>
    suspend fun buscarViagens(filtros: BuscaViagensFiltros): AppResult<ResultadoBuscaViagens>
    suspend fun buscarPassageiro(tipo: TipoDocumento, doc: String): AppResult<Passageiro?>
}
