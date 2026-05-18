package com.example.passagenexpress.core.domain.repository

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.ItemPedido
import com.example.passagenexpress.core.domain.model.Passageiro
import com.example.passagenexpress.core.domain.model.Pedido
import com.example.passagenexpress.core.domain.model.PedidoStatus

interface PedidoRepository {
    suspend fun criarPedido(input: NovoPedido): AppResult<Pedido>
    suspend fun obterUltimoPedidoAberto(): AppResult<Pedido?>
    suspend fun gerarPassagens(pedidoId: Long): AppResult<Pedido>
    suspend fun obterStatus(pedidoId: Long): AppResult<PedidoStatus>
}

data class NovoPedido(
    val pedidoId: Long? = null,
    val trechoId: Long,
    val viagemId: Long,
    val dataHora: String,
    val totalPassagens: Double,
    val totalTaxas: Double,
    val total: Double,
    val contato: Passageiro,
    val itens: List<ItemPedido>,
    /** 3 = canal totem (origem usada pelo backend) */
    val origem: Int = 3,
)
