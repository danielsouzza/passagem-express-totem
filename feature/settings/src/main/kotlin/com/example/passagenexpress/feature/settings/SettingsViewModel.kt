package com.example.passagenexpress.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.model.Embarcacao
import com.example.passagenexpress.core.domain.model.PaperWidth
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.printer.UsbPrinterDevice
import com.example.passagenexpress.core.domain.repository.TotemConfigRepository
import com.example.passagenexpress.core.domain.usecase.BuscarEmbarcacoesUseCase
import com.example.passagenexpress.core.domain.usecase.BuscarPortosUseCase
import com.example.passagenexpress.core.domain.usecase.ListarImpressorasUseCase
import com.example.passagenexpress.core.domain.usecase.TestarImpressaoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val totemConfigRepository: TotemConfigRepository,
    private val buscarPortos: BuscarPortosUseCase,
    private val buscarEmbarcacoes: BuscarEmbarcacoesUseCase,
    private val listarImpressoras: ListarImpressorasUseCase,
    private val testarImpressao: TestarImpressaoUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Observa a config para refletir sempre os valores persistidos no hub, mesmo após salvar.
        viewModelScope.launch {
            totemConfigRepository.config.collect { config ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        currentSubdomain = config.subdomain,
                        currentPortoNome = config.portoNome,
                        currentLanguage = config.defaultLanguage,
                        currentPrinterVendorId = config.printerVendorId,
                        currentPrinterProductId = config.printerProductId,
                        currentPrinterPaperWidth = config.printerPaperWidth,
                        currentOperatorPin = config.operatorPin,
                        currentEmbarcacaoId = config.embarcacaoId,
                        currentEmbarcacaoNome = config.embarcacaoNome,
                    )
                }
            }
        }
    }

    // --- PIN gate ---------------------------------------------------------

    fun onPinDigit(c: Char) {
        _uiState.update { state ->
            if (state.pinInput.length >= MAX_PIN_LENGTH) state
            else state.copy(pinInput = state.pinInput + c, pinError = false)
        }
    }

    fun onPinBackspace() {
        _uiState.update { it.copy(pinInput = it.pinInput.dropLast(1), pinError = false) }
    }

    fun onPinSubmit() {
        val state = _uiState.value
        // Valida contra o PIN definido no setup; totens antigos (sem PIN) caem no default de build.
        val expected = state.currentOperatorPin.ifEmpty { BuildConfig.SETTINGS_PIN }
        if (state.pinInput == expected) {
            _uiState.update { it.copy(unlocked = true, pinInput = "", pinError = false) }
        } else {
            _uiState.update { it.copy(pinInput = "", pinError = true) }
        }
    }

    // --- Navegação entre seções ------------------------------------------

    fun onEditSubdomain() {
        _uiState.update {
            it.copy(
                editing = SettingsField.Subdomain,
                subdomainInput = it.currentSubdomain,
                keypadField = null,
                savedNotice = null,
            )
        }
    }

    fun onEditPorto() {
        _uiState.update {
            it.copy(
                editing = SettingsField.Porto,
                portoSearchQuery = "",
                selectedPorto = null,
                portoCleared = false,
                portos = PortosState.Loading,
                keypadField = null,
                savedNotice = null,
            )
        }
        viewModelScope.launch { loadPortos() }
    }

    fun onEditEmbarcacao() {
        _uiState.update {
            // Pré-seleciona a embarcação atual (se houver) pra destacar na lista ao abrir.
            val preselected = it.currentEmbarcacaoId?.let { id ->
                Embarcacao(id = id, nome = it.currentEmbarcacaoNome)
            }
            it.copy(
                editing = SettingsField.Embarcacao,
                embarcacaoSearchQuery = "",
                selectedEmbarcacao = preselected,
                embarcacoes = EmbarcacoesState.Loading,
                keypadField = null,
                savedNotice = null,
            )
        }
        viewModelScope.launch { loadEmbarcacoes() }
    }

    fun onRetryEmbarcacoes() {
        _uiState.update { it.copy(embarcacoes = EmbarcacoesState.Loading) }
        viewModelScope.launch { loadEmbarcacoes() }
    }

    fun onEmbarcacaoSearchChange(query: String) {
        _uiState.update { it.copy(embarcacaoSearchQuery = query) }
    }

    private suspend fun loadEmbarcacoes() {
        when (val result = buscarEmbarcacoes()) {
            is AppResult.Success ->
                _uiState.update { it.copy(embarcacoes = EmbarcacoesState.Loaded(result.value)) }
            is AppResult.Failure -> _uiState.update {
                it.copy(embarcacoes = EmbarcacoesState.Error(result.error.message ?: "Erro desconhecido"))
            }
        }
    }

    fun onEmbarcacaoSelected(embarcacao: Embarcacao) {
        _uiState.update { it.copy(selectedEmbarcacao = embarcacao) }
    }

    /** "Nenhuma": desvincula a embarcação (totem em terra). */
    fun onEmbarcacaoCleared() {
        _uiState.update { it.copy(selectedEmbarcacao = null) }
    }

    fun onSaveEmbarcacao() {
        val selected = _uiState.value.selectedEmbarcacao
        viewModelScope.launch {
            totemConfigRepository.setEmbarcacao(selected?.id, selected?.nome.orEmpty())
            _uiState.update { it.copy(editing = null, keypadField = null, savedNotice = SettingsField.Embarcacao) }
        }
    }

    fun onEditPrinter() {
        _uiState.update {
            it.copy(
                editing = SettingsField.Printer,
                selectedPaperWidth = it.currentPrinterPaperWidth,
                printerTest = PrinterTestState.Idle,
                savedNotice = null,
            )
        }
        refreshPrinters()
    }

    fun onEditLanguage() {
        _uiState.update {
            it.copy(editing = SettingsField.Language, languageDraft = it.currentLanguage, savedNotice = null)
        }
    }

    fun onEditPin() {
        _uiState.update {
            it.copy(editing = SettingsField.Pin, pinDraft = "", keypadField = null, savedNotice = null)
        }
    }

    fun onCancelEdit() {
        _uiState.update { it.copy(editing = null, keypadField = null) }
    }

    // --- Editor de PIN ----------------------------------------------------

    fun onPinDraftDigit(c: Char) {
        _uiState.update { state ->
            if (!c.isDigit() || state.pinDraft.length >= MAX_PIN_LENGTH) state
            else state.copy(pinDraft = state.pinDraft + c)
        }
    }

    fun onPinDraftBackspace() {
        _uiState.update { it.copy(pinDraft = it.pinDraft.dropLast(1)) }
    }

    fun onSavePin() {
        val state = _uiState.value
        if (!state.canSavePin) return
        viewModelScope.launch {
            totemConfigRepository.setOperatorPin(state.pinDraft)
            _uiState.update { it.copy(editing = null, pinDraft = "", savedNotice = SettingsField.Pin) }
        }
    }

    // --- Teclado on-screen ------------------------------------------------

    fun onOpenKeypad(field: SettingsKeypadField) {
        _uiState.update { it.copy(keypadField = field) }
    }

    fun onCloseKeypad() {
        _uiState.update { it.copy(keypadField = null) }
    }

    fun onKeypadChar(c: Char) {
        _uiState.update { state ->
            when (state.keypadField) {
                // Subdomínio é sempre minúsculo e sem espaço (regra de DNS/slug).
                SettingsKeypadField.Subdomain ->
                    if (c == ' ') state
                    else state.copy(subdomainInput = state.subdomainInput + c.lowercaseChar())
                SettingsKeypadField.PortoSearch ->
                    state.copy(portoSearchQuery = state.portoSearchQuery + c)
                SettingsKeypadField.EmbarcacaoSearch ->
                    state.copy(embarcacaoSearchQuery = state.embarcacaoSearchQuery + c)
                null -> state
            }
        }
    }

    fun onKeypadBackspace() {
        _uiState.update { state ->
            when (state.keypadField) {
                SettingsKeypadField.Subdomain ->
                    state.copy(subdomainInput = state.subdomainInput.dropLast(1))
                SettingsKeypadField.PortoSearch ->
                    state.copy(portoSearchQuery = state.portoSearchQuery.dropLast(1))
                SettingsKeypadField.EmbarcacaoSearch ->
                    state.copy(embarcacaoSearchQuery = state.embarcacaoSearchQuery.dropLast(1))
                null -> state
            }
        }
    }

    fun onPortoSearchChange(query: String) {
        _uiState.update { it.copy(portoSearchQuery = query) }
    }

    // --- Porto ------------------------------------------------------------

    fun onRetryPortos() {
        _uiState.update { it.copy(portos = PortosState.Loading) }
        viewModelScope.launch { loadPortos() }
    }

    private suspend fun loadPortos() {
        when (val result = buscarPortos()) {
            is AppResult.Success -> _uiState.update { it.copy(portos = PortosState.Loaded(result.value)) }
            is AppResult.Failure -> _uiState.update {
                it.copy(portos = PortosState.Error(result.error.message ?: "Erro desconhecido"))
            }
        }
    }

    fun onPortoSelected(porto: Porto) {
        _uiState.update { it.copy(selectedPorto = porto, portoCleared = false) }
    }

    /** "Nenhum": desvincula o porto (totem sem porto fixo — filtra viagens pela embarcação). */
    fun onPortoCleared() {
        _uiState.update { it.copy(selectedPorto = null, portoCleared = true) }
    }

    // --- Impressora -------------------------------------------------------

    fun refreshPrinters() {
        val devices = listarImpressoras()
        _uiState.update { state ->
            // Mantém a seleção atual; senão tenta a impressora já configurada; senão única detectada.
            val keep = state.selectedPrinter?.takeIf { sel ->
                devices.any { it.vendorId == sel.vendorId && it.productId == sel.productId }
            } ?: devices.firstOrNull {
                it.vendorId == state.currentPrinterVendorId && it.productId == state.currentPrinterProductId
            } ?: devices.singleOrNull()
            state.copy(printers = devices, selectedPrinter = keep)
        }
    }

    fun onPrinterSelected(device: UsbPrinterDevice) {
        _uiState.update { it.copy(selectedPrinter = device, printerTest = PrinterTestState.Idle) }
    }

    fun onPaperWidthSelected(width: PaperWidth) {
        _uiState.update { it.copy(selectedPaperWidth = width, printerTest = PrinterTestState.Idle) }
    }

    fun onTestPrint() {
        if (_uiState.value.printerTest is PrinterTestState.Testing) return
        viewModelScope.launch {
            // Persiste a escolha antes do teste pra que a impressora resolva pelo VID/PID + largura.
            totemConfigRepository.setPrinterPaperWidth(_uiState.value.selectedPaperWidth)
            _uiState.value.selectedPrinter?.let {
                totemConfigRepository.setPrinter(it.vendorId, it.productId)
            }
            _uiState.update { it.copy(printerTest = PrinterTestState.Testing) }
            val result = testarImpressao()
            _uiState.update {
                it.copy(
                    printerTest = when (result) {
                        is AppResult.Success -> PrinterTestState.Success
                        is AppResult.Failure -> PrinterTestState.Error(
                            result.error.message ?: "Falha ao imprimir o teste",
                        )
                    },
                )
            }
        }
    }

    fun onLanguageSelected(language: AppLanguage) {
        _uiState.update { it.copy(languageDraft = language) }
    }

    // --- Salvar (persistência granular) -----------------------------------

    fun onSaveSubdomain() {
        val value = _uiState.value.subdomainInput.trim()
        viewModelScope.launch {
            totemConfigRepository.setSubdomain(value)
            _uiState.update { it.copy(editing = null, keypadField = null, savedNotice = SettingsField.Subdomain) }
        }
    }

    fun onSavePorto() {
        val state = _uiState.value
        val porto = state.selectedPorto
        if (porto == null && !state.portoCleared) return
        viewModelScope.launch {
            if (porto != null) totemConfigRepository.setPorto(porto) else totemConfigRepository.clearPorto()
            _uiState.update { it.copy(editing = null, keypadField = null, savedNotice = SettingsField.Porto) }
        }
    }

    fun onSavePrinter() {
        val printer = _uiState.value.selectedPrinter ?: return
        val paperWidth = _uiState.value.selectedPaperWidth
        viewModelScope.launch {
            totemConfigRepository.setPrinter(printer.vendorId, printer.productId)
            totemConfigRepository.setPrinterPaperWidth(paperWidth)
            _uiState.update { it.copy(editing = null, savedNotice = SettingsField.Printer) }
        }
    }

    fun onSaveLanguage() {
        val language = _uiState.value.languageDraft
        viewModelScope.launch {
            totemConfigRepository.setLanguage(language)
            _uiState.update { it.copy(editing = null, savedNotice = SettingsField.Language) }
        }
    }

    private companion object {
        // Limite defensivo de dígitos do PIN no campo (o PIN real vem de BuildConfig.SETTINGS_PIN).
        const val MAX_PIN_LENGTH = 12
    }
}
