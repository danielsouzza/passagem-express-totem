package com.example.passagenexpress.feature.setup

import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.model.Embarcacao
import com.example.passagenexpress.core.domain.model.Modalidade
import com.example.passagenexpress.core.domain.model.onibus.LocalOnibus
import com.example.passagenexpress.core.domain.model.onibus.Terminal
import com.example.passagenexpress.core.domain.model.PaperWidth
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.model.TotemConfig
import com.example.passagenexpress.core.domain.printer.UsbPrinterDevice

data class SetupWizardUiState(
    val step: SetupStep = SetupStep.Subdomain,
    val subdomainInput: String = "",
    /** Modalidades vendidas pelo totem; define quais passos o wizard mostra. */
    val modalidades: Set<Modalidade> = setOf(Modalidade.Hidroviario),
    // Passo Terminal (rodoviário): busca por texto (≥3 letras) na API de terminais.
    val terminalQuery: String = "",
    val terminais: TerminaisState = TerminaisState.Idle,
    val selectedTerminal: LocalOnibus? = null,
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

    val vendeHidroviario: Boolean get() = Modalidade.Hidroviario in modalidades
    val vendeRodoviario: Boolean get() = Modalidade.Rodoviario in modalidades

    val canAdvanceFromModalidade: Boolean get() = modalidades.isNotEmpty()

    /** Rodoviário precisa do terminal de origem — é a origem fixa de toda busca de ônibus. */
    val canAdvanceFromTerminal: Boolean get() = selectedTerminal != null

    /** Passos do wizard conforme as modalidades escolhidas (Porto/Embarcação só pra barco, Terminal só pra ônibus). */
    val steps: List<SetupStep>
        get() = buildList {
            add(SetupStep.Subdomain)
            add(SetupStep.Modalidade)
            if (vendeHidroviario) {
                add(SetupStep.Porto)
                add(SetupStep.Embarcacao)
            }
            if (vendeRodoviario) add(SetupStep.Terminal)
            add(SetupStep.Printer)
            add(SetupStep.Language)
            add(SetupStep.Pin)
        }

    /**
     * PIN é obrigatório e precisa do mínimo de dígitos; além disso cada modalidade ativa precisa
     * da sua origem: barco → porto ou embarcação; ônibus → terminal.
     */
    val canFinish: Boolean
        get() = pinInput.length >= TotemConfig.MinPinLength &&
            modalidades.isNotEmpty() &&
            (!vendeHidroviario || hasPortoOrEmbarcacao) &&
            (!vendeRodoviario || selectedTerminal != null)

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

enum class SetupStep { Subdomain, Modalidade, Porto, Embarcacao, Terminal, Printer, Language, Pin }

/** Campos de texto do wizard que recebem entrada pelo teclado on-screen do totem. */
enum class SetupKeypadField { Subdomain, PortoSearch, EmbarcacaoSearch, TerminalSearch, Pin }

sealed interface TerminaisState {
    /** Ainda não buscou (menos de 3 letras digitadas). */
    data object Idle : TerminaisState
    data object Loading : TerminaisState
    data class Loaded(val terminais: List<Terminal>) : TerminaisState
    data class Error(val message: String) : TerminaisState
}

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
