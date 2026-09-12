package com.example.passagenexpress.core.data.remote.dto

import com.example.passagenexpress.core.data.remote.parse.IntBooleanSerializer
import com.example.passagenexpress.core.domain.model.Tema
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TemaDto(
    @Serializable(with = IntBooleanSerializer::class)
    @SerialName("is_super") val isSuper: Boolean = false,
)

fun TemaDto.toDomain(): Tema = Tema(isSuper = isSuper)
