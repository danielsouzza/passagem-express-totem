package com.example.passagenexpress.core.common.result

sealed class AppError(open val message: String? = null, open val cause: Throwable? = null) {
    data class Network(override val message: String? = null, override val cause: Throwable? = null) : AppError(message, cause)
    data class Timeout(override val message: String? = null, override val cause: Throwable? = null) : AppError(message, cause)
    data class Server(val httpCode: Int, override val message: String? = null, override val cause: Throwable? = null) : AppError(message, cause)
    data class Unauthorized(override val message: String? = null) : AppError(message)
    data class NotFound(override val message: String? = null) : AppError(message)
    data class Validation(val fieldErrors: Map<String, String> = emptyMap(), override val message: String? = null) : AppError(message)
    data class Unknown(override val message: String? = null, override val cause: Throwable? = null) : AppError(message, cause)
}
