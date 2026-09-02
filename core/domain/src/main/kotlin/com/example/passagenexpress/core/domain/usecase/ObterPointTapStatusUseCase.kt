package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.PointTapPaymentResult
import com.example.passagenexpress.core.domain.repository.PointTapRepository
import javax.inject.Inject

class ObterPointTapStatusUseCase @Inject constructor(
    private val repository: PointTapRepository,
) {
    suspend operator fun invoke(pedidoId: Long): AppResult<PointTapPaymentResult> =
        repository.obterStatus(pedidoId)
}
