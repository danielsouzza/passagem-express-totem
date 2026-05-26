package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.repository.PointTapRepository
import javax.inject.Inject

class CancelarPointTapOrderUseCase @Inject constructor(
    private val repository: PointTapRepository,
) {
    suspend operator fun invoke(orderId: String): AppResult<Unit> =
        repository.cancelarOrder(orderId)
}
