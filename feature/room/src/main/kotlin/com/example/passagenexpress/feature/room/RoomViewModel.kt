package com.example.passagenexpress.feature.room

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.common.coroutine.AppScope
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.Comodo
import com.example.passagenexpress.core.domain.model.ComodoLivre
import com.example.passagenexpress.core.domain.model.TipoComodo
import com.example.passagenexpress.core.domain.model.Trecho
import com.example.passagenexpress.core.domain.usecase.DeletarReservaUseCase
import com.example.passagenexpress.core.domain.usecase.IniciarVendaUseCase
import com.example.passagenexpress.core.domain.usecase.ListarCamarotesUseCase
import com.example.passagenexpress.core.domain.usecase.ListarComodosLivresUseCase
import com.example.passagenexpress.core.domain.usecase.ListarPoltronasUseCase
import com.example.passagenexpress.core.domain.usecase.ReservarComodoUseCase
import com.example.passagenexpress.feature.room.navigation.decodeTrechoArg
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listarPoltronas: ListarPoltronasUseCase,
    private val listarCamarotes: ListarCamarotesUseCase,
    private val listarComodosLivres: ListarComodosLivresUseCase,
    private val reservarComodo: ReservarComodoUseCase,
    private val deletarReserva: DeletarReservaUseCase,
    private val iniciarVenda: IniciarVendaUseCase,
) : ViewModel() {

    /** Raw nav arg para repassar adiante (Passenger → Payment) sem re-encodar. */
    val rawTrechoArg: String = savedStateHandle.get<String>(ARG_TRECHO).orEmpty()

    private val trecho: Trecho = decodeTrechoArg(rawTrechoArg).toDomain()

    /**
     * Vira `true` quando a venda é iniciada (holds viram um pedido). A partir daí NÃO liberamos as
     * reservas ao sair da tela — quem cuida delas passa a ser o ciclo do pedido/pagamento.
     */
    @Volatile
    private var saleCommitted = false

    private val _uiState = MutableStateFlow(RoomUiState(trecho = trecho))
    val uiState: StateFlow<RoomUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    /**
     * Consome o evento one-shot de "venda iniciada" depois que a tela já navegou pra Passenger.
     * Sem isso, o StateFlow re-emite `saleStarted` quando o usuário volta pra cá e o
     * LaunchedEffect re-dispara a navegação, re-avançando sozinho.
     */
    fun onSaleStartedHandled() = _uiState.update { it.copy(saleStarted = null) }

    fun onClickComodoIndividual(comodo: Comodo) {
        if (_uiState.value.reservaInFlight) return
        val current = _uiState.value.selecionados
        val alreadySelected = current.porId.any { it.id == comodo.id }
        if (alreadySelected) {
            cancelarReserva(comodo)
        } else {
            criarReserva(comodo)
        }
    }

    fun onIncrementTipo(tipoId: Long) {
        val livre = livreById(tipoId) ?: return
        val current = _uiState.value.selecionados.porTipo[tipoId] ?: 0
        if (current >= livre.quantidade) return
        _uiState.update {
            it.copy(
                selecionados = it.selecionados.copy(
                    porTipo = it.selecionados.porTipo + (tipoId to current + 1),
                ),
            )
        }
    }

    fun onDecrementTipo(tipoId: Long) {
        val current = _uiState.value.selecionados.porTipo[tipoId] ?: return
        val novo = (current - 1).coerceAtLeast(0)
        _uiState.update {
            val novoMap = if (novo == 0) {
                it.selecionados.porTipo - tipoId
            } else {
                it.selecionados.porTipo + (tipoId to novo)
            }
            it.copy(selecionados = it.selecionados.copy(porTipo = novoMap))
        }
    }

    fun onAddTipo(tipoId: Long) = onIncrementTipo(tipoId)

    fun onRemoveTipo(tipoId: Long) {
        _uiState.update {
            it.copy(
                selecionados = it.selecionados.copy(porTipo = it.selecionados.porTipo - tipoId),
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(erroTransitorio = null) }
    }

    fun onAvancar() {
        if (_uiState.value.startingSale || _uiState.value.selecionados.isEmpty()) return
        _uiState.update { it.copy(startingSale = true, erroTransitorio = null) }
        viewModelScope.launch {
            val sel = _uiState.value.selecionados
            val result = iniciarVenda(
                trechoId = trecho.id,
                viagemId = trecho.idViagem,
                tiposComodoEscolhidos = sel.porTipo,
                comodosAssentosEscolhidos = sel.porId.map { it.id },
            )
            when (result) {
                is AppResult.Success -> {
                    // Venda iniciada: as reservas agora pertencem ao pedido; não liberar no onCleared.
                    saleCommitted = true
                    // Expande cada cômodo pela capacidade cheia (camarote de N → N entradas),
                    // espelhando o `populateComodos` do site: N entradas → N formulários de passageiro.
                    // A escolha de "ir sozinho" (remover ocupante extra) acontece na tela de dados.
                    val expandido = result.value.copy(
                        comodos = result.value.comodos.flatMap { c ->
                            List(c.quantidade.coerceAtLeast(1)) { c }
                        },
                    )
                    _uiState.update {
                        it.copy(saleStarted = expandido, startingSale = false)
                    }
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(
                        startingSale = false,
                        erroTransitorio = result.error.message,
                    )
                }
            }
        }
    }

    /**
     * Libera as reservas ao sair da tela sem ter iniciado a venda — cobre TODOS os caminhos de
     * saída que removem a tela da pilha: voltar, cancelar, início e back de hardware.
     *
     * Roda no [AppScope] (vida do processo) de propósito: o `viewModelScope` já está sendo
     * cancelado neste ponto, então uma chamada de rede lançada nele seria abortada. Best-effort —
     * o usuário já está saindo, falhas não sobem à UI.
     */
    override fun onCleared() {
        if (saleCommitted) return
        val ids = _uiState.value.selecionados.porId.map { it.id }
        if (ids.isEmpty()) return
        AppScope.launch {
            deletarReserva(trecho.id, trecho.idViagem, ids)
        }
    }

    private fun load() {
        _uiState.update { it.copy(content = RoomContentState.Loading) }
        viewModelScope.launch {
            val livresResult = listarComodosLivres(trecho.id, trecho.idViagem)
            val livres: List<ComodoLivre> = when (livresResult) {
                is AppResult.Success -> livresResult.value
                is AppResult.Failure -> emptyList()
            }
            val tiposIds = trecho.tiposComodos.map { it.id }
            val mode = when {
                tiposIds.contains(POLTRONA_TIPO_ID) && !trecho.poltronaLivre -> Mode.Poltronas
                tiposIds.contains(CAMAROTE_TIPO_ID) -> Mode.Camarotes
                else -> Mode.Livres
            }

            val tiposLivresPoltronas = bucketsForOutros(trecho.tiposComodos, livres)
            val tiposLivresCamarotes = bucketsForOutros(trecho.tiposComodos, livres)
            val tiposLivresAll = bucketsForLivres(trecho, livres)

            val content: RoomContentState = when (mode) {
                Mode.Poltronas -> {
                    val res = listarPoltronas(trecho.id, trecho.idViagem)
                    when (res) {
                        is AppResult.Success -> RoomContentState.Poltronas(
                            assentos = res.value[POLTRONA_TIPO_ID].orEmpty(),
                            tiposLivres = tiposLivresPoltronas,
                        )
                        is AppResult.Failure -> RoomContentState.Error(
                            res.error.message ?: "Erro ao carregar poltronas",
                        )
                    }
                }
                Mode.Camarotes -> {
                    val res = listarCamarotes(trecho.id, trecho.idViagem)
                    when (res) {
                        is AppResult.Success -> RoomContentState.Camarotes(
                            camarotes = res.value[CAMAROTE_TIPO_ID].orEmpty(),
                            tiposLivres = tiposLivresCamarotes,
                        )
                        is AppResult.Failure -> RoomContentState.Error(
                            res.error.message ?: "Erro ao carregar camarotes",
                        )
                    }
                }
                Mode.Livres -> RoomContentState.Livres(tipos = tiposLivresAll)
            }
            _uiState.update { it.copy(content = content) }
        }
    }

    private fun criarReserva(comodo: Comodo) {
        _uiState.update { state ->
            state.copy(
                reservaInFlight = true,
                selecionados = state.selecionados.copy(
                    porId = state.selecionados.porId + comodo,
                ),
            )
        }
        viewModelScope.launch {
            when (val r = reservarComodo(trecho.id, trecho.idViagem, comodo.id)) {
                is AppResult.Success -> _uiState.update { it.copy(reservaInFlight = false) }
                is AppResult.Failure -> _uiState.update { state ->
                    state.copy(
                        reservaInFlight = false,
                        erroTransitorio = r.error.message,
                        selecionados = state.selecionados.copy(
                            porId = state.selecionados.porId.filterNot { it.id == comodo.id },
                        ),
                    )
                }
            }
        }
    }

    private fun cancelarReserva(comodo: Comodo) {
        _uiState.update { state ->
            state.copy(
                reservaInFlight = true,
                selecionados = state.selecionados.copy(
                    porId = state.selecionados.porId.filterNot { it.id == comodo.id },
                ),
            )
        }
        viewModelScope.launch {
            when (val r = deletarReserva(trecho.id, trecho.idViagem, listOf(comodo.id))) {
                is AppResult.Success -> _uiState.update { it.copy(reservaInFlight = false) }
                is AppResult.Failure -> _uiState.update { state ->
                    state.copy(
                        reservaInFlight = false,
                        erroTransitorio = r.error.message,
                        selecionados = state.selecionados.copy(
                            porId = state.selecionados.porId + comodo,
                        ),
                    )
                }
            }
        }
    }

    private fun livreById(tipoId: Long): ComodoLivre? {
        val current = _uiState.value.content
        val list: List<TipoComodoBucket> = when (current) {
            is RoomContentState.Livres -> current.tipos
            is RoomContentState.Poltronas -> current.tiposLivres
            is RoomContentState.Camarotes -> current.tiposLivres
            else -> emptyList()
        }
        return list.firstOrNull { it.tipo.id == tipoId }?.livre
    }

    /**
     * For Poltronas/Camarotes modes, the side cards show only "extra" types (not the main mode's
     * type). We filter out the seat-type to avoid duplicating it as a counter card.
     */
    private fun bucketsForOutros(
        tipos: List<TipoComodo>,
        livres: List<ComodoLivre>,
    ): List<TipoComodoBucket> = tipos
        .filter { it.id != POLTRONA_TIPO_ID && it.id != CAMAROTE_TIPO_ID }
        .map { tipo ->
            TipoComodoBucket(
                tipo = tipo,
                livre = livres.firstOrNull { it.tipoComodidadeId == tipo.id }
                    ?: ComodoLivre(tipoComodidadeId = tipo.id, quantidade = 0),
            )
        }

    /**
     * For Livres mode (free-seating including poltrona livre), we surface ALL listed types:
     * tipo 1 here is "poltrona livre" and is the primary pick.
     */
    private fun bucketsForLivres(
        trecho: Trecho,
        livres: List<ComodoLivre>,
    ): List<TipoComodoBucket> = trecho.tiposComodos.map { tipo ->
        TipoComodoBucket(
            tipo = tipo,
            livre = livres.firstOrNull { it.tipoComodidadeId == tipo.id }
                ?: ComodoLivre(tipoComodidadeId = tipo.id, quantidade = 0),
        )
    }

    private enum class Mode { Poltronas, Camarotes, Livres }

    companion object {
        const val ARG_TRECHO = "trechoJson"
        private const val POLTRONA_TIPO_ID = 1L
        private const val CAMAROTE_TIPO_ID = 4L
    }
}
