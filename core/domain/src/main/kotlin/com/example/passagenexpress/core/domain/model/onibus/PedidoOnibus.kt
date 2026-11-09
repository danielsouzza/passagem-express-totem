package com.example.passagenexpress.core.domain.model.onibus

import java.time.Instant
import java.time.LocalDate

/**
 * Tipos de documento aceitos na venda de ônibus. [apiValue] é o que vai em `tipo_doc` no pedido
 * (`mapTipoDocOnibus` do site). Não tem Título de Eleitor (só no barco).
 */
enum class TipoDocOnibus(val apiValue: String) {
    Cpf("cpf"),
    Rg("rg"),
    Passaporte("passport"),
    Cnh("cnh"),
}

data class PassageiroOnibus(
    val nome: String,
    val tipoDoc: TipoDocOnibus,
    /** Só dígitos (CPF/RG/CNH) ou alfanumérico (passaporte). */
    val documento: String,
    val nascimento: LocalDate,
    /** Só dígitos; opcional. */
    val telefone: String,
)

data class ContatoOnibus(
    val nome: String,
    val email: String,
    /** Só dígitos. */
    val telefone: String,
)

/** Um assento de uma perna, já com o passageiro que vai nele. */
data class ItemPedidoOnibus(
    val assento: AssentoBloqueado,
    val tripUuid: String,
    val tripLeg: Int,
    val origemSlug: String,
    val destinoSlug: String,
    val dataEmbarque: String,
    val passageiro: PassageiroOnibus,
)

/**
 * Carrinho de ônibus (`POST api/onibus/pedidos`). Volta é opcional ([tripUuidVolta] null =
 * só ida). Em conexões, os itens de todas as pernas vão na mesma lista, diferenciados por `trip_leg`.
 */
data class NovoPedidoOnibus(
    val tripUuidIda: String,
    val itensIda: List<ItemPedidoOnibus>,
    val tripUuidVolta: String?,
    val itensVolta: List<ItemPedidoOnibus>,
    val contato: ContatoOnibus,
)

data class PedidoOnibus(
    val id: Long,
    val total: Double,
)

/** QR Pix gerado para o pedido (`POST api/onibus/pagamentos/pix`). */
data class PixOnibus(
    /** `qr_code` — data URI PNG pronto. */
    val qrCodeDataUri: String?,
    val copiaECola: String,
    /** Valor efetivamente cobrado (fonte da verdade). */
    val amount: Double,
    val expiraEm: Instant?,
)

/** `data.status` top-level de `GET api/pedidos/{id}/status` (fonte da verdade durante o Pix). */
enum class StatusPedidoOnibus { Pendente, Pago, Negado, Cancelado }
