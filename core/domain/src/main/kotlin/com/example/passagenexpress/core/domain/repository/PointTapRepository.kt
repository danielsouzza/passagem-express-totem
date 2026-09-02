package com.example.passagenexpress.core.domain.repository

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.NovaPointTapOrder
import com.example.passagenexpress.core.domain.model.PointTapOrder
import com.example.passagenexpress.core.domain.model.PointTapPaymentResult

interface PointTapRepository {
    suspend fun criarOrder(input: NovaPointTapOrder): AppResult<PointTapOrder>
    // status/cancel/refund são chaveados pelo id do PEDIDO (não pelo id da order/intent).
    suspend fun obterStatus(pedidoId: Long): AppResult<PointTapPaymentResult>
    suspend fun cancelarOrder(pedidoId: Long): AppResult<Unit>
    suspend fun reembolsarOrder(pedidoId: Long): AppResult<Unit>
}
