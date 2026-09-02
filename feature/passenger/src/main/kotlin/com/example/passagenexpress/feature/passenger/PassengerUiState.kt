package com.example.passagenexpress.feature.passenger

import com.example.passagenexpress.core.domain.model.Comodo
import com.example.passagenexpress.core.domain.model.TipoDocumento

data class PassengerUiState(
    val trechoId: Long = 0,
    val forms: List<PassageiroForm> = emptyList(),
    val activeIndex: Int = 0,
    val keypadField: KeypadField? = null,
    /**
     * Passo atual do modal guiado do passageiro ativo (null = modal fechado, form inline visível).
     * Abre na entrada em [PassengerModalStep.Documento]; se o backend não achar o passageiro, avança
     * campo a campo (Nome → Telefone → Nascimento); se achar, fecha e mostra o form preenchido.
     */
    val modalStep: PassengerModalStep? = null,
    val cpfLookupInFlight: Boolean = false,
    val submitting: Boolean = false,
    val erroGeral: String? = null,
    val completed: PassengerCompleted? = null,
) {
    val form: PassageiroForm? get() = forms.getOrNull(activeIndex)

    /**
     * Ocupante "extra" (removível): é o 2º+ passageiro de um mesmo cômodo — só acontece em
     * camarote de capacidade > 1. Detectado por índice: existe um form anterior com o mesmo
     * `comodo.id`. Pivô e lugares avulsos (poltrona) nunca são extras.
     */
    fun isExtraOccupant(index: Int): Boolean {
        val f = forms.getOrNull(index) ?: return false
        return forms.take(index).any { it.comodo.id == f.comodo.id }
    }

    /** Preenchido = tem os 4 campos (validação completa fica pro submit). */
    fun isFilled(form: PassageiroForm): Boolean =
        form.documentoDisplay.isNotBlank() && form.nome.isNotBlank() &&
            form.telefoneDisplay.isNotBlank() && form.nascimentoDisplay.isNotBlank()

    /**
     * Avança quando todo assento ATIVO (não removido) está preenchido. Assentos extras marcados
     * como "vou sozinho" (skipped) não contam.
     */
    val canFinish: Boolean
        get() = forms.none { !it.skipped && !isFilled(it) }
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
    /** Ocupante extra marcado como "vou sozinho": mantido como placeholder, descartado no envio. */
    val skipped: Boolean = false,
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

/** Passos do modal guiado de captura do passageiro. */
enum class PassengerModalStep { Documento, Nome, Telefone, Nascimento }

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
