package com.example.passagenexpress.feature.bus.seats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.common.coroutine.AppScope
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.onibus.AssentoBloqueado
import com.example.passagenexpress.core.domain.model.onibus.AssentoOnibus
import com.example.passagenexpress.core.domain.model.onibus.ViagemOnibus
import com.example.passagenexpress.core.domain.usecase.onibus.BloquearAssentosUseCase
import com.example.passagenexpress.core.domain.usecase.onibus.DesbloquearAssentosUseCase
import com.example.passagenexpress.core.domain.usecase.onibus.ListarAssentosOnibusUseCase
import com.example.passagenexpress.core.domain.usecase.onibus.ObterTrechoDaPernaUseCase
import com.example.passagenexpress.feature.bus.BusSaleSession
import com.example.passagenexpress.feature.bus.PernaSelecionada
import com.example.passagenexpress.feature.bus.Sentido
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/** Dados de uma perna (rota/horário) — da própria viagem (direta) ou de `viagens/{uuid}/trecho`. */
data class PernaInfo(
    val tripUuid: String,
    val tripLeg: Int,
    val origemSlug: String,
    val destinoSlug: String,
    val origemNome: String,
    val destinoNome: String,
    val dataEmbarque: String,
    val saida: LocalDateTime,
)

sealed interface MapaState {
    data object Loading : MapaState
    data class Loaded(val assentos: List<AssentoOnibus>) : MapaState
    data class Error(val message: String) : MapaState
}

data class BusSeatsUiState(
    val sentido: Sentido = Sentido.Ida,
    val viagem: ViagemOnibus? = null,
    /** null = carregando as pernas (conexão); erro em [pernasErro]. */
    val pernas: List<PernaInfo>? = null,
    val pernasErro: String? = null,
    val pernaIndex: Int = 0,
    val mapa: MapaState = MapaState.Loading,
    /** Labels escolhidos na perna atual. */
    val selecionados: List<String> = emptyList(),
    /** Assentos já bloqueados por índice de perna. */
    val bloqueadas: Map<Int, List<AssentoBloqueado>> = emptyMap(),
    /** Quantidade obrigatória (pernas 2+ e volta espelham a 1ª perna da ida); null = livre. */
    val quantidadeExigida: Int? = null,
    val bloqueando: Boolean = false,
    /** Mensagem do back quando o bloqueio falha (ex.: assento acabou de ser vendido). */
    val erroBloqueio: String? = null,
    /** Todas as pernas bloqueadas — a tela navega para o próximo passo. */
    val concluido: Boolean = false,
) {
    val perna: PernaInfo? get() = pernas?.getOrNull(pernaIndex)
    val totalPernas: Int get() = pernas?.size ?: 0

    val canConfirm: Boolean
        get() = !bloqueando && selecionados.isNotEmpty() &&
            (quantidadeExigida == null || selecionados.size == quantidadeExigida)
}

