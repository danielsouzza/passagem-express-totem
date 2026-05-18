package com.example.passagenexpress.feature.passenger

import com.example.passagenexpress.core.domain.model.Comodo
import com.example.passagenexpress.core.domain.model.TipoDocumento

data class PassengerUiState(
    val trechoId: Long = 0,
    val forms: List<PassageiroForm> = emptyList(),
    val activeIndex: Int = 0,
    val keypadField: KeypadField? = null,
    val cpfLookupInFlight: Boolean = false,
    val submitting: Boolean = false,
    val erroGeral: String? = null,
    val completed: PassengerCompleted? = null,
) {
    val form: PassageiroForm? get() = forms.getOrNull(activeIndex)
    val canGoPrev: Boolean get() = activeIndex > 0
    val canGoNext: Boolean get() = activeIndex < forms.lastIndex
    val isLast: Boolean get() = activeIndex == forms.lastIndex
}

/**
 * Contato do pedido (nome/email/telefone) que vai pro body do `criarPedido`. Mantido como tipo
 * de saída pra Payment ler sem mudanças — agora derivado automaticamente do passageiro #1 no
 * submit (sem UI dedicada).
 */
data class ContatoForm(
    val nome: String = "",
    val email: String = "",
    val telefoneDisplay: String = "",
    val errors: ContatoFormErrors = ContatoFormErrors(),
) {
    fun isEmpty(): Boolean = nome.isBlank() && email.isBlank() && telefoneDisplay.isBlank()
}

data class ContatoFormErrors(
    val nome: String? = null,
    val email: String? = null,
    val telefone: String? = null,
) {
    fun isEmpty(): Boolean = nome == null && email == null && telefone == null
}

/**
 * Single passenger form. Strings carry the *display* (masked) value while the user types;
 * digit-only values are extracted at submit time. Errors map to field name → message.
 */
data class PassageiroForm(
    val comodo: Comodo,
    val tipoDocumento: TipoDocumento = TipoDocumento.CPF,
    val documentoDisplay: String = "",
    val nome: String = "",
    val telefoneDisplay: String = "",
    val nascimentoDisplay: String = "",
    val errors: PassageiroFormErrors = PassageiroFormErrors(),
)

data class PassageiroFormErrors(
    val documento: String? = null,
    val nome: String? = null,
    val telefone: String? = null,
    val nascimento: String? = null,
) {
    fun isEmpty(): Boolean =
        documento == null && nome == null && telefone == null && nascimento == null
}

/** Which field is bound to the keypad overlay. Nome usa o keypad alfanumérico; o resto, numérico. */
sealed interface KeypadField {
    data class Documento(val formIndex: Int) : KeypadField
    data class Telefone(val formIndex: Int) : KeypadField
    data class Nascimento(val formIndex: Int) : KeypadField
    data class Nome(val formIndex: Int) : KeypadField
}

/** Sale fully captured — payload handed to the next step (payment). */
data class PassengerCompleted(
    val trechoId: Long,
    val passageiros: List<PassageiroForm>,
    val contato: ContatoForm,
    /** Trecho JSON opaco (`TrechoNavPayload` serializado), pra ser repassado em Payment sem re-encodar. */
    val rawTrechoArg: String,
)
