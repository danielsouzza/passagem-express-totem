package com.example.passagenexpress.feature.passenger.navigation

import com.example.passagenexpress.core.domain.model.Comodo
import com.example.passagenexpress.core.domain.model.FormaPagamento
import com.example.passagenexpress.core.domain.repository.InicioVenda
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Serializable mirror of [InicioVenda] used to ferry the sale payload (cômodos efetivos
 * pós-`iniciarVenda` + formas de pagamento liberadas pelo backend) through Compose Navigation.
 */
@Serializable
internal data class InicioVendaNavPayload(
    val trechoId: Long,
    val comodos: List<ComodoPayload>,
    val formasPagamento: List<FormaPagamentoPayload>,
) {
    fun toDomain(): InicioVenda = InicioVenda(
        trechoId = trechoId,
        comodos = comodos.map { it.toDomain() },
        formasPagamento = formasPagamento.map { FormaPagamento(it.id, it.codigo, it.nome) },
    )
}

@Serializable
internal data class ComodoPayload(
    val id: Long,
    val numeracao: Int? = null,
    val nome: String? = null,
    val linha: Int? = null,
    val coluna: Int? = null,
    val isOcupado: Boolean = false,
    val tipoComodidadeId: Long,
    val valor: Double? = null,
    val quantidade: Int = 1,
) {
    fun toDomain() = Comodo(
        id = id,
        numeracao = numeracao,
        nome = nome,
        linha = linha,
        coluna = coluna,
        isOcupado = isOcupado,
        tipoComodidadeId = tipoComodidadeId,
        valor = valor,
        quantidade = quantidade,
    )
}

@Serializable
internal data class FormaPagamentoPayload(val id: Long, val codigo: String, val nome: String)

internal fun InicioVenda.toNavPayload(): InicioVendaNavPayload = InicioVendaNavPayload(
    trechoId = trechoId,
    comodos = comodos.map {
        ComodoPayload(
            id = it.id,
            numeracao = it.numeracao,
            nome = it.nome,
            linha = it.linha,
            coluna = it.coluna,
            isOcupado = it.isOcupado,
            tipoComodidadeId = it.tipoComodidadeId,
            valor = it.valor,
            quantidade = it.quantidade,
        )
    },
    formasPagamento = formasPagamento.map { FormaPagamentoPayload(it.id, it.codigo, it.nome) },
)

internal val PassengerNavJson: Json = Json { ignoreUnknownKeys = true }

internal fun encodeInicioVendaArg(value: InicioVenda): String {
    val json = PassengerNavJson.encodeToString(InicioVendaNavPayload.serializer(), value.toNavPayload())
    return URLEncoder.encode(json, StandardCharsets.UTF_8.name())
}

internal fun decodeInicioVendaArg(raw: String): InicioVendaNavPayload {
    val json = URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
    return PassengerNavJson.decodeFromString(InicioVendaNavPayload.serializer(), json)
}
