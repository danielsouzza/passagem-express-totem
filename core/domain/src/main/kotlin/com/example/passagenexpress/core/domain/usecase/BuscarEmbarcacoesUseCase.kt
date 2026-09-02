package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.Embarcacao
import com.example.passagenexpress.core.domain.repository.ViagemRepository
import javax.inject.Inject

class BuscarEmbarcacoesUseCase @Inject constructor(
    private val viagemRepository: ViagemRepository,
) {
    suspend operator fun invoke(): AppResult<List<Embarcacao>> = viagemRepository.buscarEmbarcacoes()
}
