package com.example.passagenexpress.feature.bus.passengers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.TipoDocumento
import com.example.passagenexpress.core.domain.model.onibus.ContatoOnibus
import com.example.passagenexpress.core.domain.model.onibus.PassageiroOnibus
import com.example.passagenexpress.core.domain.model.onibus.TipoDocOnibus
import com.example.passagenexpress.core.domain.usecase.BuscarPassageiroUseCase
import com.example.passagenexpress.feature.bus.BusSaleSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Campo do passageiro — também é o passo do modal guiado (mesma ordem do fluxo de barco). */
enum class CampoPassageiro { Documento, Nome, Telefone, Nascimento, Email }

/** Erros de validação por campo (chaves de string resolvidas na tela). */
enum class ErroCampo { Obrigatorio, CpfInvalido, DocumentoInvalido, NomeInvalido, DataInvalida, TelefoneInvalido, EmailInvalido }

data class PassageiroOnibusForm(
    /** Poltrona deste passageiro (exibição). */
    val assentoLabel: String,
    val tipoDoc: TipoDocOnibus = TipoDocOnibus.Cpf,
    /** Só dígitos (passaporte: alfanumérico). */
    val documento: String = "",
    val nome: String = "",
    /** Só dígitos. */
    val telefone: String = "",
    /** Só dígitos ddMMyyyy. */
    val nascimento: String = "",
    val email: String = "",
    val erros: Map<CampoPassageiro, ErroCampo> = emptyMap(),
)

/**
 * Mesmo modelo da tela de passageiros do barco (`PassengerViewModel`): lista de cards por assento;
 * o ativo abre o **modal guiado** (Documento → Nome → Telefone → Nascimento) e, depois, pode ser
 * editado campo a campo com o teclado inline. Diferença do ônibus: o passageiro 1 também é o
 * contato do pedido, então o modal dele tem o passo E-mail (o pedido de ônibus exige e-mail).
 */
data class BusPassengersUiState(
    val forms: List<PassageiroOnibusForm> = emptyList(),
    val ativo: Int = 0,
    /** Passo do modal guiado aberto (null = modal fechado, formulário inline visível). */
    val modalStep: CampoPassageiro? = null,
    /** Campo editado pelo teclado inline (modal fechado). */
    val campo: CampoPassageiro? = null,
    val buscandoCadastro: Boolean = false,
    val concluido: Boolean = false,
) {
    val form: PassageiroOnibusForm? get() = forms.getOrNull(ativo)

    /** O 1º passageiro é o contato do pedido. */
    fun ehContato(index: Int): Boolean = index == 0

    fun isFilled(index: Int): Boolean {
        val f = forms.getOrNull(index) ?: return false
        return f.documento.isNotBlank() && f.nome.isNotBlank() && f.telefone.isNotBlank() &&
            f.nascimento.isNotBlank() && (!ehContato(index) || f.email.isNotBlank())
    }

    val temProximo: Boolean get() = ativo < forms.lastIndex
}

