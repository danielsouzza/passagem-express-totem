package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.BilheteMapeado
import com.example.passagenexpress.core.domain.printer.TicketPrinter
import javax.inject.Inject

class ImprimirBilheteUseCase @Inject constructor(
    private val ticketPrinter: TicketPrinter,
) {
    suspend operator fun invoke(bilhete: BilheteMapeado): AppResult<Unit> =
        ticketPrinter.print(bilhete)
}
