package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.repository.TotemConfigRepository
import javax.inject.Inject

class CompletarSetupUseCase @Inject constructor(
    private val totemConfigRepository: TotemConfigRepository,
) {
    suspend operator fun invoke(
        subdomain: String,
        porto: Porto,
        language: AppLanguage,
    ) {
        totemConfigRepository.setSubdomain(subdomain)
        totemConfigRepository.setPorto(porto)
        totemConfigRepository.setLanguage(language)
        totemConfigRepository.completeSetup()
    }
}
