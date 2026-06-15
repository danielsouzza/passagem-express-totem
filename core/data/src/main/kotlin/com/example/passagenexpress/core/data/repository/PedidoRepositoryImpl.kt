package com.example.passagenexpress.core.data.repository

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.data.remote.api.PedidoApi
import com.example.passagenexpress.core.data.remote.callEnvelope
import com.example.passagenexpress.core.data.remote.callEnvelopeNullable
import com.example.passagenexpress.core.data.remote.callRaw
import com.example.passagenexpress.core.data.remote.dto.parsePedidoStatus
import com.example.passagenexpress.core.data.remote.dto.toDomain
import com.example.passagenexpress.core.data.remote.dto.toDto
import com.example.passagenexpress.core.data.remote.dto.toPassagens
import com.example.passagenexpress.core.domain.model.BilheteMapeado
import com.example.passagenexpress.core.domain.model.PassagemPedido
import com.example.passagenexpress.core.domain.model.Pedido
import com.example.passagenexpress.core.domain.model.PedidoStatus
import com.example.passagenexpress.core.domain.repository.NovoPedido
import com.example.passagenexpress.core.domain.repository.PedidoRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PedidoRepositoryImpl @Inject constructor(
    private val api: PedidoApi,
) : PedidoRepository {

    override suspend fun criarPedido(input: NovoPedido): AppResult<Pedido> =
        callEnvelope({ api.criarPedido(input.toDto()) }) { it.toDomain() }

    override suspend fun obterUltimoPedidoAberto(): AppResult<Pedido?> =
        callEnvelopeNullable({ api.obterUltimoAberto() }) { dto -> dto?.toDomain() }

    override suspend fun obterStatus(pedidoId: Long): AppResult<PedidoStatus> =
        callEnvelope({ api.obterStatus(pedidoId) }) { parsePedidoStatus(it.status) }

    override suspend fun obterPassagens(pedidoId: Long): AppResult<List<PassagemPedido>> =
        callEnvelope({ api.obterStatus(pedidoId) }) { it.toPassagens() }

    override suspend fun obterBilheteMapeado(passageiroViagemId: Long): AppResult<BilheteMapeado> =
        callRaw({ api.obterBilheteMapeado(passageiroViagemId) }) { it.toDomain() }
}
