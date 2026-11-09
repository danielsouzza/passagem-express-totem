package com.example.passagenexpress.core.data.remote.dto.onibus

import com.example.passagenexpress.core.data.remote.parse.parseLocalDateTime
import com.example.passagenexpress.core.data.remote.parse.parseMoney
import com.example.passagenexpress.core.domain.model.onibus.AssentoBloqueado
import com.example.passagenexpress.core.domain.model.onibus.AssentoOnibus
import com.example.passagenexpress.core.domain.model.onibus.EscalaOnibus
import com.example.passagenexpress.core.domain.model.onibus.PixOnibus
import com.example.passagenexpress.core.domain.model.onibus.TipoViagemOnibus
import com.example.passagenexpress.core.domain.model.onibus.ViagemOnibus
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime

// ---------------------------------------------------------------------------
// Helpers tolerantes: o back (PHP) às vezes manda número como string ("R$ 189,99" / "189.99"),
// bool como 0/1, e ids como string ("01"). Ler como JsonElement evita quebrar a desserialização.
// ---------------------------------------------------------------------------

internal fun JsonElement?.asStringOrNull(): String? = when (this) {
    null, JsonNull -> null
    is JsonPrimitive -> contentOrNull
    else -> null
}

internal fun JsonElement?.asDouble(): Double = when (this) {
    null, JsonNull -> 0.0
    is JsonPrimitive -> doubleOrNull ?: contentOrNull.let { raw ->
        // "189.99" (ponto decimal) ou "R$ 189,99" (formato BR).
        raw?.trim()?.toDoubleOrNull() ?: raw.parseMoney()
    }
    else -> 0.0
}

internal fun JsonElement?.asBoolean(): Boolean = when (this) {
    null, JsonNull -> false
    is JsonPrimitive -> booleanOrNull ?: (contentOrNull?.trim()?.let { it == "1" || it.equals("true", true) } ?: false)
    else -> false
}

internal fun JsonElement?.asInt(default: Int = 0): Int = when (this) {
    null, JsonNull -> default
    is JsonPrimitive -> contentOrNull?.trim()?.toDoubleOrNull()?.toInt() ?: default
    else -> default
}

/** "2026-11-20T00:01:00-03:00" (ISO com offset) ou "2026-11-20 00:01:00". */
internal fun String?.parseDateTimeFlex(): LocalDateTime? {
    if (isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(trim()).toLocalDateTime() }.getOrNull()
        ?: parseLocalDateTime()
        ?: runCatching { LocalDateTime.parse(trim()) }.getOrNull()
}

// ---------------------------------------------------------------------------
// Viagens
// ---------------------------------------------------------------------------

@Serializable
data class ViagensOnibusResponseDto(
    val trechos: ViagensOnibusPageDto = ViagensOnibusPageDto(),
)

@Serializable
data class ViagensOnibusPageDto(
    val data: List<ViagemOnibusDto> = emptyList(),
)

@Serializable
data class LocalViagemOnibusDto(
    val nome: String? = null,
    val uf: String? = null,
    val slug: String? = null,
)

@Serializable
data class EmpresaOnibusDto(
    val xnome: String? = null,
    val logo: String? = null,
    @SerialName("nome_fantasia") val nomeFantasia: String? = null,
)

@Serializable
data class ServiceClassDto(val name: String? = null)

@Serializable
data class EscalaOnibusDto(
    @SerialName("trip_uuid") val tripUuid: String? = null,
    @SerialName("trip_leg") val tripLeg: JsonElement? = null,
)

/**
 * Item de `data.trechos.data[]`. Exemplo real (backend local, SP→RJ):
 * `{"id":"4c55…","trip_uuid":"4c55…","embarcacao":"Expresso Do Sul - Executivo",
 *   "tipo_embarcacao":"Executivo","saida":"2026-11-20 00:01:00","chegada":"2026-11-20 06:31:00",
 *   "data_embarque":"2026-11-20T00:01:00-03:00","tempo_viagem":"6h 30m","valor_numerico":189.99,
 *   "assentos_disponiveis":10,"trip_type":"direct","escalas":[{"trip_uuid":"4c55…","trip_leg":1}], …}`
 */
