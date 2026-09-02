package com.example.passagenexpress.feature.setup

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
import com.example.passagenexpress.core.domain.usecase.CompletarSetupUseCase
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
class SetupWizardViewModel @Inject constructor(
    private val totemConfigRepository: TotemConfigRepository,
    private val buscarPortos: BuscarPortosUseCase,
    private val buscarEmbarcacoes: BuscarEmbarcacoesUseCase,
    private val completarSetup: CompletarSetupUseCase,
    private val listarImpressoras: ListarImpressorasUseCase,
    private val testarImpressao: TestarImpressaoUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupWizardUiState())
    val uiState: StateFlow<SetupWizardUiState> = _uiState.asStateFlow()

    fun onSubdomainChange(value: String) {
        _uiState.update { it.copy(subdomainInput = value, errorMessage = null) }
    }

    /** Abre o teclado on-screen mirando um campo (totem não usa IME nativo). */
    fun onOpenKeypad(field: SetupKeypadField) {
        _uiState.update { it.copy(keypadField = field) }
    }

    fun onCloseKeypad() {
        _uiState.update { it.copy(keypadField = null) }
    }

    fun onKeypadChar(c: Char) {
        _uiState.update { state ->
            when (state.keypadField) {
                // Subdomínio é sempre minúsculo e sem espaço (regra de DNS/slug).
                SetupKeypadField.Subdomain ->
                    if (c == ' ') state
                    else state.copy(
                        subdomainInput = state.subdomainInput + c.lowercaseChar(),
                        errorMessage = null,
                    )
                SetupKeypadField.PortoSearch ->
                    state.copy(portoSearchQuery = state.portoSearchQuery + c)
                SetupKeypadField.EmbarcacaoSearch ->
                    state.copy(embarcacaoSearchQuery = state.embarcacaoSearchQuery + c)
                // PIN aceita só dígitos, com um teto defensivo de tamanho.
                SetupKeypadField.Pin ->
                    if (!c.isDigit() || state.pinInput.length >= MAX_PIN_LENGTH) state
                    else state.copy(pinInput = state.pinInput + c, errorMessage = null)
                null -> state
            }
        }
    }

    fun onKeypadBackspace() {
        _uiState.update { state ->
            when (state.keypadField) {
                SetupKeypadField.Subdomain ->
                    state.copy(subdomainInput = state.subdomainInput.dropLast(1))
                SetupKeypadField.PortoSearch ->
                    state.copy(portoSearchQuery = state.portoSearchQuery.dropLast(1))
                SetupKeypadField.EmbarcacaoSearch ->
                    state.copy(embarcacaoSearchQuery = state.embarcacaoSearchQuery.dropLast(1))
                SetupKeypadField.Pin ->
                    state.copy(pinInput = state.pinInput.dropLast(1))
                null -> state
            }
        }
    }

    fun onAdvanceFromSubdomain() {
        val state = _uiState.value
        if (!state.canAdvanceFromSubdomain) return
        viewModelScope.launch {
            totemConfigRepository.setSubdomain(state.subdomainInput.trim())
            _uiState.update {
                it.copy(step = SetupStep.Porto, portos = PortosState.Loading, keypadField = null)
            }
            loadPortos()
        }
    }

    fun onRetryPortos() {
        _uiState.update { it.copy(portos = PortosState.Loading) }
        viewModelScope.launch { loadPortos() }
    }

    fun onPortoSearchChange(query: String) {
        _uiState.update { it.copy(portoSearchQuery = query) }
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
        _uiState.update { it.copy(selectedPorto = porto) }
    }

    fun onAdvanceFromPorto() {
        val state = _uiState.value
        if (!state.canAdvanceFromPorto) return
        _uiState.update {
            it.copy(step = SetupStep.Embarcacao, embarcacoes = EmbarcacoesState.Loading, keypadField = null)
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

    /** Alterna a seleção — tocar de novo na embarcação selecionada desmarca (totem sem embarcação). */
    fun onEmbarcacaoSelected(embarcacao: Embarcacao) {
        _uiState.update {
            val novo = if (it.selectedEmbarcacao?.id == embarcacao.id) null else embarcacao
            it.copy(selectedEmbarcacao = novo)
        }
    }

    fun onAdvanceFromEmbarcacao() {
        _uiState.update { it.copy(step = SetupStep.Printer, keypadField = null) }
        refreshPrinters()
    }

    fun refreshPrinters() {
        val devices = listarImpressoras()
        _uiState.update { state ->
            // Mantém a seleção se o device ainda estiver presente.
            val keep = state.selectedPrinter?.takeIf { sel ->
                devices.any { it.vendorId == sel.vendorId && it.productId == sel.productId }
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

    fun onAdvanceFromPrinter() {
        _uiState.update { it.copy(step = SetupStep.Language) }
    }

    fun onLanguageSelected(language: AppLanguage) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun onAdvanceFromLanguage() {
        // Já abre o teclado numérico mirando o PIN — etapa final só tem esse campo.
        _uiState.update { it.copy(step = SetupStep.Pin, keypadField = SetupKeypadField.Pin) }
    }

    fun onBack() {
        _uiState.update { state ->
            val previous = when (state.step) {
                SetupStep.Subdomain -> SetupStep.Subdomain
                SetupStep.Porto -> SetupStep.Subdomain
                SetupStep.Embarcacao -> SetupStep.Porto
                SetupStep.Printer -> SetupStep.Embarcacao
                SetupStep.Language -> SetupStep.Printer
                SetupStep.Pin -> SetupStep.Language
            }
            state.copy(step = previous, errorMessage = null, keypadField = null)
        }
    }

    fun onFinish() {
        val state = _uiState.value
        if (!state.canFinish) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, keypadField = null) }
            runCatching {
                completarSetup(
                    subdomain = state.subdomainInput.trim(),
                    porto = state.selectedPorto,
                    language = state.selectedLanguage,
                    pin = state.pinInput,
                    embarcacaoId = state.selectedEmbarcacao?.id,
                    embarcacaoNome = state.selectedEmbarcacao?.nome.orEmpty(),
                    printerVendorId = state.selectedPrinter?.vendorId,
                    printerProductId = state.selectedPrinter?.productId,
                    printerPaperWidth = state.selectedPaperWidth,
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, completed = true) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSaving = false, errorMessage = throwable.message) }
            }
        }
    }

    private companion object {
        // Teto defensivo de dígitos do PIN no setup (mínimo vem de TotemConfig.MinPinLength).
        const val MAX_PIN_LENGTH = 8
    }
}
