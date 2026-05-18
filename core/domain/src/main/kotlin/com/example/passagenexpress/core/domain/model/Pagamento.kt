package com.example.passagenexpress.core.domain.model

import java.time.LocalDateTime

sealed interface Pagamento {
    val pedidoId: Long
    val valor: Double

    data class Pix(
        override val pedidoId: Long,
        override val valor: Double,
        val qrCodeBase64: String?,
        val copiaECola: String,
        val expiraEm: LocalDateTime?,
    ) : Pagamento

    data class Cartao(
        override val pedidoId: Long,
        override val valor: Double,
        val tipo: TipoCartao,
    ) : Pagamento
}

enum class TipoCartao { Credito, Debito }

enum class StatusPagamento { Pendente, Aprovado, Recusado, Expirado }

/** Forma de pagamento disponível para a venda, vinda do backend. */
data class FormaPagamento(
    val id: Long,
    val codigo: String,
    val nome: String,
)
