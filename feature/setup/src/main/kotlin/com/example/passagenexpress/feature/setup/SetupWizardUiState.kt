package com.example.passagenexpress.feature.setup

import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.model.Embarcacao
import com.example.passagenexpress.core.domain.model.PaperWidth
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.model.TotemConfig
import com.example.passagenexpress.core.domain.printer.UsbPrinterDevice

data class SetupWizardUiState(
    val step: SetupStep = SetupStep.Subdomain,
    val subdomainInput: String = "",
    val portos: PortosState = PortosState.Idle,
    val portoSearchQuery: String = "",
    val selectedPorto: Porto? = null,
    val embarcacoes: EmbarcacoesState = EmbarcacoesState.Idle,
    val embarcacaoSearchQuery: String = "",
    val selectedEmbarcacao: Embarcacao? = null,
    val printers: List<UsbPrinterDevice> = emptyList(),
    val selectedPrinter: UsbPrinterDevice? = null,
    val selectedPaperWidth: PaperWidth = PaperWidth.MM58,
    val printerTest: PrinterTestState = PrinterTestState.Idle,
    val selectedLanguage: AppLanguage = AppLanguage.PtBr,
    /** PIN de acesso às configurações, definido pelo operador no setup (somente dígitos). */
    val pinInput: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val completed: Boolean = false,
    /** Campo cujo teclado on-screen está aberto (null = nenhum). Totem não usa IME nativo. */
    val keypadField: SetupKeypadField? = null,
) {
    /** Subdomain é opcional — totem padrão (sem multi-tenant) avança em branco. */
    val canAdvanceFromSubdomain: Boolean get() = true
    /** Porto é opcional — totem a bordo de embarcação avança sem porto (filtra por embarcação). */
    val canAdvanceFromPorto: Boolean get() = true
    /** Embarcação é opcional — totem fora de embarcação avança sem selecionar. */
    val canAdvanceFromEmbarcacao: Boolean get() = true
    /** Impressora é opcional — dá pra pular (fallback: auto-detecta a classe Printer). */
    val canAdvanceFromPrinter: Boolean get() = true
    val canAdvanceFromLanguage: Boolean get() = true

    /**
     * O totem precisa de ao menos um filtro de origem: porto (totem fixo em terra) OU embarcação
     * (totem a bordo). Sem nenhum dos dois, não há como listar viagens.
     */
    val hasPortoOrEmbarcacao: Boolean get() = selectedPorto != null || selectedEmbarcacao != null

    /** PIN é obrigatório e precisa do mínimo de dígitos; além disso exige porto ou embarcação. */
    val canFinish: Boolean
        get() = pinInput.length >= TotemConfig.MinPinLength && hasPortoOrEmbarcacao

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

    /** Lista filtrada de embarcações (case-insensitive em nome + empresa). */
    val filteredEmbarcacoes: List<Embarcacao>
        get() = (embarcacoes as? EmbarcacoesState.Loaded)?.embarcacoes?.let { all ->
            val query = embarcacaoSearchQuery.trim()
            if (query.isEmpty()) all
            else all.filter { e ->
                e.nome.contains(query, ignoreCase = true) ||
                    e.empresaNome.contains(query, ignoreCase = true)
            }
        } ?: emptyList()
}

enum class SetupStep { Subdomain, Porto, Embarcacao, Printer, Language, Pin }

/** Campos de texto do wizard que recebem entrada pelo teclado on-screen do totem. */
enum class SetupKeypadField { Subdomain, PortoSearch, EmbarcacaoSearch, Pin }

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

sealed interface EmbarcacoesState {
    data object Idle : EmbarcacoesState
    data object Loading : EmbarcacoesState
    data class Loaded(val embarcacoes: List<Embarcacao>) : EmbarcacoesState
    data class Error(val message: String) : EmbarcacoesState
}
