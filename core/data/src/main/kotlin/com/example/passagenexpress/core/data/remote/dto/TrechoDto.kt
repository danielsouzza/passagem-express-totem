package com.example.passagenexpress.core.data.remote.dto

import com.example.passagenexpress.core.data.remote.parse.IntBooleanSerializer
import com.example.passagenexpress.core.data.remote.parse.parseLocalDate
import com.example.passagenexpress.core.data.remote.parse.parseLocalTime
import com.example.passagenexpress.core.data.remote.parse.parseMoney
import com.example.passagenexpress.core.data.remote.parse.parseTempoViagemMinutos
import com.example.passagenexpress.core.domain.model.Desconto
import com.example.passagenexpress.core.domain.model.DescontoValorPorTipo
import com.example.passagenexpress.core.domain.model.Municipio
import com.example.passagenexpress.core.domain.model.TipoComodo
import com.example.passagenexpress.core.domain.model.Trecho
import com.example.passagenexpress.core.domain.model.ValorPorTipo
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
    /**
     * Preço específico por tipo de cômodo. Esparso — um tipo sem registro aqui usa `valor`.
     * `valores[].valor` pode vir número JSON ou string com PONTO decimal (nunca vírgula/"R$"),
     * diferente de `valor`/`taxa_de_embarque`; `isLenient` + `coerceInputValues` do NetworkJson
     * já coagem ambos os formatos para `Double`.
     */
    val valores: List<ValorPorTipoDto> = emptyList(),
)

@Serializable
data class ValorPorTipoDto(
    @SerialName("tipo_comodidade_id") val tipoComodidadeId: Long,
    val valor: Double? = null,
)

@Serializable
data class DescontoDto(
    val id: Long,
    /** Backend manda número (ex: `90` = R$ 90 off). É um valor absoluto a subtrair do `valor`. */
    val desconto: Double? = null,
    val nome: String? = null,
    /** Formato novo: desconto restrito aos tipos listados aqui (fonte de verdade quando presente). */
    @SerialName("valores_por_tipo") val valoresPorTipo: List<DescontoValorPorTipoDto> = emptyList(),
    /** Formato novo alternativo (sem o valor por tipo, só os ids/nomes cobertos). */
    @SerialName("tipos_comodidade") val tiposComodidade: List<TipoComodoDto> = emptyList(),
)

@Serializable
data class DescontoValorPorTipoDto(
    @SerialName("tipo_comodidade_id") val tipoComodidadeId: Long,
    @SerialName("valor_desconto") val valorDesconto: Double? = null,
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
        desconto = desconto?.let {
            Desconto(
                id = it.id,
                valor = it.desconto ?: 0.0,
                nome = it.nome,
                valoresPorTipo = it.valoresPorTipo.map { v ->
                    DescontoValorPorTipo(v.tipoComodidadeId, v.valorDesconto ?: 0.0)
                },
                tiposComodidadeIds = it.tiposComodidade.map { t -> t.id },
            )
        },
        valores = valores.map { ValorPorTipo(it.tipoComodidadeId, it.valor ?: 0.0) },
    )
}

private fun MunicipioRefDto.toDomain(): Municipio =
    Municipio(slug = slug.orEmpty(), nome = nome, uf = uf)
