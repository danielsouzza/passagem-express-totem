package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.Pagamento
import com.example.passagenexpress.core.domain.repository.PagamentoRepository
import javax.inject.Inject

class GerarPagamentoPixUseCase @Inject constructor(
    private val pagamentoRepository: PagamentoRepository,
) {
    suspend operator fun invoke(
        pedidoId: Long,
        cpf: String,
        nome: String,
    ): AppResult<Pagamento.Pix> = pagamentoRepository.gerarPagamentoPix(pedidoId, cpf, nome)
}
