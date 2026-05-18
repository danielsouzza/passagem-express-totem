package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.ComodoLivre
import com.example.passagenexpress.core.domain.repository.ComodoRepository
import javax.inject.Inject

class ListarComodosLivresUseCase @Inject constructor(
    private val comodoRepository: ComodoRepository,
) {
    suspend operator fun invoke(trechoId: Long, viagemId: Long): AppResult<List<ComodoLivre>> =
        comodoRepository.listarComodosLivres(trechoId, viagemId)
}
