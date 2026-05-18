package com.example.passagenexpress.core.domain.usecase

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.Municipio
import com.example.passagenexpress.core.domain.repository.ViagemRepository
import javax.inject.Inject

class BuscarMunicipiosDestinoUseCase @Inject constructor(
    private val viagemRepository: ViagemRepository,
) {
    suspend operator fun invoke(
        portoSlug: String?,
        municipioOrigemCodigo: String?,
    ): AppResult<List<Municipio>> =
        viagemRepository.buscarMunicipiosDestino(portoSlug, municipioOrigemCodigo)
}
