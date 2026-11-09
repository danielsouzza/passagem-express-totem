package com.example.passagenexpress.core.domain.model.onibus

/**
 * Terminal rodoviário (`GET api/onibus/terminais`). Cada item vira até duas opções de escolha,
 * como no site (`CardFilter.vue`): o terminal específico ([slug]) e "Todos os terminais de
 * {cidade}" ([slugTodasEstacoes]). O slug escolhido é o que vai em `from`/`to` na busca.
 */
data class Terminal(
    val id: Long,
    val nome: String,
    val uf: String,
    val cidade: String,
    /** Nome do terminal específico (ex.: "São Paulo - Rodoviária do Tietê"); null = item agregado. */
    val terminal: String?,
    val slug: String,
    val slugTodasEstacoes: String,
)

/** Uma opção selecionável de origem/destino: o slug que vai para a API + o rótulo exibido. */
data class LocalOnibus(
    val slug: String,
    val nome: String,
)

/**
 * Achata a lista de terminais nas opções exibidas, sem repetir slugs — mesma regra do site:
 * "Todos os terminais de {cidade}" (uma vez por cidade) + "{cidade} - {terminal}".
 */
fun List<Terminal>.toOpcoes(todosLabel: (cidade: String, uf: String) -> String): List<LocalOnibus> {
    val vistos = mutableSetOf<String>()
    val opcoes = mutableListOf<LocalOnibus>()
    for (t in this) {
        if (t.slugTodasEstacoes.isNotBlank() && vistos.add(t.slugTodasEstacoes)) {
            opcoes += LocalOnibus(t.slugTodasEstacoes, todosLabel(t.cidade, t.uf))
        }
        val nomeTerminal = t.terminal?.takeIf { it.isNotBlank() }
        if (nomeTerminal != null && vistos.add(t.slug)) {
            opcoes += LocalOnibus(t.slug, nomeTerminal)
        }
    }
    return opcoes
}
