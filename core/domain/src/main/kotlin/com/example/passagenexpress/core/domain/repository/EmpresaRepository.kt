package com.example.passagenexpress.core.domain.repository

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.Tema

interface EmpresaRepository {
    suspend fun buscarTema(): AppResult<Tema>
}
