package com.example.passagenexpress.feature.setup

import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.printer.UsbPrinterDevice

data class SetupWizardUiState(
    val step: SetupStep = SetupStep.Subdomain,
    val subdomainInput: String = "",
    val portos: PortosState = PortosState.Idle,
    val portoSearchQuery: String = "",
    val selectedPorto: Porto? = null,
    val printers: List<UsbPrinterDevice> = emptyList(),
    val selectedPrinter: UsbPrinterDevice? = null,
    val printerTest: PrinterTestState = PrinterTestState.Idle,
    val selectedLanguage: AppLanguage = AppLanguage.PtBr,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val completed: Boolean = false,
) {
    /** Subdomain é opcional — totem padrão (sem multi-tenant) avança em branco. */
    val canAdvanceFromSubdomain: Boolean get() = true
    val canAdvanceFromPorto: Boolean get() = selectedPorto != null
    /** Impressora é opcional — dá pra pular (fallback: auto-detecta a classe Printer). */
    val canAdvanceFromPrinter: Boolean get() = true

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

enum class SetupStep { Subdomain, Porto, Printer, Language }

sealed interface PrinterTestState {
    data object Idle : PrinterTestState
    data object Testing : PrinterTestState
    data object Success : PrinterTestState
    data class Error(val message: String) : PrinterTestState
}

sealed interface PortosState {
    data object Idle : PortosState
    data object Loading : PortosState
    data class Loaded(val portos: List<Porto>) : PortosState
    data class Error(val message: String) : PortosState
}
