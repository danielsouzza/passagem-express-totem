package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.domain.printer.TicketPrinter
import com.example.passagenexpress.core.domain.printer.UsbPrinterDevice
import javax.inject.Inject

class ListarImpressorasUseCase @Inject constructor(
    private val ticketPrinter: TicketPrinter,
) {
    operator fun invoke(): List<UsbPrinterDevice> = ticketPrinter.listDevices()
}
