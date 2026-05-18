package com.example.passagenexpress.core.domain.model

import java.time.LocalDate

data class Passageiro(
    val nome: String,
    val cpf: String,
    val telefone: String,
    val email: String? = null,
    val dataNascimento: LocalDate?,
    val tipoDocumento: TipoDocumento = TipoDocumento.CPF,
)

enum class TipoDocumento(val id: Int) {
    CPF(5),
    RG(1),
}
