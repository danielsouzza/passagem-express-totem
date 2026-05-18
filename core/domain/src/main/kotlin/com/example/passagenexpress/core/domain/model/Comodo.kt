package com.example.passagenexpress.core.domain.model

/** Cômodo individual (poltrona ou camarote numerado) — modo assigned-seat. */
data class Comodo(
    val id: Long,
    val numeracao: Int?,
    val nome: String?,
    val linha: Int?,
    val coluna: Int?,
    val isOcupado: Boolean,
    val tipoComodidadeId: Long,
    val valor: Double?,
    val quantidade: Int = 1,
)

/** Bucket de cômodos livres por tipo — modo free-seating. */
data class ComodoLivre(
    val tipoComodidadeId: Long,
    val quantidade: Int,
)
