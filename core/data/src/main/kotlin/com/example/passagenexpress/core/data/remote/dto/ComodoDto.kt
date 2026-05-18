package com.example.passagenexpress.core.data.remote.dto

import com.example.passagenexpress.core.data.remote.parse.IntBooleanSerializer
import com.example.passagenexpress.core.data.remote.parse.PhpAssocMapSerializer
import com.example.passagenexpress.core.data.remote.parse.parseMoney
import com.example.passagenexpress.core.domain.model.Comodo
import com.example.passagenexpress.core.domain.model.ComodoLivre
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class ComodoDto(
    val id: Long,
    val numeracao: Int? = null,
    val nome: String? = null,
    val linha: Int? = null,
    val coluna: Int? = null,
    @SerialName("is_ocupado")
    @Serializable(with = IntBooleanSerializer::class)
    val isOcupado: Boolean = false,
    @SerialName("tipo_comodidade_id") val tipoComodidadeId: Long? = null,
    @SerialName("tipo_comodidade") val tipoComodidade: TipoComodoDto? = null,
    val quantidade: Int = 1,
    @SerialName("comodo_trechos") val comodoTrechos: ComodoTrechoValorDto? = null,
)

@Serializable
data class ComodoTrechoValorDto(
    val valor: String? = null,
)

@Serializable
data class ComodoLivreDto(
    @SerialName("tipo_comodidade_id") val tipoComodidadeId: Long,
    val quantidade: Int = 0,
)

/**
 * Wraps the `Map<tipoId, List<ComodoDto>>` payload returned by `/api/comodos/{poltronas,camarotes}`.
 * Backend serializes empty maps as `[]` (PHP assoc-array quirk) — the custom serializer normalizes that.
 */
@Serializable(with = ComodosByTipoSerializer::class)
data class ComodosByTipoDto(val byTipo: Map<String, List<ComodoDto>>)

private object ComodosByTipoSerializer : KSerializer<ComodosByTipoDto> {
    private val delegate = PhpAssocMapSerializer(ListSerializer(ComodoDto.serializer()))
    override val descriptor: SerialDescriptor = delegate.descriptor
    override fun deserialize(decoder: Decoder): ComodosByTipoDto =
        ComodosByTipoDto(delegate.deserialize(decoder))
    override fun serialize(encoder: Encoder, value: ComodosByTipoDto) {
        delegate.serialize(encoder, value.byTipo)
    }
}

fun ComodoDto.toDomain(): Comodo = Comodo(
    id = id,
    numeracao = numeracao,
    nome = nome,
    linha = linha,
    coluna = coluna,
    isOcupado = isOcupado,
    tipoComodidadeId = tipoComodidadeId ?: tipoComodidade?.id ?: 0L,
    valor = comodoTrechos?.valor.parseMoney().takeIf { it > 0.0 },
    quantidade = quantidade,
)

fun ComodoLivreDto.toDomain(): ComodoLivre = ComodoLivre(
    tipoComodidadeId = tipoComodidadeId,
    quantidade = quantidade,
)

@Serializable
data class IniciarVendaRequestDto(
    @SerialName("trecho_id") val trechoId: Long,
    @SerialName("viagem_id") val viagemId: Long,
    val tiposComodoEscolhidos: Map<String, Int>,
    val comodosAssentosEscolhidos: List<Long>,
)

@Serializable
data class IniciarVendaResponseDto(
    val data: IniciarVendaDataDto = IniciarVendaDataDto(),
    @SerialName("formas_pagamento") val formasPagamento: List<FormaPagamentoDto> = emptyList(),
)

@Serializable
data class IniciarVendaDataDto(
    val trecho: TrechoDto? = null,
    val comodos: List<ComodoDto> = emptyList(),
)

@Serializable
data class FormaPagamentoDto(
    val id: Long = 0L,
    val codigo: String = "",
    val nome: String = "",
)

@Serializable
data class ReservarComodoRequestDto(
    @SerialName("trecho_id") val trechoId: Long,
    @SerialName("viagem_id") val viagemId: Long,
    @SerialName("comodo_id") val comodoId: Long,
)

@Serializable
data class DeletarReservaRequestDto(
    @SerialName("trecho_id") val trechoId: Long,
    @SerialName("viagem_id") val viagemId: Long,
    @SerialName("comodo_ids") val comodoIds: List<Long>,
)
