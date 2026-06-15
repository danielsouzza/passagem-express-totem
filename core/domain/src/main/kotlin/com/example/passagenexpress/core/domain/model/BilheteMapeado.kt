package com.example.passagenexpress.core.domain.model

/**
 * Espelho do map JSON retornado por `GET passagens/{passageiro_viagem_id}/bilhete-mapeado`.
 * É a mesma estrutura que o sistema web envia pra impressora térmica — o totem monta os
 * bytes ESC/POS a partir daqui. Todos os campos textuais já vêm formatados pelo backend
 * (valores em "121,00", datas em dd/MM/yy etc.), então o app só posiciona/imprime.
 */
data class BilheteMapeado(
    val agencia: BilheteAgencia,
    val empresa: BilheteEmpresa,
    val embarcacao: BilheteEmbarcacao,
    val trecho: BilheteTrecho,
    val viagem: BilheteViagem,
    val embarque: BilheteEmbarque,
    val comodo: String,
    val valores: BilheteValores,
    /** Forma de pagamento -> valor pago (ex.: "Pix" -> "121,00"). */
    val formasPagamento: Map<String, String>,
    val troco: String,
    val passageiros: List<String>,
    val bilhete: String,
    val codigoBarras: String,
    val validacao: String,
    val tributos: String,
    val observacoes: List<String>,
    /** PNG do PDF417 em data URI base64 (`data:image/png;base64,...`); pode vir nulo. */
    val pdf417: String?,
)

data class BilheteAgencia(
    val cnpj: String,
    val nome: String,
    val endereco: String,
    val endereco1: String,
    val endereco2: String,
)

data class BilheteEmpresa(
    val nome: String,
    val cnpjCpf: String,
    val ie: String,
    val endereco: String,
    val endereco1: String,
    val endereco2: String,
)

data class BilheteEmbarcacao(
    val nome: String,
)

data class BilheteTrecho(
    val portoOrigem: String,
    val municipioOrigem: String,
    val municipioDestino: String,
)

data class BilheteViagem(
    val linha: String,
    val tipo: String,
)

data class BilheteEmbarque(
    val data: String,
    val horario: String,
    val observacao: String,
)

data class BilheteValores(
    val tarifa: String,
    val taxaEmbarque: String,
    val valorTotal: String,
    val desconto: String,
    val taxaAdicional: String,
    val valorAPagar: String,
)
