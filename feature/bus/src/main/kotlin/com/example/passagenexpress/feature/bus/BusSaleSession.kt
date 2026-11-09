package com.example.passagenexpress.feature.bus

import android.util.Log
import com.example.passagenexpress.core.common.coroutine.AppScope
import com.example.passagenexpress.core.domain.model.onibus.AssentoBloqueado
import com.example.passagenexpress.core.domain.model.onibus.ContatoOnibus
import com.example.passagenexpress.core.domain.model.onibus.LocalOnibus
import com.example.passagenexpress.core.domain.model.onibus.PassageiroOnibus
import com.example.passagenexpress.core.domain.model.onibus.PrecoOnibus
import com.example.passagenexpress.core.domain.model.onibus.ViagemOnibus
import com.example.passagenexpress.core.domain.usecase.onibus.DesbloquearAssentosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

enum class Sentido { Ida, Volta }

/** Uma perna já com assentos bloqueados (direta = 1 perna; conexão = N). */
data class PernaSelecionada(
    val tripUuid: String,
    val tripLeg: Int,
    val origemSlug: String,
    val destinoSlug: String,
    val origemNome: String,
    val destinoNome: String,
    val dataEmbarque: String,
    val saida: LocalDateTime,
    val assentos: List<AssentoBloqueado>,
)

data class SelecaoViagem(
    val viagem: ViagemOnibus,
    /** Preenchida conforme cada perna é confirmada (bloqueada) na tela de assentos. */
    val pernas: List<PernaSelecionada> = emptyList(),
) {
    val completa: Boolean get() = pernas.size == viagem.escalas.size && pernas.all { it.assentos.isNotEmpty() }
}

data class BusSaleState(
    val origem: LocalOnibus? = null,
    val destino: LocalOnibus? = null,
    val idaEVolta: Boolean = false,
    val dataIda: LocalDate = LocalDate.now(),
    val dataVolta: LocalDate? = null,
    val ida: SelecaoViagem? = null,
    val volta: SelecaoViagem? = null,
    val passageiros: List<PassageiroOnibus> = emptyList(),
    val contato: ContatoOnibus? = null,
    val pedidoId: Long? = null,
    val pago: Boolean = false,
) {
    fun selecao(sentido: Sentido): SelecaoViagem? = if (sentido == Sentido.Ida) ida else volta

    /** Quantidade de passageiros = assentos da 1ª perna da ida (as demais pernas e a volta espelham). */
    val quantidadePassageiros: Int get() = ida?.pernas?.firstOrNull()?.assentos?.size ?: 0

    /** Todas as pernas bloqueadas (ida + volta) — o que precisa ser liberado se a venda for abandonada. */
    val todasPernas: List<PernaSelecionada> get() = ida?.pernas.orEmpty() + volta?.pernas.orEmpty()

    /** Total exibido: soma dos assentos de todas as pernas, cada um com a taxa de conveniência (+13%). */
    val totalExibido: Double
        get() = PrecoOnibus.arredondar(todasPernas.flatMap { it.assentos }.sumOf { PrecoOnibus.comTaxa(it.valor) })
}

/**
 * Estado da venda de ônibus compartilhado entre as telas do grafo `bus` (busca → viagens →
 * assentos → passageiros → pagamento → impressão). Diferente do barco, que leva tudo em args de
 * navegação: com ida/volta e conexões o estado ficaria grande demais pra trafegar em JSON na rota.
 *
 * Ciclo de vida: [iniciar] no começo do fluxo; [encerrar] quando o grafo sai da pilha (Início,
 * Cancelar, fim da compra) — libera os bloqueios de assento se a venda não foi paga.
 */
