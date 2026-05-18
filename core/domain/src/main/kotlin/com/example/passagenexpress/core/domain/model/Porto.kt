package com.example.passagenexpress.core.domain.model

data class Porto(
    val id: Long,
    val slug: String,
    val nome: String,
    val municipioCodigo: String,
    val municipioNome: String,
)
