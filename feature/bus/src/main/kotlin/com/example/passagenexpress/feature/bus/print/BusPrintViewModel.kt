package com.example.passagenexpress.feature.bus.print

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.usecase.ImprimirPdfUseCase
import com.example.passagenexpress.core.domain.usecase.onibus.ObterBoardingPassUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BusPrintState {
    /** Esperando a ClickBus emitir o bilhete (boarding-pass dá 404 nos primeiros instantes). */
    data object Emitindo : BusPrintState
    data object Imprimindo : BusPrintState
    data object Concluido : BusPrintState
    data object Falhou : BusPrintState
}

@HiltViewModel
class BusPrintViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obterBoardingPass: ObterBoardingPassUseCase,
    private val imprimirPdf: ImprimirPdfUseCase,
) : ViewModel() {

    private val pedidoId: Long = savedStateHandle.get<Long>(ARG_PEDIDO_ID) ?: 0L

    private val _state = MutableStateFlow<BusPrintState>(BusPrintState.Emitindo)
    val state: StateFlow<BusPrintState> = _state.asStateFlow()

    /** PDF já baixado — "Tentar novamente" só reimprime, sem baixar de novo. */
    private var pdf: ByteArray? = null
    private var job: Job? = null

    init {
        start()
    }

    fun onRetry() = start()

    private fun start() {
        job?.cancel()
        job = viewModelScope.launch {
            val documento = pdf ?: baixar() ?: run {
                _state.value = BusPrintState.Falhou
                return@launch
            }
            pdf = documento
            _state.value = BusPrintState.Imprimindo
            when (val r = imprimirPdf(documento)) {
                is AppResult.Success -> _state.value = BusPrintState.Concluido
                is AppResult.Failure -> {
                    // Detalhe técnico só no logcat — a tela mostra a mensagem amigável.
                    Log.w(TAG, "Falha ao imprimir boarding pass (pedido=$pedidoId): ${r.error.message}")
                    _state.value = BusPrintState.Falhou
                }
            }
        }
    }

    /**
     * O bilhete só existe depois que a ClickBus confirma o pedido — antes disso o endpoint responde
     * 404. Tenta algumas vezes com intervalo antes de desistir.
     */
    private suspend fun baixar(): ByteArray? {
        _state.value = BusPrintState.Emitindo
        repeat(MAX_TENTATIVAS) { tentativa ->
            if (tentativa > 0) delay(INTERVALO_MS)
            when (val r = obterBoardingPass(pedidoId)) {
                is AppResult.Success -> return r.value
                is AppResult.Failure -> Log.d(TAG, "Boarding pass ainda indisponível (${tentativa + 1}): ${r.error.message}")
            }
        }
        Log.w(TAG, "Boarding pass não ficou disponível (pedido=$pedidoId)")
        return null
    }

    companion object {
        const val ARG_PEDIDO_ID = "pedidoId"
        private const val TAG = "BusPrint"
        private const val MAX_TENTATIVAS = 8
        private const val INTERVALO_MS = 5_000L
    }
}