@Singleton
class BusSaleSession @Inject constructor(
    private val desbloquearAssentos: DesbloquearAssentosUseCase,
) {
    private val _state = MutableStateFlow(BusSaleState())
    val state: StateFlow<BusSaleState> = _state.asStateFlow()

    val value: BusSaleState get() = _state.value

    fun iniciar(origem: LocalOnibus) {
        liberarTudo()
        _state.value = BusSaleState(origem = origem)
    }

    fun setBusca(destino: LocalOnibus, idaEVolta: Boolean, dataIda: LocalDate, dataVolta: LocalDate?) {
        val atual = _state.value
        val mudouRota = atual.destino?.slug != destino.slug
        val mudouIda = mudouRota || atual.dataIda != dataIda
        val mudouVolta = mudouRota || !idaEVolta || atual.dataVolta != dataVolta
        // Busca diferente invalida as seleções feitas com a busca anterior.
        if (mudouIda) liberar(Sentido.Ida)
        if (mudouVolta) liberar(Sentido.Volta)
        _state.update {
            it.copy(
                destino = destino,
                idaEVolta = idaEVolta,
                dataIda = dataIda,
                dataVolta = if (idaEVolta) dataVolta else null,
                ida = if (mudouIda) null else it.ida,
                volta = if (mudouVolta) null else it.volta,
                pedidoId = null,
            )
        }
    }

    fun setData(sentido: Sentido, data: LocalDate) {
        _state.update {
            if (sentido == Sentido.Ida) it.copy(dataIda = data) else it.copy(dataVolta = data)
        }
    }

    /** Escolheu uma viagem. Trocar de viagem libera os bloqueios da anterior (mesmo sentido). */
    fun setViagem(sentido: Sentido, viagem: ViagemOnibus) {
        val atual = _state.value.selecao(sentido)
        if (atual?.viagem?.tripUuid == viagem.tripUuid) return
        liberar(sentido)
        _state.update {
            val sel = SelecaoViagem(viagem)
            if (sentido == Sentido.Ida) it.copy(ida = sel, pedidoId = null) else it.copy(volta = sel, pedidoId = null)
        }
    }

    fun setPernas(sentido: Sentido, pernas: List<PernaSelecionada>) {
        _state.update { s ->
            val sel = s.selecao(sentido) ?: return@update s
            val nova = sel.copy(pernas = pernas)
            if (sentido == Sentido.Ida) s.copy(ida = nova, pedidoId = null) else s.copy(volta = nova, pedidoId = null)
        }
    }

    /** Libera (best-effort) os bloqueios de um sentido e limpa as pernas dele. */
    fun liberar(sentido: Sentido) {
        val pernas = _state.value.selecao(sentido)?.pernas.orEmpty()
        liberarPernas(pernas)
        _state.update { s ->
            val sel = s.selecao(sentido) ?: return@update s
            val nova = sel.copy(pernas = emptyList())
            if (sentido == Sentido.Ida) s.copy(ida = nova) else s.copy(volta = nova)
        }
    }

    fun setPassageiros(passageiros: List<PassageiroOnibus>, contato: ContatoOnibus) {
        _state.update { it.copy(passageiros = passageiros, contato = contato, pedidoId = null) }
    }

    fun setPedido(pedidoId: Long) {
        _state.update { it.copy(pedidoId = pedidoId) }
    }

    fun marcarPago() {
        _state.update { it.copy(pago = true) }
    }

    /** Fim do fluxo (grafo saiu da pilha): libera bloqueios se não pagou e zera o estado. */
    fun encerrar() {
        if (!_state.value.pago) liberarTudo()
        _state.value = BusSaleState()
    }

    private fun liberarTudo() {
        liberarPernas(_state.value.todasPernas)
    }

    private fun liberarPernas(pernas: List<PernaSelecionada>) {
        pernas.filter { it.assentos.isNotEmpty() }.forEach { perna ->
            // AppScope: a tela que pediu a liberação normalmente já está saindo (escopo cancelado).
            AppScope.launch {
                val r = desbloquearAssentos(perna.tripUuid, perna.assentos.map { it.seatId })
                Log.d(TAG, "Desbloqueio ${perna.tripUuid}/${perna.tripLeg}: $r")
            }
        }
    }

    private companion object {
        const val TAG = "BusSaleSession"
    }
}
