package com.example.passagenexpress.core.network.envelope

import kotlinx.serialization.Serializable

/**
 * Backend wraps every response in `{ success, data, message }`. Most endpoints nest the actual
 * payload under `data` (sometimes one extra level deeper, e.g. trecho list returns `data.trechos.data`).
 */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = true,
    val data: T? = null,
    val message: String? = null,
)
