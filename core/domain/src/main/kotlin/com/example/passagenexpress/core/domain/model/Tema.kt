package com.example.passagenexpress.core.domain.model

/** Espelha `api/empresa/tema`. */
data class Tema(
    /** Empresa/agência "super" usa meios de pagamento próprios — reflete em `venda_super`. */
    val isSuper: Boolean,
)
