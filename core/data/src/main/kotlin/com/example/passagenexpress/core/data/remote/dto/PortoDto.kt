package com.example.passagenexpress.core.data.remote.dto

import com.example.passagenexpress.core.domain.model.Porto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PortoDto(
    val id: Long,
    val slug: String,
    val nome: String,
    @SerialName("municipio_codigo") val municipioCodigo: String? = null,
    @SerialName("municipio_nome") val municipioNome: String? = null,
)

fun PortoDto.toDomain(): Porto = Porto(
    id = id,
    slug = slug,
    nome = nome,
    municipioCodigo = municipioCodigo.orEmpty(),
    municipioNome = municipioNome.orEmpty(),
)
