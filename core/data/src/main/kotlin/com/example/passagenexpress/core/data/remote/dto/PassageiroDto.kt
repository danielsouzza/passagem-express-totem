package com.example.passagenexpress.core.data.remote.dto

import com.example.passagenexpress.core.data.remote.parse.parseLocalDate
import com.example.passagenexpress.core.domain.model.Passageiro
import com.example.passagenexpress.core.domain.model.TipoDocumento
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PassageiroDto(
    val nome: String? = null,
    val email: String? = null,
    val telefone: String? = null,
    val document: String? = null,
    @SerialName("tipo_doc") val tipoDoc: Int? = null,
    val nascimento: String? = null,
    @SerialName("data_nascimento") val dataNascimento: String? = null,
)

fun PassageiroDto.toDomain(): Passageiro = Passageiro(
    nome = nome.orEmpty(),
    cpf = document.orEmpty(),
    telefone = telefone.orEmpty(),
    email = email,
    dataNascimento = (dataNascimento ?: nascimento).parseLocalDate(),
    tipoDocumento = TipoDocumento.entries.firstOrNull { it.id == tipoDoc } ?: TipoDocumento.CPF,
)
