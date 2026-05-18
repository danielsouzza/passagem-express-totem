package com.example.passagenexpress.feature.passenger

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.TipoDocumento
import com.example.passagenexpress.core.domain.usecase.BuscarPassageiroUseCase
import com.example.passagenexpress.feature.passenger.format.DocumentoMask
import com.example.passagenexpress.feature.passenger.format.PhoneMask
import com.example.passagenexpress.feature.passenger.format.BirthdateMask
import com.example.passagenexpress.feature.passenger.format.parseBirthdate
import com.example.passagenexpress.feature.passenger.navigation.decodeInicioVendaArg
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PassengerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val buscarPassageiro: BuscarPassageiroUseCase,
) : ViewModel() {

    private val inicioVenda = decodeInicioVendaArg(
        savedStateHandle.get<String>(ARG_INICIO_VENDA).orEmpty()
    ).toDomain()

    /** Pass-through: a tela passenger não consome Trecho, só repassa pra Payment. */
    private val rawTrechoArg: String = savedStateHandle.get<String>(ARG_TRECHO).orEmpty()

    private val _uiState = MutableStateFlow(
        PassengerUiState(
            trechoId = inicioVenda.trechoId,
            forms = inicioVenda.comodos.map { comodo ->
                PassageiroForm(comodo = comodo)
            },
        )
    )
    val uiState: StateFlow<PassengerUiState> = _uiState.asStateFlow()

    fun onChangeTipoDocumento(tipo: TipoDocumento) = updateActiveForm {
        it.copy(tipoDocumento = tipo, documentoDisplay = "", errors = it.errors.copy(documento = null))
    }

    fun onChangeNome(value: String) = updateActiveForm {
        it.copy(nome = value, errors = it.errors.copy(nome = null))
    }

    fun onSelectPassenger(index: Int) {
        if (index !in _uiState.value.forms.indices) return
        _uiState.update { it.copy(activeIndex = index, keypadField = null) }
    }

    fun onPrev() {
        if (!_uiState.value.canGoPrev) return
        _uiState.update { it.copy(activeIndex = it.activeIndex - 1, keypadField = null) }
    }

    fun onNext() {
        if (!_uiState.value.canGoNext) return
        _uiState.update { it.copy(activeIndex = it.activeIndex + 1, keypadField = null) }
    }

    fun onOpenKeypad(field: KeypadField) {
        _uiState.update { it.copy(keypadField = field) }
    }

    fun onCloseKeypad() {
        _uiState.update { it.copy(keypadField = null) }
    }

    fun onKeypadDigit(digit: Char) {
        val field = _uiState.value.keypadField ?: return
        if (!digit.isDigit()) return
        if (field is KeypadField.Nome) return // teclado numérico não escreve em Nome
        applyKeypadEdit(field) { current -> current + digit }
    }

    /** Single-char input vindo do teclado alfanumérico (Nome). */
    fun onKeypadChar(char: Char) {
        val field = _uiState.value.keypadField ?: return
        if (field !is KeypadField.Nome) return
        applyKeypadEdit(field) { current -> current + char }
    }

    fun onKeypadBackspace() {
        val field = _uiState.value.keypadField ?: return
        applyKeypadEdit(field) { current -> current.dropLast(1) }
    }

    /**
     * On keypad close for a Documento field, dispara o lookup `/api/filtros/get-passageiro?tipo&doc`.
     * O backend espera o documento sem máscara (dígitos puros) e o id do tipo (`TipoDocumento.id`).
     * Só dispara quando o documento estiver com tamanho válido para o tipo escolhido — evita ruído
     * de chamadas pra strings incompletas.
     */
    fun onKeypadDone() {
        val field = _uiState.value.keypadField
        _uiState.update { it.copy(keypadField = null) }
        if (field is KeypadField.Documento) {
            val form = _uiState.value.forms.getOrNull(field.formIndex) ?: return
            val rawDoc = DocumentoMask.digitsOnly(form.documentoDisplay, form.tipoDocumento)
            if (DocumentoMask.isValid(rawDoc, form.tipoDocumento)) {
                lookupPassageiro(field.formIndex, form.tipoDocumento, rawDoc)
            }
        }
    }

    fun onSubmit() {
        if (_uiState.value.submitting) return
        val validatedForms = _uiState.value.forms.map { validate(it) }
        val hasFormErrors = validatedForms.any { !it.errors.isEmpty() }
        if (hasFormErrors) {
            val firstBadIdx = validatedForms.indexOfFirst { !it.errors.isEmpty() }
            _uiState.update {
                it.copy(
                    forms = validatedForms,
                    activeIndex = firstBadIdx.coerceAtLeast(0),
                    erroGeral = null,
                )
            }
            return
        }
        val derivedContato = deriveContato(validatedForms.firstOrNull())
        _uiState.update {
            it.copy(
                forms = validatedForms,
                erroGeral = null,
                submitting = true,
                completed = PassengerCompleted(
                    trechoId = inicioVenda.trechoId,
                    passageiros = validatedForms,
                    contato = derivedContato,
                    rawTrechoArg = rawTrechoArg,
                ),
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(erroGeral = null) }
    }

    /**
     * Contato do pedido é derivado do passageiro #1 (paridade simplificada com `home.vue` —
     * web tem um bloco "Dados para Contato" dedicado, mas o totem coleta menos campos).
     * Email vai vazio; `PaymentViewModel.buildNovoPedido` já mapeia blank → null.
     */
    private fun deriveContato(first: PassageiroForm?): ContatoForm {
        if (first == null) return ContatoForm()
        return ContatoForm(
            nome = first.nome,
            email = "",
            telefoneDisplay = first.telefoneDisplay,
        )
    }

    private fun lookupPassageiro(formIndex: Int, tipo: TipoDocumento, rawDoc: String) {
        _uiState.update { it.copy(cpfLookupInFlight = true) }
        viewModelScope.launch {
            val result = buscarPassageiro(tipo, rawDoc)
            _uiState.update { state ->
                val passageiro = (result as? AppResult.Success)?.value
                if (passageiro == null) {
                    state.copy(cpfLookupInFlight = false)
                } else {
                    state.copy(
                        cpfLookupInFlight = false,
                        forms = state.forms.mapIndexed { i, f ->
                            if (i != formIndex) f else f.copy(
                                nome = passageiro.nome.ifBlank { f.nome },
                                telefoneDisplay = passageiro.telefone
                                    .takeIf { it.isNotBlank() }
                                    ?.let { PhoneMask.applyMask(it) }
                                    ?: f.telefoneDisplay,
                                nascimentoDisplay = passageiro.dataNascimento
                                    ?.let { BirthdateMask.format(it) }
                                    ?: f.nascimentoDisplay,
                            )
                        },
                    )
                }
            }
        }
    }

    private fun applyKeypadEdit(field: KeypadField, transform: (String) -> String) {
        _uiState.update { state ->
            val formIndex = passageiroFieldIndex(field)
            val forms = state.forms.toMutableList()
            val form = forms.getOrNull(formIndex) ?: return@update state
            val updated = when (field) {
                is KeypadField.Documento -> {
                    val digits = transform(DocumentoMask.digitsOnly(form.documentoDisplay, form.tipoDocumento))
                    form.copy(
                        documentoDisplay = DocumentoMask.applyMask(digits, form.tipoDocumento),
                        errors = form.errors.copy(documento = null),
                    )
                }
                is KeypadField.Telefone -> {
                    val digits = transform(PhoneMask.digitsOnly(form.telefoneDisplay))
                    form.copy(
                        telefoneDisplay = PhoneMask.applyMask(digits),
                        errors = form.errors.copy(telefone = null),
                    )
                }
                is KeypadField.Nascimento -> {
                    val digits = transform(BirthdateMask.digitsOnly(form.nascimentoDisplay))
                    form.copy(
                        nascimentoDisplay = BirthdateMask.applyMask(digits),
                        errors = form.errors.copy(nascimento = null),
                    )
                }
                is KeypadField.Nome -> {
                    form.copy(
                        nome = transform(form.nome),
                        errors = form.errors.copy(nome = null),
                    )
                }
            }
            forms[formIndex] = updated
            state.copy(forms = forms)
        }
    }

    private fun passageiroFieldIndex(field: KeypadField): Int = when (field) {
        is KeypadField.Documento -> field.formIndex
        is KeypadField.Telefone -> field.formIndex
        is KeypadField.Nascimento -> field.formIndex
        is KeypadField.Nome -> field.formIndex
    }

    private fun updateActiveForm(transform: (PassageiroForm) -> PassageiroForm) {
        val idx = _uiState.value.activeIndex
        _uiState.update { state ->
            state.copy(
                forms = state.forms.mapIndexed { i, f -> if (i == idx) transform(f) else f },
            )
        }
    }

    private fun validate(form: PassageiroForm): PassageiroForm {
        val docDigits = DocumentoMask.digitsOnly(form.documentoDisplay, form.tipoDocumento)
        val phoneDigits = PhoneMask.digitsOnly(form.telefoneDisplay)
        val dateDigits = BirthdateMask.digitsOnly(form.nascimentoDisplay)
        val parsedDate = parseBirthdate(form.nascimentoDisplay)

        val docError = when {
            docDigits.isEmpty() -> ERROR_DOC_REQUIRED
            !DocumentoMask.isValid(docDigits, form.tipoDocumento) -> ERROR_DOC_INVALID
            else -> null
        }
        val nomeError = if (form.nome.trim().length < 2) ERROR_NAME_REQUIRED else null
        val phoneError = when {
            phoneDigits.isEmpty() -> ERROR_PHONE_REQUIRED
            phoneDigits.length !in 10..11 -> ERROR_PHONE_INVALID
            else -> null
        }
        val dateError = when {
            dateDigits.isEmpty() -> ERROR_BIRTH_REQUIRED
            parsedDate == null -> ERROR_BIRTH_INVALID
            else -> null
        }
        return form.copy(
            errors = PassageiroFormErrors(
                documento = docError,
                nome = nomeError,
                telefone = phoneError,
                nascimento = dateError,
            ),
        )
    }

    companion object {
        const val ARG_TRECHO = "trechoJson"
        const val ARG_INICIO_VENDA = "inicioVendaJson"

        const val ERROR_DOC_REQUIRED = "doc_required"
        const val ERROR_DOC_INVALID = "doc_invalid"
        const val ERROR_NAME_REQUIRED = "name_required"
        const val ERROR_PHONE_REQUIRED = "phone_required"
        const val ERROR_PHONE_INVALID = "phone_invalid"
        const val ERROR_BIRTH_REQUIRED = "birth_required"
        const val ERROR_BIRTH_INVALID = "birth_invalid"
    }
}
