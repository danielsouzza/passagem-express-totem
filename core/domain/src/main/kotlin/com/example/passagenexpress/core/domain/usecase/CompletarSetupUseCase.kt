package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.model.PaperWidth
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.repository.TotemConfigRepository
import javax.inject.Inject

class CompletarSetupUseCase @Inject constructor(
    private val totemConfigRepository: TotemConfigRepository,
) {
    suspend operator fun invoke(
        subdomain: String,
        porto: Porto?,
        language: AppLanguage,
        pin: String,
        embarcacaoId: Long? = null,
        embarcacaoNome: String = "",
        printerVendorId: Int? = null,
        printerProductId: Int? = null,
        printerPaperWidth: PaperWidth = PaperWidth.MM58,
    ) {
        totemConfigRepository.setSubdomain(subdomain)
        if (porto != null) totemConfigRepository.setPorto(porto)
        totemConfigRepository.setEmbarcacao(embarcacaoId, embarcacaoNome)
        if (printerVendorId != null && printerProductId != null) {
            totemConfigRepository.setPrinter(printerVendorId, printerProductId)
        }
        totemConfigRepository.setPrinterPaperWidth(printerPaperWidth)
        totemConfigRepository.setLanguage(language)
        totemConfigRepository.setOperatorPin(pin)
        totemConfigRepository.completeSetup()
    }
}
