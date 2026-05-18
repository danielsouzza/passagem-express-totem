package com.example.passagenexpress.core.domain.model

data class TotemConfig(
    val subdomain: String,
    val portoId: Long,
    val portoSlug: String,
    val portoNome: String,
    val municipioCodigo: String,
    val municipioNome: String,
    val defaultLanguage: AppLanguage,
    val setupComplete: Boolean,
) {
    companion object {
        val Empty = TotemConfig(
            subdomain = "",
            portoId = 0L,
            portoSlug = "",
            portoNome = "",
            municipioCodigo = "",
            municipioNome = "",
            defaultLanguage = AppLanguage.PtBr,
            setupComplete = false,
        )
    }
}

enum class AppLanguage(val tag: String) {
    PtBr("pt-BR"),
    EnUs("en-US"),
}
