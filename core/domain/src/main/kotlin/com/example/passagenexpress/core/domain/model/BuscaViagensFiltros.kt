package com.example.passagenexpress.core.domain.model

import java.time.LocalDate

/**
 * Parâmetros para `GET /api/trechos-viagem`.
 *
 * Quando o totem tem porto configurado, mande [portoSlug] e deixe [municipioOrigemCodigo] null —
 * o backend assume a origem a partir do porto. Acesso por cidade (sem porto) usa
 * [municipioOrigemCodigo] com o código IBGE do município.
 */
data class BuscaViagensFiltros(
    val portoSlug: String? = null,
    val municipioOrigemCodigo: String? = null,
    val municipioDestinoSlug: String? = null,
    val data: LocalDate? = null,
    val quantia: Int = 10,
    val dataIrrestrita: Boolean = false,
    /** Quando o totem está a bordo de uma embarcação, filtra os trechos por ela (`embarcacao_id`). */
    val embarcacaoId: Long? = null,
)

data class ResultadoBuscaViagens(
    val trechos: List<Trecho>,
    val total: Int,
    val proximaViagem: Trecho? = null,
)
