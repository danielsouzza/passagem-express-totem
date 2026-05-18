package com.example.passagenexpress.feature.room

import com.example.passagenexpress.core.domain.model.Comodo
import com.example.passagenexpress.core.domain.model.ComodoLivre
import com.example.passagenexpress.core.domain.model.TipoComodo
import com.example.passagenexpress.core.domain.model.Trecho
import com.example.passagenexpress.core.domain.repository.InicioVenda

data class RoomUiState(
    val trecho: Trecho? = null,
    val content: RoomContentState = RoomContentState.Loading,
    val selecionados: SelecaoState = SelecaoState(),
    val reservaInFlight: Boolean = false,
    val erroTransitorio: String? = null,
    val saleStarted: InicioVenda? = null,
    val startingSale: Boolean = false,
)

sealed interface RoomContentState {
    data object Loading : RoomContentState

    /** Modo poltronas (id=1): grid de assentos individuais. */
    data class Poltronas(
        val assentos: List<Comodo>,
        val tiposLivres: List<TipoComodoBucket>,
    ) : RoomContentState

    /** Modo camarotes (id=4): cards individuais. */
    data class Camarotes(
        val camarotes: List<Comodo>,
        val tiposLivres: List<TipoComodoBucket>,
    ) : RoomContentState

    /** Modo poltrona livre / outros tipos: contadores por tipo. */
    data class Livres(
        val tipos: List<TipoComodoBucket>,
    ) : RoomContentState

    data class Error(val message: String) : RoomContentState
}

data class TipoComodoBucket(
    val tipo: TipoComodo,
    val livre: ComodoLivre,
)

data class SelecaoState(
    /** Cômodos individuais reservados (poltronas/camarotes). */
    val porId: List<Comodo> = emptyList(),
    /** Quantidade selecionada por tipo (free-seating). */
    val porTipo: Map<Long, Int> = emptyMap(),
) {
    fun isEmpty(): Boolean = porId.isEmpty() && porTipo.values.sum() == 0
}
