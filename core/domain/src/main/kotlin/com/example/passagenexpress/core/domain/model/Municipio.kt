package com.example.passagenexpress.core.domain.model

data class Municipio(
    val slug: String,
    val nome: String,
    val uf: String? = null,
)
