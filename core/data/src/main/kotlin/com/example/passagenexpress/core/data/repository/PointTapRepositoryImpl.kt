package com.example.passagenexpress.core.data.repository

import com.example.passagenexpress.core.common.result.AppError
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.data.remote.api.PointTapApi
import com.example.passagenexpress.core.data.remote.callEnvelope
import com.example.passagenexpress.core.data.remote.dto.toDomain
import com.example.passagenexpress.core.data.remote.dto.toDto
import com.example.passagenexpress.core.domain.model.NovaPointTapOrder
import com.example.passagenexpress.core.domain.model.PointTapOrder
import com.example.passagenexpress.core.domain.model.PointTapPaymentResult
import com.example.passagenexpress.core.domain.repository.PointTapRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PointTapRepositoryImpl @Inject constructor(
    private val api: PointTapApi,
) : PointTapRepository {

    override suspend fun criarOrder(input: NovaPointTapOrder): AppResult<PointTapOrder> =
        when (
            val r = callEnvelope({ api.criarOrder(input.toDto()) }) { it.toDomain() }
        ) {
            is AppResult.Success -> r.value
                ?.let { AppResult.Success(it) }
                ?: AppResult.Failure(AppError.Validation(message = "Resposta sem order_id"))
            is AppResult.Failure -> r
        }

    override suspend fun obterStatus(pedidoId: Long): AppResult<PointTapPaymentResult> =
        callEnvelope({ api.obterStatus(pedidoId) }) { it.toDomain() }

    override suspend fun cancelarOrder(pedidoId: Long): AppResult<Unit> =
        callEnvelope({ api.cancelarOrder(pedidoId) }) { }

    override suspend fun reembolsarOrder(pedidoId: Long): AppResult<Unit> =
        callEnvelope({ api.refundOrder(pedidoId) }) { }
}
