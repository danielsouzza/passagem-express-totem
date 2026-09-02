package com.example.passagenexpress.core.domain.model

/**
 * Embarcação (barco/lancha) que opera viagens. Usada quando o totem é instalado a bordo de uma
 * embarcação específica: a config guarda a embarcação e a busca de viagens passa a filtrar só os
 * trechos daquele barco (`embarcacao_id`).
 */
data class Embarcacao(
    val id: Long,
    val nome: String,
    val empresaNome: String = "",
)
