package com.example.passagenexpress.core.domain.repository

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.Pagamento
import com.example.passagenexpress.core.domain.model.StatusPagamento
import com.example.passagenexpress.core.domain.model.TipoCartao

interface PagamentoRepository {
    suspend fun gerarPagamentoPix(pedidoId: Long, cpf: String, nome: String): AppResult<Pagamento.Pix>
    suspend fun gerarPagamentoCartao(pedidoId: Long, tipo: TipoCartao): AppResult<Pagamento.Cartao>
    suspend fun obterStatusPagamento(pedidoId: Long): AppResult<StatusPagamento>
}
