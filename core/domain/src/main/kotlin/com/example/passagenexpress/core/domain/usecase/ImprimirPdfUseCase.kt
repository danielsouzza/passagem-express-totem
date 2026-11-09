package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.printer.TicketPrinter
import javax.inject.Inject

/** Imprime um documento PDF pronto (boarding pass de ônibus). */
class ImprimirPdfUseCase @Inject constructor(
    private val printer: TicketPrinter,
) {
    suspend operator fun invoke(pdf: ByteArray): AppResult<Unit> = printer.printPdf(pdf)
}
