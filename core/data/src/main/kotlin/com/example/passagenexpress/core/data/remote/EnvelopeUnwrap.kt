package com.example.passagenexpress.core.data.remote

import com.example.passagenexpress.core.common.result.AppError
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.network.envelope.ApiEnvelope
import com.example.passagenexpress.core.network.safe.safeApiCall
import kotlinx.coroutines.CancellationException

/**
 * Executa o mapeamento DTO→domínio de forma segura. O [safeApiCall] só protege a chamada de rede
 * (incluindo a desserialização JSON feita pelo Retrofit); o `transform` roda **fora** dela. Como o
 * totem não pode fechar por causa de uma resposta fora do padrão (um campo inesperado, um enum
 * desconhecido, uma lista vazia onde se esperava dado), qualquer exceção do mapeamento é convertida
 * em [AppError.Unknown] em vez de derrubar o app.
 */
@PublishedApi
internal inline fun <R> safeMap(block: () -> R): AppResult<R> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    // Nunca engolir cancelamento de coroutine — precisa propagar.
    throw e
} catch (e: Throwable) {
    android.util.Log.e("EnvelopeUnwrap", "Falha ao mapear resposta da API para o domínio", e)
    AppResult.Failure(
        AppError.Unknown(message = "Resposta da API em formato inesperado.", cause = e),
    )
}

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
            else -> safeMap { transform(payload) }
        }
    }
    is AppResult.Failure -> result
}

/**
 * For endpoints that return the payload bare (sem o envelope `{success,data,message}`),
 * como `bilhete-mapeado`. Apenas mapeia o sucesso da rede; erros HTTP/IO viram [AppError].
 */
suspend inline fun <T, R> callRaw(
    crossinline block: suspend () -> T,
    crossinline transform: (T) -> R,
): AppResult<R> = when (val result = safeApiCall { block() }) {
    is AppResult.Success -> safeMap { transform(result.value) }
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
            safeMap { transform(envelope.data) }
        }
    }
    is AppResult.Failure -> result
}
