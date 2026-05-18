package com.example.passagenexpress.feature.payment.navigation

import com.example.passagenexpress.core.domain.model.Desconto
import com.example.passagenexpress.core.domain.model.Municipio
import com.example.passagenexpress.core.domain.model.TipoComodo
import com.example.passagenexpress.core.domain.model.Trecho
import com.example.passagenexpress.core.domain.model.TipoDocumento
import com.example.passagenexpress.feature.passenger.ContatoForm
import com.example.passagenexpress.feature.passenger.PassageiroForm
import com.example.passagenexpress.feature.passenger.format.BirthdateMask
import com.example.passagenexpress.feature.passenger.format.DocumentoMask
import com.example.passagenexpress.feature.passenger.format.PhoneMask
import com.example.passagenexpress.feature.passenger.format.parseBirthdate
import com.example.passagenexpress.core.domain.model.Comodo
import com.example.passagenexpress.core.domain.model.Passageiro
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime

/**
 * Payment recebe dois payloads URL-encoded:
 *  - `trechoJson`: opaque do Room (passou por Passenger sem decode)
 *  - `passengersJson`: lista snapshot dos `PassageiroForm` validados
 *
 * O Trecho payload aqui é uma cópia estrutural do `TrechoNavPayload` de `feature:room`
 * (ambos compartilham o shape, mas evitamos dependência cruzada entre features).
 */

@Serializable
internal data class PaymentTrechoPayload(
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
        desconto = desconto?.let { Desconto(it.id, it.valor) },
    )
}

@Serializable
internal data class TipoComodoPayload(val id: Long, val nome: String)

@Serializable
internal data class MunicipioPayload(val slug: String, val nome: String, val uf: String? = null) {
    fun toDomain() = Municipio(slug = slug, nome = nome, uf = uf)
}

@Serializable
internal data class DescontoPayload(val id: Long, val valor: Double)

/**
 * Snapshot mínimo do PassageiroForm que sobrevive à serialização — pula `errors`,
 * `comodo` é mantido (precisamos do `id`+`tipoComodidadeId`+`valor` na montagem do pedido).
 */
@Serializable
internal data class PaymentPassageiroPayload(
    val tipoDocumentoId: Int,
    val documentoDisplay: String,
    val nome: String,
    val telefoneDisplay: String,
    val nascimentoDisplay: String,
    val comodo: PaymentComodoPayload,
)

@Serializable
internal data class PaymentComodoPayload(
    val id: Long,
    val numeracao: Int? = null,
    val nome: String? = null,
    val tipoComodidadeId: Long,
    val valor: Double? = null,
    val quantidade: Int = 1,
) {
    fun toDomain() = Comodo(
        id = id,
        numeracao = numeracao,
        nome = nome,
        linha = null,
        coluna = null,
        isOcupado = false,
        tipoComodidadeId = tipoComodidadeId,
        valor = valor,
        quantidade = quantidade,
    )
}

/** Hidrata um `Passageiro` de domínio a partir do snapshot do formulário. */
internal fun PaymentPassageiroPayload.toPassageiroDomain(): Passageiro {
    val tipo = TipoDocumento.entries.firstOrNull { it.id == tipoDocumentoId } ?: TipoDocumento.CPF
    return Passageiro(
        nome = nome,
        cpf = DocumentoMask.digitsOnly(documentoDisplay, tipo),
        telefone = PhoneMask.digitsOnly(telefoneDisplay),
        email = null,
        dataNascimento = parseBirthdate(nascimentoDisplay),
        tipoDocumento = tipo,
    )
}

internal fun List<PassageiroForm>.toPaymentSnapshot(): List<PaymentPassageiroPayload> =
    map { form ->
        PaymentPassageiroPayload(
            tipoDocumentoId = form.tipoDocumento.id,
            documentoDisplay = form.documentoDisplay,
            nome = form.nome,
            telefoneDisplay = form.telefoneDisplay,
            nascimentoDisplay = form.nascimentoDisplay,
            comodo = PaymentComodoPayload(
                id = form.comodo.id,
                numeracao = form.comodo.numeracao,
                nome = form.comodo.nome,
                tipoComodidadeId = form.comodo.tipoComodidadeId,
                valor = form.comodo.valor,
                quantidade = form.comodo.quantidade,
            ),
        )
    }

/**
 * Wrapper que viaja pelo nav arg `passengersJson`. Contém a lista de passageiros + os dados do
 * bloco "Dados para Contato" (nome/email/telefone) que viram o `contato` do `NovoPedido`.
 */
@Serializable
internal data class PaymentSalePayload(
    val passageiros: List<PaymentPassageiroPayload>,
    val contato: PaymentContatoPayload,
)

@Serializable
internal data class PaymentContatoPayload(
    val nome: String,
    val email: String,
    val telefoneDisplay: String,
)

internal fun ContatoForm.toSnapshot() = PaymentContatoPayload(
    nome = nome,
    email = email,
    telefoneDisplay = telefoneDisplay,
)

internal val PaymentNavJson: Json = Json { ignoreUnknownKeys = true }

internal fun decodeTrechoArg(raw: String): PaymentTrechoPayload {
    val json = URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
    return PaymentNavJson.decodeFromString(PaymentTrechoPayload.serializer(), json)
}

internal fun encodeSaleArg(value: PaymentSalePayload): String {
    val json = PaymentNavJson.encodeToString(PaymentSalePayload.serializer(), value)
    return URLEncoder.encode(json, StandardCharsets.UTF_8.name())
}

internal fun decodeSaleArg(raw: String): PaymentSalePayload {
    val json = URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
    return PaymentNavJson.decodeFromString(PaymentSalePayload.serializer(), json)
}

/** Avoid leaking `PassageiroForm`/`ContatoForm` to TotemNavHost. */
fun buildSaleArg(forms: List<PassageiroForm>, contato: ContatoForm): String =
    encodeSaleArg(
        PaymentSalePayload(
            passageiros = forms.toPaymentSnapshot(),
            contato = contato.toSnapshot(),
        )
    )

/** Format `dd/MM/yyyy` (a API espera nesse formato em `data_hora`/`nascimento`). */
internal fun LocalDate.toBackendDate(): String = BirthdateMask.format(this)
