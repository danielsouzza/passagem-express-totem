package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.repository.ComodoRepository
import com.example.passagenexpress.core.domain.repository.InicioVenda
import javax.inject.Inject

class IniciarVendaUseCase @Inject constructor(
    private val comodoRepository: ComodoRepository,
) {
    suspend operator fun invoke(
        trechoId: Long,
        viagemId: Long,
        tiposComodoEscolhidos: Map<Long, Int>,
        comodosAssentosEscolhidos: List<Long>,
    ): AppResult<InicioVenda> = comodoRepository.iniciarVenda(
        trechoId = trechoId,
        viagemId = viagemId,
        tiposComodoEscolhidos = tiposComodoEscolhidos,
        comodosAssentosEscolhidos = comodosAssentosEscolhidos,
    )
}
