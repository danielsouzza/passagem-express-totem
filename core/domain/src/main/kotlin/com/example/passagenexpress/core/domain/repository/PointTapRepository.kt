package com.example.passagenexpress.core.domain.repository

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.NovaPointTapIntent
import com.example.passagenexpress.core.domain.model.PointTapIntent
import com.example.passagenexpress.core.domain.model.PointTapPaymentResult

interface PointTapRepository {
    suspend fun criarIntent(input: NovaPointTapIntent): AppResult<PointTapIntent>
    suspend fun obterStatus(paymentIntentId: String): AppResult<PointTapPaymentResult>
    suspend fun cancelarIntent(paymentIntentId: String): AppResult<Unit>
}
