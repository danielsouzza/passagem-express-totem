package com.example.passagenexpress.core.common.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Escopo com tempo de vida do processo para trabalho best-effort que precisa **sobreviver ao
 * cancelamento de escopos de tela**.
 *
 * Caso de uso principal: liberar reservas de cômodo quando o usuário sai da tela (voltar, cancelar,
 * início ou back de hardware). Nesses casos o `viewModelScope` já foi cancelado pela navegação, e
 * uma chamada de rede lançada nele seria abortada antes de completar. Fire-and-forget: falhas não
 * sobem à UI, pois o usuário já está saindo.
 */
val AppScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
