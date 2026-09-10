package com.example.passagenexpress.feature.room.navigation

import com.example.passagenexpress.core.domain.model.Desconto
import com.example.passagenexpress.core.domain.model.DescontoValorPorTipo
import com.example.passagenexpress.core.domain.model.Municipio
import com.example.passagenexpress.core.domain.model.TipoComodo
import com.example.passagenexpress.core.domain.model.Trecho
import com.example.passagenexpress.core.domain.model.ValorPorTipo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime

/**
 * Serializable mirror of [Trecho] used to ferry the selected viagem through Compose Navigation.
 * The domain model holds java.time types — we flatten them to ISO strings at the nav boundary
 * so the payload stays pure JSON-friendly without leaking serializer config into core/domain.
 */
@Serializable
internal data class TrechoNavPayload(
    val id: Long,
    val idViagem: Long,
    val dataEmbarque: String,
    val horario: String,
    val tempoViagemMinutos: Int,
    val valor: Double,
    val taxaDeEmbarque: Double,
    val embarcacao: String,
    val poltronaLivre: Boolean,
    val linhas: Int,
    val colunas: Int,
    val tiposComodos: List<TipoComodoPayload>,
    val municipioOrigem: MunicipioPayload,
    val municipioDestino: MunicipioPayload,
    val desconto: DescontoPayload? = null,
    val valores: List<ValorPorTipoPayload> = emptyList(),
) {
    fun toDomain(): Trecho = Trecho(
        id = id,
        idViagem = idViagem,
        dataEmbarque = LocalDate.parse(dataEmbarque),
        horario = LocalTime.parse(horario),
        tempoViagemMinutos = tempoViagemMinutos,
        valor = valor,
        taxaDeEmbarque = taxaDeEmbarque,
        embarcacao = embarcacao,
        poltronaLivre = poltronaLivre,
        linhas = linhas,
        colunas = colunas,
        tiposComodos = tiposComodos.map { TipoComodo(it.id, it.nome) },
        municipioOrigem = municipioOrigem.toDomain(),
        municipioDestino = municipioDestino.toDomain(),
        desconto = desconto?.let {
            Desconto(
                id = it.id,
                valor = it.valor,
                valoresPorTipo = it.valoresPorTipo.map { v -> DescontoValorPorTipo(v.tipoComodidadeId, v.valorDesconto) },
                tiposComodidadeIds = it.tiposComodidadeIds,
            )
        },
        valores = valores.map { ValorPorTipo(it.tipoComodidadeId, it.valor) },
    )
}

@Serializable
internal data class TipoComodoPayload(val id: Long, val nome: String)

@Serializable
internal data class MunicipioPayload(val slug: String, val nome: String, val uf: String? = null) {
    fun toDomain() = Municipio(slug = slug, nome = nome, uf = uf)
}

@Serializable
internal data class DescontoPayload(
    val id: Long,
    val valor: Double,
    val valoresPorTipo: List<DescontoValorPorTipoPayload> = emptyList(),
    val tiposComodidadeIds: List<Long> = emptyList(),
)

@Serializable
internal data class DescontoValorPorTipoPayload(val tipoComodidadeId: Long, val valorDesconto: Double)

@Serializable
internal data class ValorPorTipoPayload(val tipoComodidadeId: Long, val valor: Double)

internal fun Trecho.toNavPayload(): TrechoNavPayload = TrechoNavPayload(
    id = id,
    idViagem = idViagem,
    dataEmbarque = dataEmbarque.toString(),
    horario = horario.toString(),
    tempoViagemMinutos = tempoViagemMinutos,
    valor = valor,
    taxaDeEmbarque = taxaDeEmbarque,
    embarcacao = embarcacao,
    poltronaLivre = poltronaLivre,
    linhas = linhas,
    colunas = colunas,
    tiposComodos = tiposComodos.map { TipoComodoPayload(it.id, it.nome) },
    municipioOrigem = MunicipioPayload(municipioOrigem.slug, municipioOrigem.nome, municipioOrigem.uf),
    municipioDestino = MunicipioPayload(municipioDestino.slug, municipioDestino.nome, municipioDestino.uf),
    desconto = desconto?.let {
        DescontoPayload(
            id = it.id,
            valor = it.valor,
            valoresPorTipo = it.valoresPorTipo.map { v -> DescontoValorPorTipoPayload(v.tipoComodidadeId, v.valorDesconto) },
            tiposComodidadeIds = it.tiposComodidadeIds,
        )
    },
    valores = valores.map { ValorPorTipoPayload(it.tipoComodidadeId, it.valor) },
)

internal val RoomNavJson: Json = Json { ignoreUnknownKeys = true }

internal fun encodeTrechoArg(trecho: Trecho): String {
    val json = RoomNavJson.encodeToString(TrechoNavPayload.serializer(), trecho.toNavPayload())
    return URLEncoder.encode(json, StandardCharsets.UTF_8.name())
}

internal fun decodeTrechoArg(raw: String): TrechoNavPayload {
    val json = URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
    return RoomNavJson.decodeFromString(TrechoNavPayload.serializer(), json)
}
