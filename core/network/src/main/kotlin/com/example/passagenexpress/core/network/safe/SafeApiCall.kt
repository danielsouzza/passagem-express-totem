package com.example.passagenexpress.core.network.safe

import com.example.passagenexpress.core.common.result.AppError
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.network.json.NetworkJson
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: SocketTimeoutException) {
    AppResult.Failure(AppError.Timeout(message = e.message, cause = e))
} catch (e: IOException) {
    AppResult.Failure(AppError.Network(message = e.message, cause = e))
} catch (e: HttpException) {
    AppResult.Failure(httpExceptionToAppError(e))
} catch (e: Exception) {
    AppResult.Failure(AppError.Unknown(message = e.message, cause = e))
}

/**
 * Mapeia um [HttpException] para [AppError] **preservando a mensagem do backend**. O backend
 * envolve as respostas (inclusive as de erro 4xx/5xx) em `{ success, message, data }`, então a
 * frase genérica do HTTP ("Bad Request", "Conflict") não diz nada ao usuário. Aqui lemos o corpo
 * do erro e usamos o `message` do envelope; só caímos no `e.message()` quando o parse falha.
 */
fun httpExceptionToAppError(e: HttpException): AppError {
    val code = e.code()
    val msg = parseBackendMessage(e) ?: e.message()
    return when (code) {
        401, 403 -> AppError.Unauthorized(message = msg)
        404 -> AppError.NotFound(message = msg)
        in 400..499 -> AppError.Validation(message = msg)
        else -> AppError.Server(httpCode = code, message = msg, cause = e)
    }
}

private fun parseBackendMessage(e: HttpException): String? = try {
    val raw = e.response()?.errorBody()?.string()?.takeIf { it.isNotBlank() }
    raw?.let { NetworkJson.decodeFromString(BackendErrorBody.serializer(), it).message }
        ?.takeIf { it.isNotBlank() }
} catch (_: Exception) {
    null
}

@Serializable
private data class BackendErrorBody(val message: String? = null)