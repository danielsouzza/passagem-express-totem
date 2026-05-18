package com.example.passagenexpress.core.data.remote.dto

import com.example.passagenexpress.core.data.remote.parse.parseLocalDateTime
import com.example.passagenexpress.core.data.remote.parse.parseMoney
import com.example.passagenexpress.core.domain.model.Pagamento
import com.example.passagenexpress.core.domain.model.StatusPagamento
import com.example.passagenexpress.core.domain.model.TipoCartao
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PagamentoPixRequestDto(
    @SerialName("pedido_id") val pedidoId: Long,
    val cpf: String,
    val nome: String,
)

@Serializable
data class PagamentoPixResponseDto(
    @SerialName("pedido_id") val pedidoId: Long? = null,
    val valor: String? = null,
    @SerialName("qr_code_base64") val qrCodeBase64: String? = null,
    @SerialName("qr_code") val qrCode: String? = null,
    /**
     * Campo BR Code (string Pix copia-e-cola). O backend retorna como `pix_copia_cola`
     * (web: `StepPagamento.vue` lê `paymentPending.pix_copia_cola`). `copia_e_cola` fica como
     * fallback para variações antigas/legadas do payload.
     */
    @SerialName("pix_copia_cola") val pixCopiaCola: String? = null,
    @SerialName("copia_e_cola") val copiaECola: String? = null,
    @SerialName("expira_em") val expiraEm: String? = null,
)

fun PagamentoPixResponseDto.toDomain(fallbackPedidoId: Long): Pagamento.Pix = Pagamento.Pix(
    pedidoId = pedidoId ?: fallbackPedidoId,
    valor = valor.parseMoney(),
    qrCodeBase64 = qrCodeBase64,
    copiaECola = (pixCopiaCola ?: copiaECola ?: qrCode).orEmpty(),
    expiraEm = expiraEm.parseLocalDateTime(),
)

@Serializable
data class PagamentoCartaoRequestDto(
    @SerialName("pedido_id") val pedidoId: Long,
    val tipo: String,
)

@Serializable
data class StatusPagamentoResponseDto(
    val status: String? = null,
)

fun parseStatusPagamento(raw: String?): StatusPagamento = when (raw?.lowercase()) {
    "aprovado", "approved", "pago" -> StatusPagamento.Aprovado
    "recusado", "denied", "rejected" -> StatusPagamento.Recusado
    "expirado", "expired" -> StatusPagamento.Expirado
    else -> StatusPagamento.Pendente
}

fun TipoCartao.toApiValue(): String = when (this) {
    TipoCartao.Credito -> "credito"
    TipoCartao.Debito -> "debito"
}
