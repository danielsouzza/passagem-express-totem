package com.example.passagenexpress.feature.idle

import com.example.passagenexpress.core.domain.model.AppLanguage

data class IdleUiState(
    val portoNome: String = "",
    val defaultLanguage: AppLanguage = AppLanguage.PtBr,
)
