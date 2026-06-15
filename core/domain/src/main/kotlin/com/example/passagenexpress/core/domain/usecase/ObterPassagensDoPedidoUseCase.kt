package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.PassagemPedido
import com.example.passagenexpress.core.domain.repository.PedidoRepository
import javax.inject.Inject

/**
 * Passagens de um pedido já pago — vêm na resposta do endpoint de status (não há rota
 * `gerar-passagens`). Cada [PassagemPedido] carrega o `passageiro_viagem_id` usado pra
 * buscar e imprimir o bilhete.
 */
class ObterPassagensDoPedidoUseCase @Inject constructor(
    private val pedidoRepository: PedidoRepository,
) {
    suspend operator fun invoke(pedidoId: Long): AppResult<List<PassagemPedido>> =
        pedidoRepository.obterPassagens(pedidoId)
}