@Serializable
data class ViagemOnibusDto(
    val id: JsonElement? = null,
    @SerialName("trip_uuid") val tripUuid: String? = null,
    val embarcacao: String? = null,
    @SerialName("tipo_embarcacao") val tipoEmbarcacao: String? = null,
    @SerialName("service_class") val serviceClass: ServiceClassDto? = null,
    val empresa: EmpresaOnibusDto? = null,
    val saida: String? = null,
    val chegada: String? = null,
    @SerialName("data_embarque") val dataEmbarque: String? = null,
    @SerialName("tempo_viagem") val tempoViagem: String? = null,
    val valor: JsonElement? = null,
    @SerialName("valor_numerico") val valorNumerico: JsonElement? = null,
    @SerialName("assentos_disponiveis") val assentosDisponiveis: JsonElement? = null,
    @SerialName("municipio_origem") val municipioOrigem: LocalViagemOnibusDto? = null,
    @SerialName("municipio_destino") val municipioDestino: LocalViagemOnibusDto? = null,
    @SerialName("trip_type") val tripType: String? = null,
    val escalas: List<EscalaOnibusDto> = emptyList(),
)

fun ViagemOnibusDto.toDomain(): ViagemOnibus {
    val uuid = tripUuid ?: id.asStringOrNull() ?: error("Viagem de ônibus sem trip_uuid")
    val saidaDt = dataEmbarque.parseDateTimeFlex() ?: saida.parseDateTimeFlex()
        ?: error("Viagem $uuid sem data de embarque")
    val escalasDomain = escalas.mapNotNull { e ->
        e.tripUuid?.let { EscalaOnibus(tripUuid = it, tripLeg = e.tripLeg.asInt(1)) }
    }.ifEmpty { listOf(EscalaOnibus(uuid, 1)) }
    val preco = valorNumerico.asDouble().takeIf { it > 0.0 } ?: valor.asDouble()
    return ViagemOnibus(
        tripUuid = uuid,
        empresaNome = empresa?.nomeFantasia ?: empresa?.xnome ?: embarcacao.orEmpty(),
        empresaLogo = empresa?.logo?.takeIf { it.isNotBlank() },
        classe = serviceClass?.name ?: tipoEmbarcacao.orEmpty(),
        origemNome = municipioOrigem?.nome.orEmpty(),
        origemSlug = municipioOrigem?.slug.orEmpty(),
        destinoNome = municipioDestino?.nome.orEmpty(),
        destinoSlug = municipioDestino?.slug.orEmpty(),
        saida = saidaDt,
        chegada = chegada.parseDateTimeFlex(),
        dataEmbarqueRaw = dataEmbarque ?: saida.orEmpty(),
        duracao = tempoViagem.orEmpty(),
        valor = preco,
        assentosDisponiveis = assentosDisponiveis.asInt(0),
        tipo = when (tripType) {
            "one_stop" -> TipoViagemOnibus.UmaParada
            "connection" -> TipoViagemOnibus.Conexao
            else -> TipoViagemOnibus.Direta
        },
        escalas = escalasDomain,
    )
}

// ---------------------------------------------------------------------------
// Mapa de assentos: data = {"1": [ {label, numeracao, linha, coluna, is_ocupado, valor, …} ]}
// ---------------------------------------------------------------------------

@Serializable
data class AssentoOnibusDto(
    val id: JsonElement? = null,
    val label: JsonElement? = null,
    val numeracao: JsonElement? = null,
    val linha: JsonElement? = null,
    val coluna: JsonElement? = null,
    @SerialName("is_ocupado") val isOcupado: JsonElement? = null,
    val valor: JsonElement? = null,
)

fun AssentoOnibusDto.toDomain(): AssentoOnibus? {
    val labelStr = label.asStringOrNull() ?: id.asStringOrNull() ?: return null
    return AssentoOnibus(
        label = labelStr,
        numeracao = numeracao.asStringOrNull() ?: labelStr,
        linha = linha.asInt(0),
        coluna = coluna.asInt(0),
        ocupado = isOcupado.asBoolean(),
        valor = valor.asDouble(),
    )
}

/** Extrai a lista de assentos: chave "1" (Poltrona) ou, na falta, o primeiro array do objeto. */
internal fun JsonElement.assentosArray(): JsonArray? = when (this) {
    is JsonArray -> this
    is JsonObject -> (this["1"] as? JsonArray) ?: values.firstOrNull { it is JsonArray } as? JsonArray
    else -> null
}

// ---------------------------------------------------------------------------
// Bloqueio: envelope.data = { data: { comodos, taxa_embarque, trecho }, formas_pagamento }
// ---------------------------------------------------------------------------

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BloquearAssentosRequestDto(
    @SerialName("trip_id") val tripId: String,
    val seats: List<SeatLabelDto>,
    // Só vai em conexão; em viagem direta o site omite o campo (não manda `null`).
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("trip_leg") val tripLeg: Int? = null,
)

