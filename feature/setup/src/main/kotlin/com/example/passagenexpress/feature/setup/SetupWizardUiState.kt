package com.example.passagenexpress.feature.setup

import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.model.Porto

data class SetupWizardUiState(
    val step: SetupStep = SetupStep.Subdomain,
    val subdomainInput: String = "",
    val portos: PortosState = PortosState.Idle,
    val portoSearchQuery: String = "",
    val selectedPorto: Porto? = null,
    val selectedLanguage: AppLanguage = AppLanguage.PtBr,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val completed: Boolean = false,
) {
    /** Subdomain é opcional — totem padrão (sem multi-tenant) avança em branco. */
    val canAdvanceFromSubdomain: Boolean get() = true
    val canAdvanceFromPorto: Boolean get() = selectedPorto != null

    /** Lista filtrada de portos (case-insensitive em nome + município). */
    val filteredPortos: List<Porto>
        get() = (portos as? PortosState.Loaded)?.portos?.let { all ->
            val query = portoSearchQuery.trim()
            if (query.isEmpty()) all
            else all.filter { porto ->
                porto.nome.contains(query, ignoreCase = true) ||
                    porto.municipioNome.contains(query, ignoreCase = true)
            }
        } ?: emptyList()
}

enum class SetupStep { Subdomain, Porto, Language }

sealed interface PortosState {
    data object Idle : PortosState
    data object Loading : PortosState
    data class Loaded(val portos: List<Porto>) : PortosState
    data class Error(val message: String) : PortosState
}
