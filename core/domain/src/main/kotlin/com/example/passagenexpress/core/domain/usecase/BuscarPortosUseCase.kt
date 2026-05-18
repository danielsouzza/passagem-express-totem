package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.repository.ViagemRepository
import javax.inject.Inject

class BuscarPortosUseCase @Inject constructor(
    private val viagemRepository: ViagemRepository,
) {
    suspend operator fun invoke(): AppResult<List<Porto>> = viagemRepository.buscarPortos()
}
