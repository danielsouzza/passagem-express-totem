package com.example.passagenexpress.core.domain.repository

import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.model.TotemConfig
import kotlinx.coroutines.flow.Flow

interface TotemConfigRepository {
    val config: Flow<TotemConfig>
    suspend fun setSubdomain(subdomain: String)
    suspend fun setPorto(porto: Porto)
    suspend fun setPrinter(vendorId: Int, productId: Int)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun completeSetup()
    suspend fun resetSetup()
}
