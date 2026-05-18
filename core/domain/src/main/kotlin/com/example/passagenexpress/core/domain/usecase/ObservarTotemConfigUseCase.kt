package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.domain.model.TotemConfig
import com.example.passagenexpress.core.domain.repository.TotemConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservarTotemConfigUseCase @Inject constructor(
    private val totemConfigRepository: TotemConfigRepository,
) {
    operator fun invoke(): Flow<TotemConfig> = totemConfigRepository.config
}
