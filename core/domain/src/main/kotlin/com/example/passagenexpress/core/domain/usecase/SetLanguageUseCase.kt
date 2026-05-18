package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.repository.TotemConfigRepository
import javax.inject.Inject

class SetLanguageUseCase @Inject constructor(
    private val totemConfigRepository: TotemConfigRepository,
) {
    suspend operator fun invoke(language: AppLanguage) {
        totemConfigRepository.setLanguage(language)
    }
}
