package com.example.passagenexpress.core.domain.printer

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.BilheteMapeado

/**
 * Abstração da impressora térmica USB. A implementação (`:core:printer`) monta os bytes
 * ESC/POS e fala direto com o `UsbManager` nativo — sem SDK de terceiros. O alvo é uma
 * 58mm genérica (MobilePrinter/GEZHI), ESC/POS padrão.
 */
interface TicketPrinter {
    /** Dispositivos USB conectados (usado no setup pra escolher/configurar a impressora). */
    fun listDevices(): List<UsbPrinterDevice>

    /** Monta e imprime um bilhete. Resolve o device pelo VID/PID salvo na config. */
    suspend fun print(bilhete: BilheteMapeado): AppResult<Unit>

    /** Imprime um ticket de teste curto (botão "Testar impressão" no setup). */
    suspend fun testPrint(): AppResult<Unit>
}

data class UsbPrinterDevice(
    val vendorId: Int,
    val productId: Int,
    val name: String,
)
