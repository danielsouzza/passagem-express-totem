package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.repository.ComodoRepository
import javax.inject.Inject

class DeletarReservaUseCase @Inject constructor(
    private val comodoRepository: ComodoRepository,
) {
    suspend operator fun invoke(
        trechoId: Long,
        viagemId: Long,
        comodoIds: List<Long>,
    ): AppResult<Unit> = comodoRepository.deletarReserva(trechoId, viagemId, comodoIds)
}
