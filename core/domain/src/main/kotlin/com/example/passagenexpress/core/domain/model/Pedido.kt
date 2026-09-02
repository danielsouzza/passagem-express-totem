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
    /** Capacidade física do camarote (no pivô); 1 nos filhos e em cômodos simples. */
    val comodosFilhos: Int = 1,
    /** Ocupantes efetivos do camarote (no pivô); 1 nos filhos e em cômodos simples. */
    val qtdComodosFilhos: Int = 1,
)
