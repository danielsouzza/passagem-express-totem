package com.example.passagenexpress.core.data.remote.dto

import com.example.passagenexpress.core.domain.model.Municipio
import kotlinx.serialization.Serializable

@Serializable
data class MunicipioDto(
    val slug: String,
    val nome: String,
    val uf: String? = null,
)

fun MunicipioDto.toDomain(): Municipio = Municipio(slug = slug, nome = nome, uf = uf)

@Serializable
data class FiltrosResponseDto(
    val municipiosOrigem: List<MunicipioDto> = emptyList(),
    val municipiosDestino: List<MunicipioDto> = emptyList(),
)
