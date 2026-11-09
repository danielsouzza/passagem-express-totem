package com.example.passagenexpress.core.domain.model.onibus

import java.time.LocalDateTime
import kotlin.math.roundToLong

/**
 * Viagem de ônibus (`GET api/onibus/viagens` → `data.trechos.data[]`). Identificada por
 * [tripUuid] (ClickBus), não por id numérico como o `Trecho` do barco.
 */
data class ViagemOnibus(
    val tripUuid: String,
    val empresaNome: String,
    val empresaLogo: String?,
    /** Classe do serviço (ex.: "Executivo", "Leito"). */
    val classe: String,
    val origemNome: String,
    val origemSlug: String,
    val destinoNome: String,
    val destinoSlug: String,
    val saida: LocalDateTime,
    val chegada: LocalDateTime?,
    /** `data_embarque` exatamente como veio (ISO com offset) — reenviado no pedido. */
    val dataEmbarqueRaw: String,
    /** Duração formatada pelo back (ex.: "6h 30m"). */
    val duracao: String,
    /** Preço-base por assento (sem a taxa de conveniência). Estimativa: o real vem no bloqueio. */
    val valor: Double,
    val assentosDisponiveis: Int,
    val tipo: TipoViagemOnibus,
    /** Pernas da viagem; direta = uma perna `(tripUuid, 1)`. */
    val escalas: List<EscalaOnibus>,
) {
    val temConexao: Boolean get() = escalas.size > 1
}

enum class TipoViagemOnibus { Direta, UmaParada, Conexao }

data class EscalaOnibus(val tripUuid: String, val tripLeg: Int)

/** Assento do mapa (`GET api/onibus/viagens/{tripUuid}` → `data["1"][]`). */
data class AssentoOnibus(
    /** Identificador do assento no mapa — é o que vai no bloqueio. */
    val label: String,
    val numeracao: String,
    /** Posição lateral (janela/corredor), a partir de 0. O corredor é uma linha que não vem. */
    val linha: Int,
    /** Fileira, ao longo do ônibus. */
    val coluna: Int,
    val ocupado: Boolean,
    val valor: Double,
)

/**
 * Assento bloqueado (`POST api/onibus/reservas/bloquear-assentos`). Depois do bloqueio o assento
 * passa a ser identificado por [seatId] (desbloqueio, pedido); [valor] é o preço real.
 */
data class AssentoBloqueado(
    val seatId: String,
    val label: String,
    val numeracao: String,
    val valor: Double,
)

/** Regra de preço exibido do site (`src/utils/precoOnibus.js`): base + 13% de conveniência. */
object PrecoOnibus {
    const val TAXA_CONVENIENCIA = 0.13

    fun comTaxa(base: Double): Double = arredondar(base * (1 + TAXA_CONVENIENCIA))

    fun arredondar(valor: Double): Double = (valor * 100).roundToLong() / 100.0
}
