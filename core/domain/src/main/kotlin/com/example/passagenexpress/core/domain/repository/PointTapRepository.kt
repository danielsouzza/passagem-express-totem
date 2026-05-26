package com.example.passagenexpress.core.domain.repository

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.NovaPointTapOrder
import com.example.passagenexpress.core.domain.model.PointTapOrder
import com.example.passagenexpress.core.domain.model.PointTapPaymentResult

interface PointTapRepository {
    suspend fun criarOrder(input: NovaPointTapOrder): AppResult<PointTapOrder>
    suspend fun obterStatus(orderId: String): AppResult<PointTapPaymentResult>
    suspend fun cancelarOrder(orderId: String): AppResult<Unit>
}
