package com.example.passagenexpress.core.network.safe

import com.example.passagenexpress.core.common.result.AppError
import com.example.passagenexpress.core.common.result.AppResult
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
    val code = e.code()
    val error = when (code) {
        401, 403 -> AppError.Unauthorized(message = e.message())
        404 -> AppError.NotFound(message = e.message())
        in 400..499 -> AppError.Validation(message = e.message())
        in 500..599 -> AppError.Server(httpCode = code, message = e.message(), cause = e)
        else -> AppError.Server(httpCode = code, message = e.message(), cause = e)
    }
    AppResult.Failure(error)
} catch (e: Exception) {
    AppResult.Failure(AppError.Unknown(message = e.message, cause = e))
}
