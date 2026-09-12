package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.repository.EmpresaRepository
import com.example.passagenexpress.core.domain.repository.TotemConfigRepository
import javax.inject.Inject

/**
 * Busca `api/empresa/tema` pro subdomain configurado e persiste `is_super` — tema é opcional
 * (indisponível/erro não deve travar o setup), então falha vira `isSuper = false`.
 */
class SincronizarTemaUseCase @Inject constructor(
    private val empresaRepository: EmpresaRepository,
    private val totemConfigRepository: TotemConfigRepository,
) {
    suspend operator fun invoke() {
        val isSuper = (empresaRepository.buscarTema() as? AppResult.Success)?.value?.isSuper ?: false
        totemConfigRepository.setSuper(isSuper)
    }
}
