package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.Comodo
import com.example.passagenexpress.core.domain.repository.ComodoRepository
import javax.inject.Inject

class ListarPoltronasUseCase @Inject constructor(
    private val comodoRepository: ComodoRepository,
) {
    suspend operator fun invoke(trechoId: Long, viagemId: Long): AppResult<Map<Long, List<Comodo>>> =
        comodoRepository.listarPoltronas(trechoId, viagemId)
}
