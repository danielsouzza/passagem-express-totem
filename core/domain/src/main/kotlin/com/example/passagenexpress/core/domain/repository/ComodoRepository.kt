package com.example.passagenexpress.core.domain.repository

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.Comodo
import com.example.passagenexpress.core.domain.model.ComodoLivre

interface ComodoRepository {
    suspend fun listarPoltronas(trechoId: Long, viagemId: Long): AppResult<Map<Long, List<Comodo>>>
    suspend fun listarCamarotes(trechoId: Long, viagemId: Long): AppResult<Map<Long, List<Comodo>>>
    suspend fun listarComodosLivres(trechoId: Long, viagemId: Long): AppResult<List<ComodoLivre>>
    suspend fun reservarComodo(trechoId: Long, viagemId: Long, comodoId: Long): AppResult<Unit>
    suspend fun deletarReserva(trechoId: Long, viagemId: Long, comodoIds: List<Long>): AppResult<Unit>
    suspend fun iniciarVenda(
        trechoId: Long,
        viagemId: Long,
        tiposComodoEscolhidos: Map<Long, Int>,
        comodosAssentosEscolhidos: List<Long>,
    ): AppResult<InicioVenda>
}

data class InicioVenda(
    val trechoId: Long,
    val comodos: List<Comodo>,
    val formasPagamento: List<com.example.passagenexpress.core.domain.model.FormaPagamento>,
)
