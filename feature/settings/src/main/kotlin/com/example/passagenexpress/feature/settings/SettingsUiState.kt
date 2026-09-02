package com.example.passagenexpress.feature.settings

import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.model.Embarcacao
import com.example.passagenexpress.core.domain.model.PaperWidth
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.model.TotemConfig
import com.example.passagenexpress.core.domain.printer.UsbPrinterDevice

data class SettingsUiState(
    val loading: Boolean = true,
    /** Gate de PIN: enquanto false, só a tela de PIN é exibida. */
    val unlocked: Boolean = false,
    val pinInput: String = "",
    val pinError: Boolean = false,
    // Valores atualmente persistidos (exibidos no hub).
    val currentSubdomain: String = "",
    val currentPortoNome: String = "",
    val currentLanguage: AppLanguage = AppLanguage.PtBr,
    val currentPrinterVendorId: Int? = null,
    val currentPrinterProductId: Int? = null,
    val currentPrinterPaperWidth: PaperWidth = PaperWidth.MM58,
    /** PIN de acesso salvo; vazio = não definido (totem antigo, usa o fallback de build). */
    val currentOperatorPin: String = "",
    val currentEmbarcacaoId: Long? = null,
    val currentEmbarcacaoNome: String = "",
    // Seção em edição (null = hub). Cada editor tem seu próprio buffer abaixo.
    val editing: SettingsField? = null,
    /** Última seção salva com sucesso — mostra confirmação no hub. */
    val savedNotice: SettingsField? = null,
    // Editor de subdomínio.
    val subdomainInput: String = "",
    // Editor de porto.
    val portos: PortosState = PortosState.Idle,
    val portoSearchQuery: String = "",
    val selectedPorto: Porto? = null,
    /** True quando o operador escolheu "Nenhum" (desvincular o porto). */
    val portoCleared: Boolean = false,
    // Editor de embarcação (selectedEmbarcacao null = "Nenhuma"/totem em terra).
    val embarcacoes: EmbarcacoesState = EmbarcacoesState.Idle,
    val embarcacaoSearchQuery: String = "",
    val selectedEmbarcacao: Embarcacao? = null,
    // Editor de impressora.
    val printers: List<UsbPrinterDevice> = emptyList(),
    val selectedPrinter: UsbPrinterDevice? = null,
    val selectedPaperWidth: PaperWidth = PaperWidth.MM58,
    val printerTest: PrinterTestState = PrinterTestState.Idle,
    // Editor de idioma.
    val languageDraft: AppLanguage = AppLanguage.PtBr,
    // Editor de PIN (novo PIN sendo digitado).
    val pinDraft: String = "",
    /** Campo cujo teclado on-screen está aberto (totem não usa IME nativo). */
    val keypadField: SettingsKeypadField? = null,
) {
    val canSavePorto: Boolean get() = selectedPorto != null || portoCleared
    val canSavePrinter: Boolean get() = selectedPrinter != null
    val canSavePin: Boolean get() = pinDraft.length >= TotemConfig.MinPinLength

    /** Rótulo da impressora configurada (VID/PID) ou vazio quando não definida. */
    val currentPrinterLabel: String
        get() = if (currentPrinterVendorId != null && currentPrinterProductId != null) {
            "VID $currentPrinterVendorId · PID $currentPrinterProductId"
        } else {
            ""
        }

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

/** Seções editáveis nas configurações. */
enum class SettingsField { Subdomain, Porto, Embarcacao, Printer, Language, Pin }

/** Campos de texto que recebem entrada pelo teclado on-screen do totem. */
enum class SettingsKeypadField { Subdomain, PortoSearch, EmbarcacaoSearch }

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
