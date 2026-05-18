package com.example.passagenexpress.core.data.repository

import com.example.passagenexpress.core.common.result.AppError
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.common.result.map
import com.example.passagenexpress.core.data.remote.api.PointTapApi
import com.example.passagenexpress.core.data.remote.dto.toDomain
import com.example.passagenexpress.core.data.remote.dto.toDto
import com.example.passagenexpress.core.domain.model.NovaPointTapIntent
import com.example.passagenexpress.core.domain.model.PointTapIntent
import com.example.passagenexpress.core.domain.model.PointTapPaymentResult
import com.example.passagenexpress.core.domain.repository.PointTapRepository
import com.example.passagenexpress.core.network.safe.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PointTapRepositoryImpl @Inject constructor(
    private val api: PointTapApi,
) : PointTapRepository {

    override suspend fun criarIntent(input: NovaPointTapIntent): AppResult<PointTapIntent> =
        when (val r = safeApiCall { api.criarIntent(input.toDto()) }) {
            is AppResult.Success -> r.value.toDomain()
                ?.let { AppResult.Success(it) }
                ?: AppResult.Failure(AppError.Validation(message = "Resposta sem payment_intent_id"))
            is AppResult.Failure -> r
        }

    override suspend fun obterStatus(paymentIntentId: String): AppResult<PointTapPaymentResult> =
        safeApiCall { api.obterStatus(paymentIntentId) }.map { it.toDomain() }

    override suspend fun cancelarIntent(paymentIntentId: String): AppResult<Unit> =
        safeApiCall { api.cancelarIntent(paymentIntentId) }
}
