package com.example.passagenexpress.core.data.repository

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.common.result.map
import com.example.passagenexpress.core.data.remote.api.ViagemApi
import com.example.passagenexpress.core.data.remote.callEnvelope
import com.example.passagenexpress.core.data.remote.dto.toDomain
import com.example.passagenexpress.core.network.safe.safeApiCall
import com.example.passagenexpress.core.domain.model.BuscaViagensFiltros
import com.example.passagenexpress.core.domain.model.Embarcacao
import com.example.passagenexpress.core.domain.model.Municipio
import com.example.passagenexpress.core.domain.model.Passageiro
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.model.ResultadoBuscaViagens
import com.example.passagenexpress.core.domain.model.TipoDocumento
import com.example.passagenexpress.core.domain.repository.ViagemRepository
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViagemRepositoryImpl @Inject constructor(
    private val api: ViagemApi,
) : ViagemRepository {

    // Portos e destinos são estáticos durante a sessão do totem. City e Trip pedem os mesmos
    // destinos (mesma GET /api/filtros), então cacheamos em memória os resultados bem-sucedidos
    // pra evitar rebusca a cada navegação. Só viagens (trechos) variam por data → nunca cacheadas.
    @Volatile
    private var portosCache: List<Porto>? = null
    @Volatile
    private var embarcacoesCache: List<Embarcacao>? = null
    private val destinosCache = ConcurrentHashMap<String, List<Municipio>>()

    override suspend fun buscarPortos(): AppResult<List<Porto>> {
        portosCache?.let { return AppResult.Success(it) }
        return callEnvelope({ api.getPortos() }) { dtos -> dtos.map { it.toDomain() } }
            .also { if (it is AppResult.Success) portosCache = it.value }
    }

    override suspend fun buscarEmbarcacoes(): AppResult<List<Embarcacao>> {
        embarcacoesCache?.let { return AppResult.Success(it) }
        return callEnvelope({ api.getEmbarcacoes() }) { dtos -> dtos.map { it.toDomain() } }
            .also { if (it is AppResult.Success) embarcacoesCache = it.value }
    }

    override suspend fun buscarMunicipiosOrigem(portoSlug: String?): AppResult<List<Municipio>> =
        callEnvelope({ api.getFiltros(portoSlug = portoSlug) }) { wrapper ->
            wrapper.municipiosOrigem.map { it.toDomain() }
        }

    override suspend fun buscarMunicipiosDestino(
        portoSlug: String?,
        municipioOrigemCodigo: String?,
    ): AppResult<List<Municipio>> {
        val cacheKey = "${portoSlug.orEmpty()}|${municipioOrigemCodigo.orEmpty()}"
        destinosCache[cacheKey]?.let { return AppResult.Success(it) }
        return callEnvelope({
            api.getFiltros(portoSlug = portoSlug, municipioOrigemCodigo = municipioOrigemCodigo)
        }) { wrapper ->
            wrapper.municipiosDestino.map { it.toDomain() }
        }.also { if (it is AppResult.Success) destinosCache[cacheKey] = it.value }
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
                embarcacaoId = filtros.embarcacaoId,
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
                embarcacaoId = filtros.embarcacaoId,
            )
        }) { wrapper ->
            wrapper.trechos.data.firstOrNull()?.toDomain()
        }

        return proximaResult.map { proxima ->
            firstPass.value.copy(proximaViagem = proxima)
        }
    }

    override suspend fun buscarPassageiro(tipo: TipoDocumento, doc: String): AppResult<Passageiro?> =
        // O mapeamento .toDomain() roda dentro do safeApiCall de propósito: se a API responder algo
        // fora do padrão, vira AppError em vez de derrubar o totem.
        safeApiCall {
            api.getPassageiro(tipo = tipo.id, doc = doc)
                ?.toDomain()
                ?.takeIf { it.nome.isNotBlank() }
        }
}
