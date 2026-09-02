package com.example.passagenexpress.core.domain.repository

import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.model.PaperWidth
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.model.TotemConfig
import kotlinx.coroutines.flow.Flow

interface TotemConfigRepository {
    val config: Flow<TotemConfig>
    suspend fun setSubdomain(subdomain: String)
    suspend fun setPorto(porto: Porto)
    /** Limpa o porto vinculado (totem sem porto fixo — filtra viagens só pela embarcação). */
    suspend fun clearPorto()
    /** Define (ou limpa, com id null) a embarcação vinculada ao totem. */
    suspend fun setEmbarcacao(id: Long?, nome: String)
    suspend fun setPrinter(vendorId: Int, productId: Int)
    /** Largura da bobina da impressora (58mm/80mm) — adapta o layout do bilhete. */
    suspend fun setPrinterPaperWidth(paperWidth: PaperWidth)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setOperatorPin(pin: String)
    suspend fun completeSetup()
    suspend fun resetSetup()
}