@HiltViewModel
class BusPassengersViewModel @Inject constructor(
    private val session: BusSaleSession,
    private val buscarPassageiro: BuscarPassageiroUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusPassengersUiState())
    val uiState: StateFlow<BusPassengersUiState> = _uiState.asStateFlow()

    init {
        val s = session.value
        val assentos = s.ida?.pernas?.firstOrNull()?.assentos.orEmpty()
        val forms = assentos.mapIndexed { i, a ->
            val base = PassageiroOnibusForm(assentoLabel = a.label)
            // Volta pra tela depois de já ter preenchido: restaura o que foi digitado.
            val p = s.passageiros.getOrNull(i) ?: return@mapIndexed base
            base.copy(
                tipoDoc = p.tipoDoc,
                documento = p.documento,
                nome = p.nome,
                telefone = p.telefone,
                nascimento = p.nascimento.let { d -> "%02d%02d%04d".format(d.dayOfMonth, d.monthValue, d.year) },
                email = if (i == 0) s.contato?.email.orEmpty() else "",
            )
        }
        // Igual ao barco: entra com o modal guiado aberto no documento do 1º passageiro.
        val primeiroVazio = forms.firstOrNull()?.documento.isNullOrEmpty()
        _uiState.update {
            it.copy(forms = forms, modalStep = if (primeiroVazio) CampoPassageiro.Documento else null)
        }
    }

    // --- Modal guiado -----------------------------------------------------

    fun onOpenDocModal() = _uiState.update { it.copy(modalStep = CampoPassageiro.Documento, campo = null) }

    fun onCloseModal() = _uiState.update { it.copy(modalStep = null, campo = null) }

    /**
     * "Continuar" do modal: valida o passo atual e avança. Documento → busca o cadastro (achou =
     * preenche e fecha; não achou = segue pro Nome) → Telefone → Nascimento → (E-mail no contato) → fecha.
     */
    fun onModalNext() {
        val s = _uiState.value
        val step = s.modalStep ?: return
        val f = s.form ?: return
        val erro = validar(step, f, s.ehContato(s.ativo))
        if (erro != null) {
            editarAtivo { it.copy(erros = it.erros + (step to erro)) }
            return
        }
        when (step) {
            CampoPassageiro.Documento -> buscarCadastro(s.ativo, f, fromModal = true)
            CampoPassageiro.Nome -> irPara(CampoPassageiro.Telefone)
            CampoPassageiro.Telefone -> irPara(CampoPassageiro.Nascimento)
            CampoPassageiro.Nascimento ->
                if (s.ehContato(s.ativo)) irPara(CampoPassageiro.Email) else onCloseModal()
            CampoPassageiro.Email -> onCloseModal()
        }
    }

    private fun irPara(step: CampoPassageiro) = _uiState.update { it.copy(modalStep = step) }

    fun onTipoDoc(tipo: TipoDocOnibus) = editarAtivo {
        it.copy(tipoDoc = tipo, documento = "", erros = it.erros - CampoPassageiro.Documento)
    }

    // --- Cards / rodapé ---------------------------------------------------

    /** Ativa o card [index]: documento vazio abre o modal; preenchido mostra o form inline. */
    fun onEditPassenger(index: Int) {
        if (index !in _uiState.value.forms.indices) return
        _uiState.update { it.ativar(index) }
    }

    private fun BusPassengersUiState.ativar(index: Int): BusPassengersUiState {
        val vazio = forms.getOrNull(index)?.documento.isNullOrEmpty()
        return copy(ativo = index, modalStep = if (vazio) CampoPassageiro.Documento else null, campo = null)
    }

    /** "Próximo passageiro" / "Avançar": valida o ativo e segue; no último, grava e conclui. */
    fun onAdvance() {
        val s = _uiState.value
        val f = s.form ?: return
        val erros = errosDe(f, s.ehContato(s.ativo))
        if (erros.isNotEmpty()) {
            editarAtivo { it.copy(erros = erros) }
            return
        }
        if (s.temProximo) {
            _uiState.update { it.ativar(it.ativo + 1) }
            return
        }
        // Todos os anteriores já foram validados ao avançar; revalida por segurança.
        val ruim = s.forms.indices.firstOrNull { errosDe(s.forms[it], s.ehContato(it)).isNotEmpty() }
        if (ruim != null) {
            _uiState.update { it.copy(ativo = ruim, modalStep = CampoPassageiro.Documento) }
            return
        }
        val passageiros = s.forms.map { form ->
            PassageiroOnibus(
                nome = form.nome.trim().replace(Regex("\\s+"), " "),
                tipoDoc = form.tipoDoc,
                documento = form.documento,
                nascimento = parseNascimento(form.nascimento)!!,
                telefone = form.telefone,
            )
        }
        val primeiro = s.forms.first()
        session.setPassageiros(
            passageiros = passageiros,
            contato = ContatoOnibus(
                nome = passageiros.first().nome,
                email = primeiro.email.trim(),
                telefone = primeiro.telefone,
            ),
        )
        _uiState.update { it.copy(concluido = true, modalStep = null, campo = null) }
    }

    fun onNavigated() = _uiState.update { it.copy(concluido = false) }

    // --- Teclado (modal e inline) ----------------------------------------

    fun onOpenKeypad(campo: CampoPassageiro) = _uiState.update { it.copy(campo = campo) }

    fun onCloseKeypad() = _uiState.update { it.copy(campo = null) }

    /** Campo que recebe a digitação: o passo do modal, ou o campo inline. */
    private fun campoAtual(): CampoPassageiro? = _uiState.value.modalStep ?: _uiState.value.campo

    fun onChar(c: Char) {
        val campo = campoAtual() ?: return
        editarAtivo { f ->
            val novo = when (campo) {
                CampoPassageiro.Documento -> {
                    val permitido = if (f.tipoDoc == TipoDocOnibus.Passaporte) c.isLetterOrDigit() else c.isDigit()
                    if (!permitido || f.documento.length >= maxDoc(f.tipoDoc)) f
                    else f.copy(documento = f.documento + c.uppercaseChar())
                }
                CampoPassageiro.Nome -> if (f.nome.length >= 80) f else f.copy(nome = f.nome + c)
                CampoPassageiro.Telefone -> if (!c.isDigit() || f.telefone.length >= 11) f else f.copy(telefone = f.telefone + c)
                CampoPassageiro.Nascimento -> if (!c.isDigit() || f.nascimento.length >= 8) f else f.copy(nascimento = f.nascimento + c)
                CampoPassageiro.Email -> if (c == ' ' || f.email.length >= 120) f else f.copy(email = f.email + c.lowercaseChar())
            }
            novo.copy(erros = novo.erros - campo)
        }
    }

    fun onBackspace() {
        val campo = campoAtual() ?: return
        editarAtivo { f ->
            when (campo) {
                CampoPassageiro.Documento -> f.copy(documento = f.documento.dropLast(1))
                CampoPassageiro.Nome -> f.copy(nome = f.nome.dropLast(1))
                CampoPassageiro.Telefone -> f.copy(telefone = f.telefone.dropLast(1))
                CampoPassageiro.Nascimento -> f.copy(nascimento = f.nascimento.dropLast(1))
                CampoPassageiro.Email -> f.copy(email = f.email.dropLast(1))
            }
        }
    }

    /** "Concluído" do teclado inline: fecha e valida o campo editado. */
    fun onKeypadDone() {
        val s = _uiState.value
        val campo = s.campo ?: return
        val f = s.form ?: return
        _uiState.update { it.copy(campo = null) }
        validar(campo, f, s.ehContato(s.ativo))?.let { erro -> editarAtivo { it.copy(erros = it.erros + (campo to erro)) } }
    }

    // --- Busca do cadastro (mesmo endpoint do barco) ---------------------

    private fun buscarCadastro(index: Int, form: PassageiroOnibusForm, fromModal: Boolean) {
        _uiState.update { it.copy(buscandoCadastro = true) }
        viewModelScope.launch {
            val p = (buscarPassageiro(form.tipoDoc.toTipoDocumento(), form.documento) as? AppResult.Success)?.value
            _uiState.update { s ->
                if (p == null) {
                    // Não achou: segue guiando campo a campo.
                    if (fromModal && s.modalStep == CampoPassageiro.Documento) {
                        s.copy(buscandoCadastro = false, modalStep = CampoPassageiro.Nome)
                    } else {
                        s.copy(buscandoCadastro = false)
                    }
                } else {
                    val forms = s.forms.mapIndexed { i, f ->
                        if (i != index) f else f.copy(
                            nome = p.nome.ifBlank { f.nome },
                            telefone = p.telefone.filter(Char::isDigit).ifBlank { f.telefone },
                            nascimento = p.dataNascimento
                                ?.let { d -> "%02d%02d%04d".format(d.dayOfMonth, d.monthValue, d.year) }
                                ?: f.nascimento,
                            email = f.email.ifBlank { p.email.orEmpty() },
                        )
                    }
                    val preenchido = s.copy(buscandoCadastro = false, forms = forms)
                    // Achou: fecha o modal e mostra o form preenchido pra revisão — mas se ainda faltar
                    // algo obrigatório (ex.: e-mail do contato), continua guiando a partir do que falta.
                    val falta = CampoPassageiro.entries
                        .drop(1)
                        .firstOrNull { c -> validar(c, forms[index], s.ehContato(index)) != null }
                    if (fromModal) preenchido.copy(modalStep = falta) else preenchido
                }
            }
        }
    }

    // --- Validação (regras do site: ValidacaoPassageiro.js) --------------

    private fun errosDe(f: PassageiroOnibusForm, contato: Boolean): Map<CampoPassageiro, ErroCampo> =
        CampoPassageiro.entries.mapNotNull { c -> validar(c, f, contato)?.let { c to it } }.toMap()

    private fun validar(campo: CampoPassageiro, f: PassageiroOnibusForm, contato: Boolean): ErroCampo? = when (campo) {
        CampoPassageiro.Documento -> when {
            f.documento.isBlank() -> ErroCampo.Obrigatorio
            f.tipoDoc == TipoDocOnibus.Cpf && !cpfValido(f.documento) -> ErroCampo.CpfInvalido
            f.tipoDoc == TipoDocOnibus.Rg && f.documento.length !in 5..15 -> ErroCampo.DocumentoInvalido
            f.tipoDoc == TipoDocOnibus.Passaporte && f.documento.length !in 6..20 -> ErroCampo.DocumentoInvalido
            f.tipoDoc == TipoDocOnibus.Cnh && f.documento.length != 11 -> ErroCampo.DocumentoInvalido
            else -> null
        }
        CampoPassageiro.Nome -> nomeErro(f.nome)
        CampoPassageiro.Telefone -> when {
            f.telefone.isBlank() -> ErroCampo.Obrigatorio
            f.telefone.length !in 10..11 -> ErroCampo.TelefoneInvalido
            else -> null
        }
        CampoPassageiro.Nascimento -> when {
            f.nascimento.isBlank() -> ErroCampo.Obrigatorio
            parseNascimento(f.nascimento) == null -> ErroCampo.DataInvalida
            else -> null
        }
        CampoPassageiro.Email -> when {
            !contato -> null
            f.email.isBlank() -> ErroCampo.Obrigatorio
            !EMAIL_REGEX.matches(f.email.trim()) -> ErroCampo.EmailInvalido
            else -> null
        }
    }

    private fun nomeErro(nome: String): ErroCampo? {
        val limpo = nome.trim().replace(Regex("\\s+"), " ")
        if (limpo.isEmpty()) return ErroCampo.Obrigatorio
        // Regra do site: só letras, ao menos 2 palavras e 5 caracteres.
        val soLetras = limpo.all { it.isLetter() || it == ' ' || it == '\'' || it == '-' }
        val palavras = limpo.split(' ').count { it.isNotBlank() }
        return if (!soLetras || palavras < 2 || limpo.length < 5) ErroCampo.NomeInvalido else null
    }

    private fun editarAtivo(transform: (PassageiroOnibusForm) -> PassageiroOnibusForm) {
        _uiState.update { s ->
            val f = s.form ?: return@update s
            s.copy(forms = s.forms.toMutableList().also { it[s.ativo] = transform(f) })
        }
    }

    private fun maxDoc(tipo: TipoDocOnibus): Int = when (tipo) {
        TipoDocOnibus.Cpf -> 11
        TipoDocOnibus.Rg -> 15
        TipoDocOnibus.Passaporte -> 20
        TipoDocOnibus.Cnh -> 11
    }

    private fun TipoDocOnibus.toTipoDocumento(): TipoDocumento = when (this) {
        TipoDocOnibus.Cpf -> TipoDocumento.CPF
        TipoDocOnibus.Rg -> TipoDocumento.RG
        TipoDocOnibus.Passaporte -> TipoDocumento.Passaporte
        TipoDocOnibus.Cnh -> TipoDocumento.CNH
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
    }
}

/** "ddMMyyyy" → data válida, não futura e com até 120 anos. */
internal fun parseNascimento(digits: String): LocalDate? {
    if (digits.length != 8) return null
    val d = digits.substring(0, 2).toIntOrNull() ?: return null
    val m = digits.substring(2, 4).toIntOrNull() ?: return null
    val y = digits.substring(4, 8).toIntOrNull() ?: return null
    val date = runCatching { LocalDate.of(y, m, d) }.getOrNull() ?: return null
    val hoje = LocalDate.now()
    return date.takeIf { !it.isAfter(hoje) && it.isAfter(hoje.minusYears(120)) }
}

/** Dígitos verificadores do CPF (mesma regra do `validarCPF` do site). */
internal fun cpfValido(cpf: String): Boolean {
    if (cpf.length != 11 || !cpf.all(Char::isDigit) || cpf.all { it == cpf[0] }) return false
    val n = cpf.map { it - '0' }
    val d1 = (0 until 9).sumOf { n[it] * (10 - it) }.let { (it * 10 % 11) % 10 }
    val d2 = (0 until 10).sumOf { n[it] * (11 - it) }.let { (it * 10 % 11) % 10 }
    return d1 == n[9] && d2 == n[10]
}
