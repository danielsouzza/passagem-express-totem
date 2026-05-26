package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.NovaPointTapOrder
import com.example.passagenexpress.core.domain.model.PointTapOrder
import com.example.passagenexpress.core.domain.repository.PointTapRepository
import javax.inject.Inject

class CriarPointTapOrderUseCase @Inject constructor(
    private val repository: PointTapRepository,
) {
    suspend operator fun invoke(input: NovaPointTapOrder): AppResult<PointTapOrder> =
        repository.criarOrder(input)
}
