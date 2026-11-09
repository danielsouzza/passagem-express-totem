package com.example.passagenexpress.core.domain.model

/**
 * Modalidade de transporte que o totem vende. Um totem pode vender só barco, só ônibus ou os dois
 * (aí o "Começar" pergunta ao cliente). O fluxo hidroviário é o original; o rodoviário tem telas
 * próprias (`bus/...`) e não compartilha estado com ele.
 */
enum class Modalidade(val key: String) {
    Hidroviario("hidroviario"),
    Rodoviario("rodoviario"),
    ;

    companion object {
        fun fromKey(key: String): Modalidade? = entries.firstOrNull { it.key == key }
    }
}
