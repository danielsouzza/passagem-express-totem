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
import com.example.passagenexpress.core.domain.repository.TotemConfigRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PedidoRepositoryImpl @Inject constructor(
    private val api: PedidoApi,
    private val totemConfigRepository: TotemConfigRepository,
) : PedidoRepository {

    override suspend fun criarPedido(input: NovoPedido): AppResult<Pedido> {
        // Origem da venda configurada no totem — mesmo papel do `...origemGuardada()` que o site
        // espalha em todo item do carrinho (`home.vue#handleInsertData`). Falha ao ler a config
        // não pode travar a venda: segue sem agente/qr.
        val config = runCatching { totemConfigRepository.config.first() }.getOrNull()
        val dto = input.toDto(
            agente = config?.agente?.ifBlank { null },
            qr = config?.origemQr?.ifBlank { null },
        )
        return callEnvelope({ api.criarPedido(dto) }) { it.toDomain() }
    }

    override suspend fun obterUltimoPedidoAberto(): AppResult<Pedido?> =
        callEnvelopeNullable({ api.obterUltimoAberto() }) { dto -> dto?.toDomain() }

    override suspend fun obterStatus(pedidoId: Long): AppResult<PedidoStatus> =
        callEnvelope({ api.obterStatus(pedidoId) }) { parsePedidoStatus(it.status) }

    override suspend fun obterPassagens(pedidoId: Long): AppResult<List<PassagemPedido>> =
        callEnvelope({ api.obterStatus(pedidoId) }) { it.toPassagens() }

    override suspend fun obterBilheteMapeado(passageiroViagemId: Long): AppResult<BilheteMapeado> =
        callRaw({ api.obterBilheteMapeado(passageiroViagemId) }) { it.toDomain() }
}
