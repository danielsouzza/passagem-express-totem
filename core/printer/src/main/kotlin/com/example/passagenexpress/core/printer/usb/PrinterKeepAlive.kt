package com.example.passagenexpress.core.printer.usb

import com.example.passagenexpress.core.domain.printer.TicketPrinter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mantém a impressora térmica acordada enquanto o totem está ligado: a cada [INTERVAL_MS] dispara
 * um ping de keep-alive ([TicketPrinter.keepAlive]) — uma consulta de status que não imprime papel.
 *
 * Iniciado uma vez no `TotemApplication.onCreate()`. O escopo é preso ao processo do app (o totem
 * fica em foreground/kiosk com a tela ligada), então roda enquanto o app vive. Cada ping é
 * best-effort e silencioso; se não houver impressora pronta, não faz nada.
 */
@Singleton
class PrinterKeepAlive @Inject constructor(
    private val ticketPrinter: TicketPrinter,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)

    /** Inicia o ticker. Idempotente — chamadas repetidas são ignoradas. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            while (isActive) {
                delay(INTERVAL_MS)
                ticketPrinter.keepAlive()
            }
        }
    }

    private companion object {
        const val INTERVAL_MS = 60_000L
    }
}
