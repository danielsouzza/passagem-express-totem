package com.example.passagenexpress.core.data.repository

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.data.remote.api.ComodoApi
import com.example.passagenexpress.core.data.remote.callEnvelope
import com.example.passagenexpress.core.data.remote.dto.DeletarReservaRequestDto
import com.example.passagenexpress.core.data.remote.dto.IniciarVendaRequestDto
import com.example.passagenexpress.core.data.remote.dto.ReservarComodoRequestDto
import com.example.passagenexpress.core.data.remote.dto.toDomain
import com.example.passagenexpress.core.domain.model.Comodo
import com.example.passagenexpress.core.domain.model.ComodoLivre
import com.example.passagenexpress.core.domain.model.FormaPagamento
import com.example.passagenexpress.core.domain.repository.ComodoRepository
import com.example.passagenexpress.core.domain.repository.InicioVenda
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComodoRepositoryImpl @Inject constructor(
    private val api: ComodoApi,
) : ComodoRepository {

    override suspend fun listarPoltronas(trechoId: Long, viagemId: Long): AppResult<Map<Long, List<Comodo>>> =
        callEnvelope({ api.getPoltronas(trechoId, viagemId) }) { wrapper ->
            wrapper.byTipo.mapKeys { it.key.toLongOrNull() ?: 0L }
                .mapValues { entry -> entry.value.map { it.toDomain() } }
        }

    override suspend fun listarCamarotes(trechoId: Long, viagemId: Long): AppResult<Map<Long, List<Comodo>>> =
        callEnvelope({ api.getCamarotes(trechoId, viagemId) }) { wrapper ->
            wrapper.byTipo.mapKeys { it.key.toLongOrNull() ?: 0L }
                .mapValues { entry -> entry.value.map { it.toDomain() } }
        }

    override suspend fun listarComodosLivres(trechoId: Long, viagemId: Long): AppResult<List<ComodoLivre>> =
        callEnvelope({ api.getComodosLivres(trechoId, viagemId) }) { dtos ->
            dtos.map { it.toDomain() }
        }

    override suspend fun reservarComodo(trechoId: Long, viagemId: Long, comodoId: Long): AppResult<Unit> =
        callEnvelope({
            api.reservarComodo(ReservarComodoRequestDto(trechoId, viagemId, comodoId))
        }) { }

    override suspend fun deletarReserva(trechoId: Long, viagemId: Long, comodoIds: List<Long>): AppResult<Unit> =
        callEnvelope({
            api.deletarReserva(DeletarReservaRequestDto(trechoId, viagemId, comodoIds))
        }) { }

    override suspend fun iniciarVenda(
        trechoId: Long,
        viagemId: Long,
        tiposComodoEscolhidos: Map<Long, Int>,
        comodosAssentosEscolhidos: List<Long>,
    ): AppResult<InicioVenda> = callEnvelope({
        api.iniciarVenda(
            IniciarVendaRequestDto(
                trechoId = trechoId,
                viagemId = viagemId,
                tiposComodoEscolhidos = tiposComodoEscolhidos.mapKeys { it.key.toString() },
                comodosAssentosEscolhidos = comodosAssentosEscolhidos,
            )
        )
    }) { resp ->
        InicioVenda(
            trechoId = resp.data.trecho?.id ?: trechoId,
            comodos = resp.data.comodos.map { it.toDomain() },
            formasPagamento = resp.formasPagamento.map {
                FormaPagamento(id = it.id, codigo = it.codigo, nome = it.nome)
            },
        )
    }
}
