package com.example.passagenexpress.core.data.remote.dto

import com.example.passagenexpress.core.domain.model.Embarcacao
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmbarcacaoDto(
    val id: Long,
    val nome: String,
    val empresa: EmbarcacaoEmpresaDto? = null,
)

@Serializable
data class EmbarcacaoEmpresaDto(
    @SerialName("nome_fantasia") val nomeFantasia: String? = null,
    @SerialName("xnome") val xnome: String? = null,
)

fun EmbarcacaoDto.toDomain(): Embarcacao = Embarcacao(
    id = id,
    nome = nome,
    empresaNome = empresa?.nomeFantasia?.takeIf { it.isNotBlank() } ?: empresa?.xnome.orEmpty(),
)
