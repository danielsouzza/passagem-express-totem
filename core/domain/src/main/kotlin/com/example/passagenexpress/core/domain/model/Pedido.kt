package com.example.passagenexpress.core.domain.model

import java.time.LocalDateTime

data class Pedido(
    val id: Long,
    val status: PedidoStatus,
    val totalPassagens: Double,
    val totalTaxas: Double,
    val total: Double,
    val criadoEm: LocalDateTime,
    val itens: List<ItemPedido> = emptyList(),
)

enum class PedidoStatus { Aberto, Pago, Cancelado, Expirado }

data class ItemPedido(
    val comodoId: Long,
    val tipoComodidadeId: Long,
    val passageiro: Passageiro,
    val valor: Double,
    val taxaEmbarque: Double,
    val descontoId: Long? = null,
    val isContact: Boolean = false,
    val comodoRelacionado: Long? = null,
    val comodosFilhos: Int = 1,
)
