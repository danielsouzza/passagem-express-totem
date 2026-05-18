package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.repository.ComodoRepository
import javax.inject.Inject

class ReservarComodoUseCase @Inject constructor(
    private val comodoRepository: ComodoRepository,
) {
    suspend operator fun invoke(trechoId: Long, viagemId: Long, comodoId: Long): AppResult<Unit> =
        comodoRepository.reservarComodo(trechoId, viagemId, comodoId)
}
