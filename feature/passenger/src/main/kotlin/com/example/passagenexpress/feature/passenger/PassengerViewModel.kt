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
        run {
            val forms = inicioVenda.comodos.map { comodo -> PassageiroForm(comodo = comodo) }
            PassengerUiState(
                trechoId = inicioVenda.trechoId,
                forms = forms,
                // Ao entrar, já abre o modal guiado no passo do documento, com o teclado pronto.
                modalStep = if (forms.isEmpty()) null else PassengerModalStep.Documento,
                keypadField = if (forms.isEmpty()) null else KeypadField.Documento(0),
            )
        }
    )
    val uiState: StateFlow<PassengerUiState> = _uiState.asStateFlow()

    /** Reabre o modal guiado (a partir do documento) do passageiro ativo — ex.: tocar no campo doc. */
    fun onOpenDocModal() = _uiState.update {
        it.copy(modalStep = PassengerModalStep.Documento, keypadField = KeypadField.Documento(it.activeIndex))
    }

    fun onCloseModal() = _uiState.update { it.copy(modalStep = null, keypadField = null) }

    /**
     * "Próximo/Concluir" do modal. Valida o campo do passo atual; se ok avança:
     * Documento → dispara a busca no backend (o resultado decide: achou = fecha e preenche o form;
     * não achou = segue para Nome); Nome → Telefone → Nascimento → fecha o modal.
     */
    fun onModalNext() {
        val state = _uiState.value
        val step = state.modalStep ?: return
        val idx = state.activeIndex
        val form = state.forms.getOrNull(idx) ?: return
        when (step) {
            PassengerModalStep.Documento -> {
                val rawDoc = DocumentoMask.digitsOnly(form.documentoDisplay, form.tipoDocumento)
                if (!DocumentoMask.isValid(rawDoc, form.tipoDocumento)) {
                    setActiveError { it.copy(documento = if (rawDoc.isEmpty()) ERROR_DOC_REQUIRED else ERROR_DOC_INVALID) }
                    return
                }
                lookupPassageiro(idx, form.tipoDocumento, rawDoc, fromModal = true)
            }
            PassengerModalStep.Nome -> {
                if (form.nome.trim().length < 2) {
                    setActiveError { it.copy(nome = ERROR_NAME_REQUIRED) }
                    return
                }
                goToStep(PassengerModalStep.Telefone, idx)
            }
            PassengerModalStep.Telefone -> {
                val phone = PhoneMask.digitsOnly(form.telefoneDisplay)
                if (phone.length !in 10..11) {
                    setActiveError { it.copy(telefone = if (phone.isEmpty()) ERROR_PHONE_REQUIRED else ERROR_PHONE_INVALID) }
                    return
                }
                goToStep(PassengerModalStep.Nascimento, idx)
            }
            PassengerModalStep.Nascimento -> {
                val dateDigits = BirthdateMask.digitsOnly(form.nascimentoDisplay)
                if (dateDigits.isEmpty() || parseBirthdate(form.nascimentoDisplay) == null) {
                    setActiveError { it.copy(nascimento = if (dateDigits.isEmpty()) ERROR_BIRTH_REQUIRED else ERROR_BIRTH_INVALID) }
                    return
                }
                // Fecha o modal e mantém o passageiro ativo com o formulário inline preenchido,
                // pra revisão/edição. O avanço pro próximo é explícito (botão "Próximo passageiro").
                _uiState.update { it.copy(modalStep = null, keypadField = null) }
            }
        }
    }

    private fun goToStep(step: PassengerModalStep, idx: Int) = _uiState.update {
        it.copy(modalStep = step, keypadField = keypadFieldFor(step, idx))
    }

    private fun keypadFieldFor(step: PassengerModalStep, idx: Int): KeypadField = when (step) {
        PassengerModalStep.Documento -> KeypadField.Documento(idx)
        PassengerModalStep.Nome -> KeypadField.Nome(idx)
        PassengerModalStep.Telefone -> KeypadField.Telefone(idx)
        PassengerModalStep.Nascimento -> KeypadField.Nascimento(idx)
    }

    private fun setActiveError(transform: (PassageiroFormErrors) -> PassageiroFormErrors) =
        updateActiveForm { it.copy(errors = transform(it.errors)) }

    fun onChangeTipoDocumento(tipo: TipoDocumento) = updateActiveForm {
        it.copy(tipoDocumento = tipo, documentoDisplay = "", errors = it.errors.copy(documento = null))
    }

    fun onChangeNome(value: String) = updateActiveForm {
        it.copy(nome = value, errors = it.errors.copy(nome = null))
    }

    /**
     * Ativa/expande o assento [index]. Se o documento está vazio, abre o modal guiado; se já tem
     * dados, mostra o formulário inline preenchido (para edição campo a campo).
     */
    fun onEditPassenger(index: Int) {
        if (index !in _uiState.value.forms.indices) return
        _uiState.update { it.activateAt(index) }
    }

    private fun PassengerUiState.activateAt(index: Int): PassengerUiState {
        val docEmpty = forms.getOrNull(index)?.documentoDisplay.isNullOrEmpty()
        return copy(
            activeIndex = index,
            modalStep = if (docEmpty) PassengerModalStep.Documento else null,
            keypadField = if (docEmpty) KeypadField.Documento(index) else null,
        )
    }

    /**
     * "Próximo passageiro" / "Avançar" (rodapé). Valida o assento ativo e ativa o próximo assento
     * não removido; se for o último, envia. Ocupante extra vazio é marcado "vou sozinho" e o fluxo
     * segue. Coexiste com os botões "Preencher dados" dos cards.
     */
    fun onAdvance() {
        val state = _uiState.value
        if (state.submitting) return
        val idx = state.activeIndex
        val form = state.forms.getOrNull(idx) ?: return onSubmit()
        // Extra deixado em branco: marca como "vou sozinho" e segue.
        if (state.isExtraOccupant(idx) && form.documentoDisplay.isBlank()) {
            setSkipped(idx, true)
            advanceFrom(idx)
            return
        }
        val validated = validate(form)
        if (!validated.errors.isEmpty()) {
            updateActiveForm { validated }
            return
        }
        _uiState.update { it.copy(forms = it.forms.setAt(idx, validated)) }
        advanceFrom(idx)
    }

    /** Ativa o próximo assento não removido após [idx]; se não houver, envia. */
    private fun advanceFrom(idx: Int) {
        val next = _uiState.value.forms.indices.firstOrNull { it > idx && !_uiState.value.forms[it].skipped }
        if (next != null) {
            _uiState.update { it.activateAt(next) }
        } else {
            onSubmit()
        }
    }

    private fun <T> List<T>.setAt(index: Int, value: T): List<T> =
        mapIndexed { i, item -> if (i == index) value else item }

    /**
     * Marca um ocupante extra como "vou sozinho" — NÃO deleta o assento; vira um placeholder
     * "removido" com opção de readicionar. Só extras podem ser removidos.
     */
    fun onRemovePassenger(index: Int) = setSkipped(index, true)

    /** Remove o ocupante extra aberto no modal (botão "Vou sozinho" dentro do modal) e fecha. */
    fun onRemoveActiveOccupant() {
        val idx = _uiState.value.activeIndex
        if (!_uiState.value.isExtraOccupant(idx)) return
        setSkipped(idx, true)
        _uiState.update { it.copy(modalStep = null, keypadField = null) }
    }

    /** Readiciona um ocupante extra antes removido (botão "Adicionar"). */
    fun onRestoreOccupant(index: Int) = setSkipped(index, false)

    private fun setSkipped(index: Int, skipped: Boolean) {
        if (!_uiState.value.isExtraOccupant(index)) return
        _uiState.update { state ->
            state.copy(forms = state.forms.mapIndexed { i, f -> if (i == index) f.copy(skipped = skipped) else f })
        }
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
        val current = _uiState.value
        if (current.submitting) return
        // Descarta ocupantes extras removidos ("vou sozinho") e extras deixados totalmente em branco.
        val kept = current.forms.filterIndexed { i, f ->
            val extra = current.isExtraOccupant(i)
            !(extra && (f.skipped || f.documentoDisplay.isBlank()))
        }
        val validatedForms = kept.map { validate(it) }
        val hasFormErrors = validatedForms.any { !it.errors.isEmpty() }
        if (hasFormErrors) {
            val firstBadIdx = validatedForms.indexOfFirst { !it.errors.isEmpty() }.coerceAtLeast(0)
            _uiState.update {
                it.copy(
                    forms = validatedForms,
                    activeIndex = firstBadIdx,
                    // Editor é o modal: reabre no passageiro com erro pra correção.
                    modalStep = PassengerModalStep.Documento,
                    keypadField = KeypadField.Documento(firstBadIdx),
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
     * Consome o evento one-shot de "passageiros concluídos" depois que a tela já navegou pra
     * Payment. Sem isso, o StateFlow re-emite `completed` quando o usuário volta pra cá e o
     * LaunchedEffect re-dispara a navegação, re-avançando sozinho. `submitting` volta a false
     * pra que o botão "Avançar" continue funcional ao voltar.
     */
    fun onCompletedHandled() = _uiState.update { it.copy(completed = null, submitting = false) }

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

    private fun lookupPassageiro(
        formIndex: Int,
        tipo: TipoDocumento,
        rawDoc: String,
        fromModal: Boolean = false,
    ) {
        _uiState.update { it.copy(cpfLookupInFlight = true) }
        viewModelScope.launch {
            val result = buscarPassageiro(tipo, rawDoc)
            _uiState.update { state ->
                val passageiro = (result as? AppResult.Success)?.value
                if (passageiro == null) {
                    // Não achou: no modal, segue guiando campo a campo (próximo = Nome).
                    if (fromModal && state.modalStep == PassengerModalStep.Documento) {
                        state.copy(
                            cpfLookupInFlight = false,
                            modalStep = PassengerModalStep.Nome,
                            keypadField = KeypadField.Nome(formIndex),
                        )
                    } else {
                        state.copy(cpfLookupInFlight = false)
                    }
                } else {
                    val forms = state.forms.mapIndexed { i, f ->
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
                    }
                    // Achou: preenche e, se veio do modal, fecha o modal e mostra o formulário inline
                    // preenchido pra revisão/edição (SEM auto-avançar pro próximo passageiro).
                    val filled = state.copy(cpfLookupInFlight = false, forms = forms)
                    if (fromModal) filled.copy(modalStep = null, keypadField = null) else filled
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
