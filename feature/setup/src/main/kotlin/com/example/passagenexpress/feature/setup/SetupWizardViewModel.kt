package com.example.passagenexpress.feature.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.printer.UsbPrinterDevice
import com.example.passagenexpress.core.domain.repository.TotemConfigRepository
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
    private val completarSetup: CompletarSetupUseCase,
    private val listarImpressoras: ListarImpressorasUseCase,
    private val testarImpressao: TestarImpressaoUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupWizardUiState())
    val uiState: StateFlow<SetupWizardUiState> = _uiState.asStateFlow()

    fun onSubdomainChange(value: String) {
        _uiState.update { it.copy(subdomainInput = value, errorMessage = null) }
    }

    fun onAdvanceFromSubdomain() {
        val state = _uiState.value
        if (!state.canAdvanceFromSubdomain) return
        viewModelScope.launch {
            totemConfigRepository.setSubdomain(state.subdomainInput.trim())
            _uiState.update { it.copy(step = SetupStep.Porto, portos = PortosState.Loading) }
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
        _uiState.update { it.copy(step = SetupStep.Printer) }
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

    fun onTestPrint() {
        if (_uiState.value.printerTest is PrinterTestState.Testing) return
        viewModelScope.launch {
            // Persiste a escolha antes do teste pra que a impressora resolva pelo VID/PID salvo.
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

    fun onBack() {
        _uiState.update { state ->
            val previous = when (state.step) {
                SetupStep.Subdomain -> SetupStep.Subdomain
                SetupStep.Porto -> SetupStep.Subdomain
                SetupStep.Printer -> SetupStep.Porto
                SetupStep.Language -> SetupStep.Printer
            }
            state.copy(step = previous, errorMessage = null)
        }
    }

    fun onFinish() {
        val state = _uiState.value
        val porto = state.selectedPorto ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                completarSetup(
                    subdomain = state.subdomainInput.trim(),
                    porto = porto,
                    language = state.selectedLanguage,
                    printerVendorId = state.selectedPrinter?.vendorId,
                    printerProductId = state.selectedPrinter?.productId,
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, completed = true) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSaving = false, errorMessage = throwable.message) }
            }
        }
    }
}
