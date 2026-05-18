package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.NovaPointTapIntent
import com.example.passagenexpress.core.domain.model.PointTapIntent
import com.example.passagenexpress.core.domain.repository.PointTapRepository
import javax.inject.Inject

class CriarPointTapIntentUseCase @Inject constructor(
    private val repository: PointTapRepository,
) {
    suspend operator fun invoke(input: NovaPointTapIntent): AppResult<PointTapIntent> =
        repository.criarIntent(input)
}
