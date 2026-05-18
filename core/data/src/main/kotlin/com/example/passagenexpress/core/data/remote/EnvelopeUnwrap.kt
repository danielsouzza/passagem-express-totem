package com.example.passagenexpress.core.data.remote

import com.example.passagenexpress.core.common.result.AppError
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.network.envelope.ApiEnvelope
import com.example.passagenexpress.core.network.safe.safeApiCall

/**
 * Wrap a Retrofit call returning [ApiEnvelope] into an [AppResult].
 * Treats `success=false` as a Validation error using the envelope message.
 */
suspend inline fun <T, R> callEnvelope(
    crossinline block: suspend () -> ApiEnvelope<T>,
    crossinline transform: (T) -> R,
): AppResult<R> = when (val result = safeApiCall { block() }) {
    is AppResult.Success -> {
        val envelope = result.value
        val payload = envelope.data
        when {
            !envelope.success -> AppResult.Failure(AppError.Validation(message = envelope.message))
            payload == null -> AppResult.Failure(AppError.NotFound(message = envelope.message))
            else -> AppResult.Success(transform(payload))
        }
    }
    is AppResult.Failure -> result
}

/** For endpoints whose payload may be legitimately null/absent (e.g. obter-ultimo-aberto). */
suspend inline fun <T, R> callEnvelopeNullable(
    crossinline block: suspend () -> ApiEnvelope<T?>,
    crossinline transform: (T?) -> R,
): AppResult<R> = when (val result = safeApiCall { block() }) {
    is AppResult.Success -> {
        val envelope = result.value
        if (!envelope.success) {
            AppResult.Failure(AppError.Validation(message = envelope.message))
        } else {
            AppResult.Success(transform(envelope.data))
        }
    }
    is AppResult.Failure -> result
}
