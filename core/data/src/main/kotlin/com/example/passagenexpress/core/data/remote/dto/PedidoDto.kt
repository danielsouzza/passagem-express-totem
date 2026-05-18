package com.example.passagenexpress.core.data.remote.dto

import com.example.passagenexpress.core.data.remote.parse.parseLocalDateTime
import com.example.passagenexpress.core.data.remote.parse.parseMoney
import com.example.passagenexpress.core.domain.model.Pedido
import com.example.passagenexpress.core.domain.model.PedidoStatus
import com.example.passagenexpress.core.domain.repository.NovoPedido
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class PedidoDto(
    val id: Long,
    val status: String? = null,
    @SerialName("total_passagens") val totalPassagens: String? = null,
    @SerialName("total_taxas") val totalTaxas: String? = null,
    val total: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

fun PedidoDto.toDomain(): Pedido = Pedido(
    id = id,
    status = parsePedidoStatus(status),
    totalPassagens = totalPassagens.parseMoney(),
    totalTaxas = totalTaxas.parseMoney(),
    total = total.parseMoney(),
    criadoEm = createdAt.parseLocalDateTime() ?: LocalDateTime.now(),
)

@Serializable
data class StatusResponseDto(val status: String? = null)

fun parsePedidoStatus(raw: String?): PedidoStatus = when (raw?.lowercase()) {
    "pago", "approved", "aprovado" -> PedidoStatus.Pago
    "cancelado", "cancelled" -> PedidoStatus.Cancelado
    "expirado", "expired" -> PedidoStatus.Expirado
    else -> PedidoStatus.Aberto
}

@Serializable
data class CriarPedidoRequestDto(
    @SerialName("pedido_id") val pedidoId: Long? = null,
    val trecho: Long,
    val viagem: Long,
    @SerialName("data_hora") val dataHora: String,
    @SerialName("total_passagems") val totalPassagens: Double,
    @SerialName("total_taxas") val totalTaxas: Double,
    val total: Double,
    val origem: Int = 3,
    val contato: ContatoRequestDto,
    val dataComodos: List<DataComodoRequestDto>,
)

@Serializable
data class ContatoRequestDto(
    val nome: String,
    val email: String? = null,
    val telefone: String,
    val document: String,
    @SerialName("tipo_doc") val tipoDoc: Int,
    val nascimento: String? = null,
    @SerialName("data_nascimento") val dataNascimento: String? = null,
)

@Serializable
data class DataComodoRequestDto(
    val nome: String,
    val document: String,
    val telefone: String,
    @SerialName("tipo_doc") val tipoDoc: Int,
    val nascimento: String? = null,
    @SerialName("data_nascimento") val dataNascimento: String? = null,
    val comodo: Long,
    @SerialName("tipo_comodidade") val tipoComodidade: Long,
    val embarque: Double,
    val valor: Double,
    @SerialName("desconto_id") val descontoId: Long? = null,
    @SerialName("comodo_relacionado") val comodoRelacionado: Long? = null,
    @SerialName("comodos_filhos") val comodosFilhos: Int = 1,
    @SerialName("qtd_comodos_filhos") val qtdComodosFilhos: Int = 1,
    val isContact: Boolean = false,
)

fun NovoPedido.toDto(): CriarPedidoRequestDto = CriarPedidoRequestDto(
    pedidoId = pedidoId,
    trecho = trechoId,
    viagem = viagemId,
    dataHora = dataHora,
    totalPassagens = totalPassagens,
    totalTaxas = totalTaxas,
    total = total,
    origem = origem,
    contato = ContatoRequestDto(
        nome = contato.nome,
        email = contato.email,
        telefone = contato.telefone,
        document = contato.cpf,
        tipoDoc = contato.tipoDocumento.id,
        nascimento = contato.dataNascimento?.toString(),
        dataNascimento = contato.dataNascimento?.toString(),
    ),
    dataComodos = itens.map { item ->
        DataComodoRequestDto(
            nome = item.passageiro.nome,
            document = item.passageiro.cpf,
            telefone = item.passageiro.telefone,
            tipoDoc = item.passageiro.tipoDocumento.id,
            nascimento = item.passageiro.dataNascimento?.toString(),
            dataNascimento = item.passageiro.dataNascimento?.toString(),
            comodo = item.comodoId,
            tipoComodidade = item.tipoComodidadeId,
            embarque = item.taxaEmbarque,
            valor = item.valor,
            descontoId = item.descontoId,
            comodoRelacionado = item.comodoRelacionado,
            comodosFilhos = item.comodosFilhos,
            qtdComodosFilhos = item.comodosFilhos,
            isContact = item.isContact,
        )
    },
)
