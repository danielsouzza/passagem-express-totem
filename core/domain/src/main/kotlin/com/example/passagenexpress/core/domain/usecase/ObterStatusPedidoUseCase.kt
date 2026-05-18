package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.PedidoStatus
import com.example.passagenexpress.core.domain.repository.PedidoRepository
import javax.inject.Inject

/**
 * Polling-friendly: o web compara `orderStatus.status === "Pago"` em `/pedidos/{id}/status`,
 * que é o status do pedido (não do pagamento separadamente).
 */
class ObterStatusPedidoUseCase @Inject constructor(
    private val pedidoRepository: PedidoRepository,
) {
    suspend operator fun invoke(pedidoId: Long): AppResult<PedidoStatus> =
        pedidoRepository.obterStatus(pedidoId)
}
