package com.example.passagenexpress.core.data.repository

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.data.remote.api.EmpresaApi
import com.example.passagenexpress.core.data.remote.callEnvelope
import com.example.passagenexpress.core.data.remote.dto.toDomain
import com.example.passagenexpress.core.domain.model.Tema
import com.example.passagenexpress.core.domain.repository.EmpresaRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmpresaRepositoryImpl @Inject constructor(
    private val api: EmpresaApi,
) : EmpresaRepository {

    override suspend fun buscarTema(): AppResult<Tema> =
        callEnvelope({ api.getTema() }) { it.toDomain() }
}
