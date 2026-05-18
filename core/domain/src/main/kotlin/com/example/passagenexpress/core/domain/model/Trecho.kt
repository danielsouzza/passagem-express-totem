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

data class Desconto(
    val id: Long,
    val valor: Double,
)

/** id 1 = poltrona; id 4 = camarote; demais = cabines/setores diversos */
data class TipoComodo(
    val id: Long,
    val nome: String,
)
