package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.printer.TicketPrinter
import javax.inject.Inject

class TestarImpressaoUseCase @Inject constructor(
    private val ticketPrinter: TicketPrinter,
) {
    suspend operator fun invoke(): AppResult<Unit> = ticketPrinter.testPrint()
}