@Serializable
data class SeatLabelDto(val label: String)

@Serializable
data class DesbloquearAssentosRequestDto(
    @SerialName("trip_id") val tripId: String,
    @SerialName("seat_ids") val seatIds: List<String>,
)

@Serializable
data class ComodoBloqueadoDto(
    val id: JsonElement? = null,
    @SerialName("seat_id") val seatId: JsonElement? = null,
    val label: JsonElement? = null,
    val numeracao: JsonElement? = null,
    val valor: JsonElement? = null,
)

fun ComodoBloqueadoDto.toDomain(): AssentoBloqueado? {
    val seat = seatId.asStringOrNull() ?: id.asStringOrNull() ?: return null
    val lbl = label.asStringOrNull() ?: numeracao.asStringOrNull() ?: seat
    return AssentoBloqueado(
        seatId = seat,
        label = lbl,
        numeracao = numeracao.asStringOrNull() ?: lbl,
        valor = valor.asDouble(),
    )
}

/** Acha `comodos` em `data.data.comodos` (formato atual) ou `data.comodos` (defensivo). */
internal fun JsonElement.comodosArray(): JsonArray? {
    val obj = this as? JsonObject ?: return null
    (obj["comodos"] as? JsonArray)?.let { return it }
    return ((obj["data"] as? JsonObject)?.get("comodos")) as? JsonArray
}

// ---------------------------------------------------------------------------
// Pedido
// ---------------------------------------------------------------------------

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CriarPedidoOnibusRequestDto(
    val trecho: String,
    val viagem: String,
    val contato: ContatoOnibusDto,
    val dataComodos: List<DataComodoOnibusDto>,
    @SerialName("pedido_id") val pedidoId: Long? = null,
    // Opcionais omitidos quando nulos — igual ao site (só ida não manda `dataVolta`).
    @EncodeDefault(EncodeDefault.Mode.NEVER) val dataVolta: DataVoltaOnibusDto? = null,
    /** Canal totem — mesmo valor que o totem manda no pedido de barco. */
    val origem: Int = 3,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val agente: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val qr: String? = null,
)

@Serializable
data class ContatoOnibusDto(
    val nome: String,
    val email: String,
    val telefone: String,
)

@Serializable
data class DataComodoOnibusDto(
    val comodo: String,
    val valor: Double,
    @SerialName("tipo_doc") val tipoDoc: String,
    val document: String,
    val nome: String,
    @SerialName("data_nascimento") val dataNascimento: String,
    val telefone: String,
    @SerialName("seat_label") val seatLabel: String,
    @SerialName("item_type") val itemType: String = "ticket",
    // O site manda sempre "direct", mesmo em conexão: a perna vem de trip_uuid + trip_leg.
    @SerialName("trip_type") val tripType: String = "direct",
    @SerialName("trip_uuid") val tripUuid: String,
    @SerialName("trip_leg") val tripLeg: Int,
    @SerialName("origem_slug") val origemSlug: String,
    @SerialName("destino_slug") val destinoSlug: String,
    @SerialName("data_embarque") val dataEmbarque: String,
)

@Serializable
data class DataVoltaOnibusDto(
    val trecho: String,
    val viagem: String,
    val dataComodos: List<DataComodoOnibusDto>,
)

@Serializable
data class PedidoOnibusResponseDto(
    val id: Long,
    val total: JsonElement? = null,
)

// ---------------------------------------------------------------------------
// Pix / boarding pass
// ---------------------------------------------------------------------------

@Serializable
data class PixOnibusRequestDto(
    @SerialName("pedido_id") val pedidoId: Long,
    /** Konduto (antifraude) — o totem não roda o JS da Konduto, então vai null. */
    @SerialName("visitor_id") val visitorId: String? = null,
    val nome: String? = null,
    val cpf: String? = null,
)

@Serializable
data class PixOnibusResponseDto(
    @SerialName("qr_code") val qrCode: String? = null,
    @SerialName("pix_copia_cola") val pixCopiaCola: String? = null,
    val amount: JsonElement? = null,
    val expiration: String? = null,
)

fun PixOnibusResponseDto.toDomain(): PixOnibus = PixOnibus(
    qrCodeDataUri = qrCode?.takeIf { it.isNotBlank() },
    copiaECola = pixCopiaCola.orEmpty(),
    amount = amount.asDouble(),
    expiraEm = expiration?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }
        ?: expiration?.let { runCatching { Instant.parse(it) }.getOrNull() },
)

@Serializable
data class BoardingPassResponseDto(
    val boardingPassBase64: String? = null,
)
