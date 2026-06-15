package com.example.passagenexpress.core.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Trecho(
    val id: Long,
    val idViagem: Long,
    val dataEmbarque: LocalDate,
    val horario: LocalTime,
    val tempoViagemMinutos: Int,
    val valor: Double,
    val taxaDeEmbarque: Double,
    val embarcacao: String,
    val poltronaLivre: Boolean,
    val linhas: Int,
    val colunas: Int,
    val tiposComodos: List<TipoComodo>,
    val municipioOrigem: Municipio,
    val municipioDestino: Municipio,
    val desconto: Desconto? = null,
)

/**
 * Desconto promocional aplicado a um trecho. `valor` é o **valor absoluto** em reais
 * a subtrair do preço base (ex: 90.0 = R$ 90,00 off). `nome` é o código/label da promo
 * (ex: "PROMO-STM-MAO-120") quando o backend manda.
 */
data class Desconto(
    val id: Long,
    val valor: Double,
    val nome: String? = null,
)

/**
 * Preço final por passagem, aplicando desconto (se houver) sobre `comodoValor` (preço
 * específico do cômodo, quando o backend devolve um) ou `valor` base do trecho.
 * Taxa de embarque NÃO entra aqui — soma à parte.
 */
fun Trecho.precoBaseComDesconto(comodoValor: Double? = null): Double {
    val base = comodoValor ?: valor
    val off = desconto?.valor ?: 0.0
    return (base - off).coerceAtLeast(0.0)
}

/** id 1 = poltrona; id 4 = camarote; demais = cabines/setores diversos */
data class TipoComodo(
    val id: Long,
    val nome: String,
)
