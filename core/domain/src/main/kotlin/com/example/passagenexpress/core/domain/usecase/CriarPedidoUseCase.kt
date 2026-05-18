package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.Pedido
import com.example.passagenexpress.core.domain.repository.NovoPedido
import com.example.passagenexpress.core.domain.repository.PedidoRepository
import javax.inject.Inject

class CriarPedidoUseCase @Inject constructor(
    private val pedidoRepository: PedidoRepository,
) {
    suspend operator fun invoke(input: NovoPedido): AppResult<Pedido> =
        pedidoRepository.criarPedido(input)
}
