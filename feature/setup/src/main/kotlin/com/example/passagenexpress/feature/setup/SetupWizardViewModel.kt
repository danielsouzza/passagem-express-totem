package com.example.passagenexpress.feature.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.model.Embarcacao
import com.example.passagenexpress.core.domain.model.Modalidade
import com.example.passagenexpress.core.domain.model.PaperWidth
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.model.onibus.LocalOnibus
import com.example.passagenexpress.core.domain.printer.UsbPrinterDevice
import com.example.passagenexpress.core.domain.repository.TotemConfigRepository
import com.example.passagenexpress.core.domain.usecase.BuscarEmbarcacoesUseCase
import com.example.passagenexpress.core.domain.usecase.BuscarPortosUseCase
import com.example.passagenexpress.core.domain.usecase.CompletarSetupUseCase
import com.example.passagenexpress.core.domain.usecase.ListarImpressorasUseCase
import com.example.passagenexpress.core.domain.usecase.SincronizarTemaUseCase
import com.example.passagenexpress.core.domain.usecase.TestarImpressaoUseCase
import com.example.passagenexpress.core.domain.usecase.onibus.BuscarTerminaisUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val buscarTerminais: BuscarTerminaisUseCase,
    private val completarSetup: CompletarSetupUseCase,
    private val listarImpressoras: ListarImpressorasUseCase,
    private val testarImpressao: TestarImpressaoUseCase,
    private val sincronizarTema: SincronizarTemaUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupWizardUiState())
    val uiState: StateFlow<SetupWizardUiState> = _uiState.asStateFlow()

    private var terminalSearchJob: Job? = null

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
        var terminalChanged = false
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
                SetupKeypadField.TerminalSearch -> {
                    terminalChanged = true
                    state.copy(terminalQuery = state.terminalQuery + c)
                }
                // PIN aceita só dígitos, com um teto defensivo de tamanho.
                SetupKeypadField.Pin ->
                    if (!c.isDigit() || state.pinInput.length >= MAX_PIN_LENGTH) state
                    else state.copy(pinInput = state.pinInput + c, errorMessage = null)
                null -> state
            }
        }
        if (terminalChanged) scheduleTerminalSearch()
    }

    fun onKeypadBackspace() {
        var terminalChanged = false
        _uiState.update { state ->
            when (state.keypadField) {
                SetupKeypadField.Subdomain ->
                    state.copy(subdomainInput = state.subdomainInput.dropLast(1))
                SetupKeypadField.PortoSearch ->
                    state.copy(portoSearchQuery = state.portoSearchQuery.dropLast(1))
                SetupKeypadField.EmbarcacaoSearch ->
                    state.copy(embarcacaoSearchQuery = state.embarcacaoSearchQuery.dropLast(1))
                SetupKeypadField.TerminalSearch -> {
                    terminalChanged = true
                    state.copy(terminalQuery = state.terminalQuery.dropLast(1))
                }
                SetupKeypadField.Pin ->
                    state.copy(pinInput = state.pinInput.dropLast(1))
                null -> state
            }
        }
        if (terminalChanged) scheduleTerminalSearch()
    }

    // --- Navegação entre passos (lista dinâmica: depende das modalidades) ---------

    /** Vai para o passo seguinte da lista atual de passos, executando o que ele precisa ao entrar. */
    private fun advance() {
        val state = _uiState.value
        val steps = state.steps
        val next = steps.getOrNull(steps.indexOf(state.step) + 1) ?: return
        enter(next)
    }

    private fun enter(step: SetupStep) {
        _uiState.update {
            it.copy(
                step = step,
                errorMessage = null,
                keypadField = when (step) {
                    // Fluxo guiado: passos de digitação já abrem o teclado.
                    SetupStep.Terminal -> SetupKeypadField.TerminalSearch
                    SetupStep.Pin -> SetupKeypadField.Pin
                    else -> null
                },
            )
        }
        when (step) {
            SetupStep.Porto -> if (_uiState.value.portos !is PortosState.Loaded) {
                _uiState.update { it.copy(portos = PortosState.Loading) }
                viewModelScope.launch { loadPortos() }
            }
            SetupStep.Embarcacao -> if (_uiState.value.embarcacoes !is EmbarcacoesState.Loaded) {
                _uiState.update { it.copy(embarcacoes = EmbarcacoesState.Loading) }
                viewModelScope.launch { loadEmbarcacoes() }
            }
            SetupStep.Printer -> refreshPrinters()
            else -> Unit
        }
    }

    fun onAdvanceFromSubdomain() {
        val state = _uiState.value
        if (!state.canAdvanceFromSubdomain) return
        viewModelScope.launch {
            totemConfigRepository.setSubdomain(state.subdomainInput.trim())
            sincronizarTema()
            // Subdomínio pode ter mudado → listas carregadas antes podem ser de outra empresa.
            _uiState.update {
                it.copy(
                    portos = PortosState.Idle,
                    embarcacoes = EmbarcacoesState.Idle,
                    terminais = TerminaisState.Idle,
                )
            }
            advance()
        }
    }

    // --- Modalidade ---------------------------------------------------------

    /** Liga/desliga uma modalidade. Nunca deixa vazio (o botão Continuar exige ao menos uma). */
    fun onModalidadeToggled(modalidade: Modalidade) {
        _uiState.update { state ->
            val novas = if (modalidade in state.modalidades) state.modalidades - modalidade
            else state.modalidades + modalidade
            state.copy(modalidades = novas)
        }
    }

    fun onAdvanceFromModalidade() {
        if (!_uiState.value.canAdvanceFromModalidade) return
        advance()
    }

    // --- Porto --------------------------------------------------------------

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
        if (!_uiState.value.canAdvanceFromPorto) return
        advance()
    }

    // --- Embarcação ---------------------------------------------------------

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

    fun onAdvanceFromEmbarcacao() = advance()

    // --- Terminal (rodoviário) ----------------------------------------------

    fun onTerminalQueryClear() {
        terminalSearchJob?.cancel()
        // Mantém o teclado aberto: limpar é pra digitar outra busca.
        _uiState.update {
            it.copy(terminalQuery = "", terminais = TerminaisState.Idle, keypadField = SetupKeypadField.TerminalSearch)
        }
    }

    fun onRetryTerminais() = scheduleTerminalSearch(debounce = false)

    /** Debounce de 400 ms (igual ao site) e só com ≥ 3 letras. */
    private fun scheduleTerminalSearch(debounce: Boolean = true) {
        terminalSearchJob?.cancel()
        val query = _uiState.value.terminalQuery.trim()
        if (query.length < BuscarTerminaisUseCase.MinChars) {
            _uiState.update { it.copy(terminais = TerminaisState.Idle) }
            return
        }
        terminalSearchJob = viewModelScope.launch {
            if (debounce) delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(terminais = TerminaisState.Loading) }
            when (val result = buscarTerminais(query)) {
                is AppResult.Success ->
                    _uiState.update { it.copy(terminais = TerminaisState.Loaded(result.value)) }
                is AppResult.Failure -> _uiState.update {
                    it.copy(terminais = TerminaisState.Error(result.error.message ?: "Erro desconhecido"))
                }
            }
        }
    }

    fun onTerminalSelected(local: LocalOnibus) {
        _uiState.update { it.copy(selectedTerminal = local, keypadField = null) }
    }

    fun onAdvanceFromTerminal() {
        if (!_uiState.value.canAdvanceFromTerminal) return
        advance()
    }

    // --- Impressora ---------------------------------------------------------

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

    fun onAdvanceFromPrinter() = advance()

    // --- Idioma / PIN -------------------------------------------------------

    fun onLanguageSelected(language: AppLanguage) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun onAdvanceFromLanguage() = advance()

    fun onBack() {
        val state = _uiState.value
        val steps = state.steps
        val previous = steps.getOrNull(steps.indexOf(state.step) - 1) ?: return
        _uiState.update { it.copy(step = previous, errorMessage = null, keypadField = null) }
    }

    fun onFinish() {
        val state = _uiState.value
        if (!state.canFinish) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, keypadField = null) }
            runCatching {
                completarSetup(
                    subdomain = state.subdomainInput.trim(),
                    // Modalidade desligada não leva a origem dela (porto/embarcação ou terminal),
                    // mesmo que o operador tenha escolhido antes de voltar e desmarcar.
                    porto = state.selectedPorto.takeIf { state.vendeHidroviario },
                    language = state.selectedLanguage,
                    pin = state.pinInput,
                    embarcacaoId = state.selectedEmbarcacao?.id.takeIf { state.vendeHidroviario },
                    embarcacaoNome = state.selectedEmbarcacao?.nome.orEmpty().takeIf { state.vendeHidroviario }.orEmpty(),
                    printerVendorId = state.selectedPrinter?.vendorId,
                    printerProductId = state.selectedPrinter?.productId,
                    printerPaperWidth = state.selectedPaperWidth,
                    modalidades = state.modalidades,
                    terminal = state.selectedTerminal.takeIf { state.vendeRodoviario },
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
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
