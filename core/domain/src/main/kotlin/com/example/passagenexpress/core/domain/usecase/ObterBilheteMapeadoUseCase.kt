package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.BilheteMapeado
import com.example.passagenexpress.core.domain.repository.PedidoRepository
import javax.inject.Inject

class ObterBilheteMapeadoUseCase @Inject constructor(
    private val pedidoRepository: PedidoRepository,
) {
    suspend operator fun invoke(passageiroViagemId: Long): AppResult<BilheteMapeado> =
        pedidoRepository.obterBilheteMapeado(passageiroViagemId)
}