@HiltViewModel
class BusSeatsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val session: BusSaleSession,
    private val obterTrechoDaPerna: ObterTrechoDaPernaUseCase,
    private val listarAssentos: ListarAssentosOnibusUseCase,
    private val bloquearAssentos: BloquearAssentosUseCase,
    private val desbloquearAssentos: DesbloquearAssentosUseCase,
) : ViewModel() {

    private val sentido: Sentido = savedStateHandle.get<String>(ARG_SENTIDO)
        ?.let { runCatching { Sentido.valueOf(it) }.getOrNull() } ?: Sentido.Ida

    private val _uiState = MutableStateFlow(BusSeatsUiState(sentido = sentido))
    val uiState: StateFlow<BusSeatsUiState> = _uiState.asStateFlow()

    private var mapaJob: Job? = null

    init {
        val selecao = session.value.selecao(sentido)
        val viagem = selecao?.viagem
        // Volta de outra tela com pernas já bloqueadas: restaura pra não rebloquear à toa.
        val restauradas = selecao?.pernas.orEmpty()
            .mapIndexed { i, p -> i to p.assentos }
            .toMap()
        _uiState.update {
            it.copy(
                viagem = viagem,
                bloqueadas = restauradas,
                selecionados = restauradas[0].orEmpty().map { a -> a.label },
                quantidadeExigida = quantidadeExigida(0, restauradas),
            )
        }
        if (viagem != null) carregarPernas(viagem)
    }

    /** Ida/perna 1 é livre; as demais pernas e a volta precisam do mesmo número de assentos. */
    private fun quantidadeExigida(pernaIndex: Int, bloqueadas: Map<Int, List<AssentoBloqueado>>): Int? =
        when {
            sentido == Sentido.Ida && pernaIndex == 0 -> null
            sentido == Sentido.Ida -> bloqueadas[0]?.size
            else -> session.value.quantidadePassageiros.takeIf { it > 0 }
        }

    fun onRetryPernas() {
        _uiState.value.viagem?.let { carregarPernas(it) }
    }

    private fun carregarPernas(viagem: ViagemOnibus) {
        viewModelScope.launch {
            _uiState.update { it.copy(pernas = null, pernasErro = null) }
            if (!viagem.temConexao) {
                val unica = PernaInfo(
                    tripUuid = viagem.tripUuid,
                    tripLeg = viagem.escalas.firstOrNull()?.tripLeg ?: 1,
                    origemSlug = viagem.origemSlug,
                    destinoSlug = viagem.destinoSlug,
                    origemNome = viagem.origemNome,
                    destinoNome = viagem.destinoNome,
                    dataEmbarque = viagem.dataEmbarqueRaw,
                    saida = viagem.saida,
                )
                _uiState.update { it.copy(pernas = listOf(unica)) }
                carregarMapa()
                return@launch
            }
            // Conexão: cada perna tem rota/horário próprios (`viagens/{uuid}/trecho`).
            val pernas = mutableListOf<PernaInfo>()
            for (escala in viagem.escalas) {
                when (val r = obterTrechoDaPerna(escala.tripUuid)) {
                    is AppResult.Success -> pernas += PernaInfo(
                        tripUuid = escala.tripUuid,
                        tripLeg = escala.tripLeg,
                        origemSlug = r.value.origemSlug.ifEmpty { viagem.origemSlug },
                        destinoSlug = r.value.destinoSlug.ifEmpty { viagem.destinoSlug },
                        origemNome = r.value.origemNome,
                        destinoNome = r.value.destinoNome,
                        dataEmbarque = r.value.dataEmbarqueRaw,
                        saida = r.value.saida,
                    )
                    is AppResult.Failure -> {
                        _uiState.update { it.copy(pernasErro = r.error.message.orEmpty()) }
                        return@launch
                    }
                }
            }
            _uiState.update { it.copy(pernas = pernas) }
            carregarMapa()
        }
    }

    fun onRetryMapa() = carregarMapa()

    private fun carregarMapa() {
        val perna = _uiState.value.perna ?: return
        mapaJob?.cancel()
        mapaJob = viewModelScope.launch {
            _uiState.update { it.copy(mapa = MapaState.Loading) }
            when (val r = listarAssentos(perna.tripUuid)) {
                is AppResult.Success -> _uiState.update { it.copy(mapa = MapaState.Loaded(r.value)) }
                is AppResult.Failure -> _uiState.update { it.copy(mapa = MapaState.Error(r.error.message.orEmpty())) }
            }
        }
    }

    fun onSeatToggled(assento: AssentoOnibus) {
        _uiState.update { s ->
            // Assento que este próprio totem já bloqueou aparece "ocupado" no mapa — deixa tocar.
            val meuBloqueio = s.bloqueadas[s.pernaIndex].orEmpty().any { it.label == assento.label }
            if (assento.ocupado && !meuBloqueio) return@update s
            val atual = s.selecionados
            when {
                assento.label in atual -> s.copy(selecionados = atual - assento.label, erroBloqueio = null)
                s.quantidadeExigida != null && atual.size >= s.quantidadeExigida -> s
                s.quantidadeExigida == null && atual.size >= MAX_ASSENTOS -> s
                else -> s.copy(selecionados = atual + assento.label, erroBloqueio = null)
            }
        }
    }

    fun onConfirm() {
        val s = _uiState.value
        val perna = s.perna ?: return
        if (!s.canConfirm) return
        val jaBloqueados = s.bloqueadas[s.pernaIndex].orEmpty()
        // Mesma seleção já bloqueada (voltou pra tela e não mudou nada) → só avança.
        if (jaBloqueados.map { it.label }.toSet() == s.selecionados.toSet()) {
            avancar()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(bloqueando = true, erroBloqueio = null) }
            // Troca de assentos: libera os bloqueados antes nesta perna.
            if (jaBloqueados.isNotEmpty()) {
                val ids = jaBloqueados.map { it.seatId }
                AppScope.launch { desbloquearAssentos(perna.tripUuid, ids) }
            }
            val tripLeg = if (s.totalPernas > 1) perna.tripLeg else null
            when (val r = bloquearAssentos(perna.tripUuid, s.selecionados, tripLeg)) {
                is AppResult.Success -> {
                    val bloqueadas = s.bloqueadas + (s.pernaIndex to r.value)
                    _uiState.update { it.copy(bloqueando = false, bloqueadas = bloqueadas) }
                    publicarNaSessao(bloqueadas)
                    avancar()
                }
                is AppResult.Failure -> {
                    // Assento acabou de ser vendido/reservado: mostra o motivo e recarrega o mapa.
                    val bloqueadas = s.bloqueadas - s.pernaIndex
                    _uiState.update {
                        it.copy(
                            bloqueando = false,
                            bloqueadas = bloqueadas,
                            selecionados = emptyList(),
                            erroBloqueio = r.error.message.orEmpty(),
                        )
                    }
                    publicarNaSessao(bloqueadas)
                    carregarMapa()
                }
            }
        }
    }

    private fun publicarNaSessao(bloqueadas: Map<Int, List<AssentoBloqueado>>) {
        val pernas = _uiState.value.pernas.orEmpty()
        val selecionadas = pernas.mapIndexedNotNull { i, p ->
            val assentos = bloqueadas[i] ?: return@mapIndexedNotNull null
            PernaSelecionada(
                tripUuid = p.tripUuid,
                tripLeg = p.tripLeg,
                origemSlug = p.origemSlug,
                destinoSlug = p.destinoSlug,
                origemNome = p.origemNome,
                destinoNome = p.destinoNome,
                dataEmbarque = p.dataEmbarque,
                saida = p.saida,
                assentos = assentos,
            )
        }
        session.setPernas(sentido, selecionadas)
    }

    private fun avancar() {
        val s = _uiState.value
        val proxima = s.pernaIndex + 1
        if (proxima < s.totalPernas) {
            _uiState.update {
                it.copy(
                    pernaIndex = proxima,
                    selecionados = it.bloqueadas[proxima].orEmpty().map { a -> a.label },
                    quantidadeExigida = quantidadeExigida(proxima, it.bloqueadas),
                    erroBloqueio = null,
                )
            }
            carregarMapa()
        } else {
            _uiState.update { it.copy(concluido = true) }
        }
    }

    fun onDismissError() = _uiState.update { it.copy(erroBloqueio = null) }

    /** A tela consumiu o "concluído" (navegou). Volta a permitir confirmar ao retornar. */
    fun onNavigated() {
        _uiState.update { it.copy(concluido = false) }
    }

    /** Voltar dentro da tela: perna anterior (mantém os bloqueios). Retorna false na 1ª perna. */
    fun onBackPerna(): Boolean {
        val s = _uiState.value
        if (s.pernaIndex == 0) return false
        val anterior = s.pernaIndex - 1
        _uiState.update {
            it.copy(
                pernaIndex = anterior,
                selecionados = it.bloqueadas[anterior].orEmpty().map { a -> a.label },
                quantidadeExigida = quantidadeExigida(anterior, it.bloqueadas),
                erroBloqueio = null,
            )
        }
        carregarMapa()
        return true
    }

    override fun onCleared() {
        // Tela saiu da pilha (cliente voltou pra lista de viagens, ou o fluxo acabou). Sem venda
        // paga, os bloqueios deste sentido são liberados — mesmo papel do RoomViewModel.onCleared.
        if (!session.value.pago) session.liberar(sentido)
        super.onCleared()
    }

    companion object {
        const val ARG_SENTIDO = "sentido"

        /** Teto de assentos por compra no totem (o site não limita; evita seleção acidental em massa). */
        const val MAX_ASSENTOS = 6
    }
}
