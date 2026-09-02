package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.repository.PointTapRepository
import javax.inject.Inject

/**
 * Reembolsa uma order já finalizada (`POST /api/point/refund/{orderId}`). Não é usado pelo
 * fluxo do totem (que não estorna no autoatendimento), mas o backend expõe a rota e mantemos
 * o plumbing pronto.
 */
class ReembolsarPointTapOrderUseCase @Inject constructor(
    private val repository: PointTapRepository,
) {
    suspend operator fun invoke(pedidoId: Long): AppResult<Unit> =
        repository.reembolsarOrder(pedidoId)
}
