package com.example.passagenexpress.core.data.remote.dto.onibus

import com.example.passagenexpress.core.domain.model.onibus.Terminal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Item de `GET api/onibus/terminais`. Exemplo real:
 * `{"id":4017,"nome":"Sao Paulo, SP - Tiete","uf":"SP","cidade":"São Paulo",
 *   "terminal":"São Paulo - Rodoviária do Tietê","slug":"sao-paulo-tiete-sp",
 *   "slug_todas_estacoes":"sao-paulo-sp-todos"}` — `terminal` vem null no item agregado.
 */
@Serializable
data class TerminalDto(
    val id: Long = 0,
    val nome: String? = null,
    val uf: String? = null,
    val cidade: String? = null,
    val terminal: String? = null,
    val slug: String? = null,
    @SerialName("slug_todas_estacoes") val slugTodasEstacoes: String? = null,
)

fun TerminalDto.toDomain(): Terminal = Terminal(
    id = id,
    nome = nome.orEmpty(),
    uf = uf.orEmpty(),
    cidade = cidade.orEmpty().ifEmpty { nome.orEmpty() },
    terminal = terminal,
    slug = slug.orEmpty(),
    slugTodasEstacoes = slugTodasEstacoes.orEmpty(),
)
