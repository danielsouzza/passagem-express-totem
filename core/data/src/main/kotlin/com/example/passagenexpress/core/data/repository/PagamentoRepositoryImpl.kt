package com.example.passagenexpress.core.data.repository

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.common.result.map
import com.example.passagenexpress.core.data.remote.api.PagamentoApi
import com.example.passagenexpress.core.data.remote.api.PedidoApi
import com.example.passagenexpress.core.data.remote.callEnvelope
import com.example.passagenexpress.core.data.remote.dto.PagamentoCartaoRequestDto
import com.example.passagenexpress.core.data.remote.dto.PagamentoPixRequestDto
import com.example.passagenexpress.core.data.remote.dto.parseStatusPagamento
import com.example.passagenexpress.core.data.remote.dto.toApiValue
import com.example.passagenexpress.core.data.remote.dto.toDomain
import com.example.passagenexpress.core.domain.model.Pagamento
import com.example.passagenexpress.core.domain.model.StatusPagamento
import com.example.passagenexpress.core.domain.model.TipoCartao
import com.example.passagenexpress.core.domain.repository.PagamentoRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PagamentoRepositoryImpl @Inject constructor(
    private val pagamentoApi: PagamentoApi,
    private val pedidoApi: PedidoApi,
) : PagamentoRepository {

    override suspend fun gerarPagamentoPix(
        pedidoId: Long,
        cpf: String,
        nome: String,
    ): AppResult<Pagamento.Pix> =
        callEnvelope({
            pagamentoApi.criarPix(PagamentoPixRequestDto(pedidoId = pedidoId, cpf = cpf, nome = nome))
        }) { resp -> resp.toDomain(fallbackPedidoId = pedidoId) }

    override suspend fun gerarPagamentoCartao(pedidoId: Long, tipo: TipoCartao): AppResult<Pagamento.Cartao> =
        callEnvelope({
            pagamentoApi.criarCredito(
                PagamentoCartaoRequestDto(pedidoId = pedidoId, tipo = tipo.toApiValue())
            )
        }) {
            Pagamento.Cartao(pedidoId = pedidoId, valor = 0.0, tipo = tipo)
        }

    override suspend fun obterStatusPagamento(pedidoId: Long): AppResult<StatusPagamento> =
        callEnvelope({ pedidoApi.obterStatus(pedidoId) }) { parseStatusPagamento(it.status) }
            .map { it }
}
