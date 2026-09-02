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

/** Tipos de documento aceitos (o `id` é o código do backend — ver tiposDoc no app web). */
enum class TipoDocumento(val id: Int) {
    CPF(5),
    RG(1),
    TituloEleitor(2),
    Passaporte(3),
    CNH(4),
}
