package com.example.passagenexpress.core.data.remote.dto

import com.example.passagenexpress.core.data.remote.parse.IntBooleanSerializer
import com.example.passagenexpress.core.data.remote.parse.parseLocalDate
import com.example.passagenexpress.core.data.remote.parse.parseLocalTime
import com.example.passagenexpress.core.data.remote.parse.parseMoney
import com.example.passagenexpress.core.data.remote.parse.parseTempoViagemMinutos
import com.example.passagenexpress.core.domain.model.Desconto
import com.example.passagenexpress.core.domain.model.Municipio
import com.example.passagenexpress.core.domain.model.TipoComodo
import com.example.passagenexpress.core.domain.model.Trecho
import java.time.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrechoDto(
    val id: Long,
    @SerialName("id_viagem") val idViagem: Long,
    @SerialName("data_embarque") val dataEmbarque: String? = null,
    val horario: String? = null,
    @SerialName("tempo_viagem") val tempoViagem: String? = null,
    val valor: String? = null,
    @SerialName("taxa_de_embarque") val taxaDeEmbarque: String? = null,
    val embarcacao: String? = null,
    @SerialName("poltrona_livre")
    @Serializable(with = IntBooleanSerializer::class)
    val poltronaLivre: Boolean = false,
    val linhas: Int = 0,
    val colunas: Int = 0,
    val desconto: DescontoDto? = null,
    @SerialName("tipos_comodos") val tiposComodos: List<TipoComodoDto> = emptyList(),
    @SerialName("municipio_origem") val municipioOrigem: MunicipioRefDto? = null,
    @SerialName("municipio_destino") val municipioDestino: MunicipioRefDto? = null,
)

@Serializable
data class DescontoDto(
    val id: Long,
    val desconto: String? = null,
)

@Serializable
data class TipoComodoDto(
    val id: Long,
    val nome: String,
)

@Serializable
data class MunicipioRefDto(
    val nome: String,
    val uf: String? = null,
    val slug: String? = null,
)

@Serializable
data class TrechosWrapperDto(
    val data: List<TrechoDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class TrechosViagemResponseDto(
    val trechos: TrechosWrapperDto = TrechosWrapperDto(),
)

fun TrechoDto.toDomain(): Trecho {
    val parsedDate = dataEmbarque.parseLocalDate()
    if (parsedDate == null && !dataEmbarque.isNullOrBlank()) {
        android.util.Log.w(
            "TrechoDto",
            "Falha ao parsear data_embarque='$dataEmbarque' (trecho id=$id)",
        )
    }
    return Trecho(
        id = id,
        idViagem = idViagem,
        dataEmbarque = parsedDate ?: java.time.LocalDate.now(),
        horario = horario.parseLocalTime() ?: dataEmbarque.parseLocalTime() ?: LocalTime.MIDNIGHT,
        tempoViagemMinutos = tempoViagem.parseTempoViagemMinutos(),
        valor = valor.parseMoney(),
        taxaDeEmbarque = taxaDeEmbarque.parseMoney(),
        embarcacao = embarcacao.orEmpty(),
        poltronaLivre = poltronaLivre,
        linhas = linhas,
        colunas = colunas,
        tiposComodos = tiposComodos.map { TipoComodo(it.id, it.nome) },
        municipioOrigem = municipioOrigem?.toDomain() ?: Municipio(slug = "", nome = ""),
        municipioDestino = municipioDestino?.toDomain() ?: Municipio(slug = "", nome = ""),
        desconto = desconto?.let { Desconto(id = it.id, valor = it.desconto.parseMoney()) },
    )
}

private fun MunicipioRefDto.toDomain(): Municipio =
    Municipio(slug = slug.orEmpty(), nome = nome, uf = uf)
