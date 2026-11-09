package com.example.passagenexpress.core.domain.usecase.onibus

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.onibus.Terminal
import com.example.passagenexpress.core.domain.repository.OnibusRepository
import javax.inject.Inject

class BuscarTerminaisUseCase @Inject constructor(
    private val onibusRepository: OnibusRepository,
) {
    /** Busca com menos de [MinChars] caracteres devolve vazio sem chamar a API (igual ao site). */
    suspend operator fun invoke(search: String): AppResult<List<Terminal>> {
        val termo = search.trim()
        if (termo.length < MinChars) return AppResult.Success(emptyList())
        return onibusRepository.buscarTerminais(termo)
    }

    companion object {
        const val MinChars = 3
    }
}
