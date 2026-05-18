package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.BuscaViagensFiltros
import com.example.passagenexpress.core.domain.model.ResultadoBuscaViagens
import com.example.passagenexpress.core.domain.repository.ViagemRepository
import javax.inject.Inject

class BuscarViagensUseCase @Inject constructor(
    private val viagemRepository: ViagemRepository,
) {
    suspend operator fun invoke(filtros: BuscaViagensFiltros): AppResult<ResultadoBuscaViagens> =
        viagemRepository.buscarViagens(filtros)
}
