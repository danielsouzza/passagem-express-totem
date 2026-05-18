package com.example.passagenexpress.core.data.repository

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.common.result.map
import com.example.passagenexpress.core.data.remote.api.ViagemApi
import com.example.passagenexpress.core.data.remote.callEnvelope
import com.example.passagenexpress.core.data.remote.dto.toDomain
import com.example.passagenexpress.core.network.safe.safeApiCall
import com.example.passagenexpress.core.domain.model.BuscaViagensFiltros
import com.example.passagenexpress.core.domain.model.Municipio
import com.example.passagenexpress.core.domain.model.Passageiro
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.model.ResultadoBuscaViagens
import com.example.passagenexpress.core.domain.model.TipoDocumento
import com.example.passagenexpress.core.domain.repository.ViagemRepository
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViagemRepositoryImpl @Inject constructor(
    private val api: ViagemApi,
) : ViagemRepository {

    override suspend fun buscarPortos(): AppResult<List<Porto>> =
        callEnvelope({ api.getPortos() }) { dtos -> dtos.map { it.toDomain() } }

    override suspend fun buscarMunicipiosOrigem(portoSlug: String?): AppResult<List<Municipio>> =
        callEnvelope({ api.getFiltros(portoSlug = portoSlug) }) { wrapper ->
            wrapper.municipiosOrigem.map { it.toDomain() }
        }

    override suspend fun buscarMunicipiosDestino(
        portoSlug: String?,
        municipioOrigemCodigo: String?,
    ): AppResult<List<Municipio>> =
        callEnvelope({
            api.getFiltros(portoSlug = portoSlug, municipioOrigemCodigo = municipioOrigemCodigo)
        }) { wrapper ->
            wrapper.municipiosDestino.map { it.toDomain() }
        }

    override suspend fun buscarViagens(filtros: BuscaViagensFiltros): AppResult<ResultadoBuscaViagens> {
        val firstPass = callEnvelope({
            api.getTrechosViagem(
                portoSlug = filtros.portoSlug,
                municipioOrigemCodigo = filtros.municipioOrigemCodigo,
                destino = filtros.municipioDestinoSlug,
                dataHora = filtros.data?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                quantia = filtros.quantia,
                dataIrrestrita = if (filtros.dataIrrestrita) 1 else null,
            )
        }) { wrapper ->
            ResultadoBuscaViagens(
                trechos = wrapper.trechos.data.map { it.toDomain() },
                total = wrapper.trechos.total,
            )
        }

        if (firstPass !is AppResult.Success || firstPass.value.trechos.isNotEmpty() || filtros.dataIrrestrita) {
            return firstPass
        }

        val proximaResult = callEnvelope({
            api.getTrechosViagem(
                portoSlug = filtros.portoSlug,
                municipioOrigemCodigo = filtros.municipioOrigemCodigo,
                destino = filtros.municipioDestinoSlug,
                dataHora = filtros.data?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                quantia = 1,
                dataIrrestrita = 1,
            )
        }) { wrapper ->
            wrapper.trechos.data.firstOrNull()?.toDomain()
        }

        return proximaResult.map { proxima ->
            firstPass.value.copy(proximaViagem = proxima)
        }
    }

    override suspend fun buscarPassageiro(tipo: TipoDocumento, doc: String): AppResult<Passageiro?> =
        when (val result = safeApiCall { api.getPassageiro(tipo = tipo.id, doc = doc) }) {
            is AppResult.Success -> {
                val dto = result.value
                val passageiro = dto?.toDomain()?.takeIf { it.nome.isNotBlank() }
                AppResult.Success(passageiro)
            }
            is AppResult.Failure -> result
        }
}
