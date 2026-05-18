package com.example.passagenexpress.feature.city

import com.example.passagenexpress.core.domain.model.Municipio

data class CityUiState(
    val portoNome: String = "",
    val municipioOrigemNome: String = "",
    val destinos: MunicipiosState = MunicipiosState.Loading,
    val destinoSelecionada: Municipio? = null,
    val destinoSearch: String = "",
)

sealed interface MunicipiosState {
    data object Loading : MunicipiosState
    data class Loaded(val municipios: List<Municipio>) : MunicipiosState
    data class Error(val message: String) : MunicipiosState
}

fun MunicipiosState.filter(query: String): MunicipiosState = when (this) {
    is MunicipiosState.Loaded -> {
        val q = query.trim()
        if (q.isEmpty()) this
        else MunicipiosState.Loaded(
            municipios.filter { m ->
                m.nome.contains(q, ignoreCase = true) ||
                    (m.uf?.contains(q, ignoreCase = true) == true)
            }
        )
    }
    else -> this
}
