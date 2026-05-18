package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.Passageiro
import com.example.passagenexpress.core.domain.model.TipoDocumento
import com.example.passagenexpress.core.domain.repository.ViagemRepository
import javax.inject.Inject

/**
 * Consulta o backend em `/api/filtros/get-passageiro?tipo=<id>&doc=<numero>`.
 * Retorna `Success(null)` quando o backend não acha o passageiro.
 */
class BuscarPassageiroUseCase @Inject constructor(
    private val viagemRepository: ViagemRepository,
) {
    suspend operator fun invoke(tipo: TipoDocumento, doc: String): AppResult<Passageiro?> =
        viagemRepository.buscarPassageiro(tipo, doc)
}
