package com.example.passagenexpress.feature.print

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.PassagemPedido
import com.example.passagenexpress.core.domain.usecase.ImprimirBilheteUseCase
import com.example.passagenexpress.core.domain.usecase.ObterBilheteMapeadoUseCase
import com.example.passagenexpress.core.domain.usecase.ObterPassagensDoPedidoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Orquestra a etapa final: confirma as passagens do pedido pago (`gerar-passagens`) e, para
 * cada `passageiro_viagem_id`, busca o map do bilhete e imprime na térmica USB — uma a uma.
 * Em caso de falha, mantém o índice pra que o retry retome de onde parou.
 */
@HiltViewModel
class PrintViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obterPassagensDoPedido: ObterPassagensDoPedidoUseCase,
    private val obterBilheteMapeado: ObterBilheteMapeadoUseCase,
    private val imprimirBilhete: ImprimirBilheteUseCase,
) : ViewModel() {

    private val pedidoId: Long = savedStateHandle.get<Long>(ARG_PEDIDO_ID) ?: 0L

    private val _uiState = MutableStateFlow<PrintUiState>(PrintUiState.Preparing)
    val uiState: StateFlow<PrintUiState> = _uiState.asStateFlow()

    private var passagens: List<PassagemPedido> = emptyList()
    private var nextIndex: Int = 0
    private var job: Job? = null

    init {
        start()
    }

    /** (Re)inicia a partir do zero: confirma passagens e imprime todas. */
    fun onRetry() {
        if (passagens.isEmpty()) start() else printFrom(nextIndex)
    }

    private fun start() {
        job?.cancel()
        job = viewModelScope.launch {
            _uiState.value = PrintUiState.Preparing
            when (val r = obterPassagensDoPedido(pedidoId)) {
                is AppResult.Success -> {
                    passagens = r.value
                    nextIndex = 0
                    if (passagens.isEmpty()) {
                        _uiState.value = PrintUiState.Failed(
                            message = "Nenhuma passagem encontrada para impressão.",
                            current = 0,
                            total = 0,
                        )
                    } else {
                        printFrom(0)
                    }
                }
                is AppResult.Failure -> _uiState.value = PrintUiState.Failed(
                    message = r.error.message ?: "Erro ao obter as passagens.",
                    current = 0,
                    total = 0,
                )
            }
        }
    }

    private fun printFrom(startIndex: Int) {
        job?.cancel()
        job = viewModelScope.launch {
            val total = passagens.size
            for (index in startIndex until total) {
                nextIndex = index
                _uiState.value = PrintUiState.Printing(current = index + 1, total = total)
                val passagem = passagens[index]

                val bilhete = when (val r = obterBilheteMapeado(passagem.id)) {
                    is AppResult.Success -> r.value
                    is AppResult.Failure -> {
                        _uiState.value = PrintUiState.Failed(
                            message = r.error.message ?: "Erro ao obter o bilhete.",
                            current = index + 1,
                            total = total,
                        )
                        return@launch
                    }
                }

                when (val r = imprimirBilhete(bilhete)) {
                    is AppResult.Success -> Unit
                    is AppResult.Failure -> {
                        _uiState.value = PrintUiState.Failed(
                            message = r.error.message ?: "Erro ao imprimir o bilhete.",
                            current = index + 1,
                            total = total,
                        )
                        return@launch
                    }
                }
            }
            nextIndex = total
            _uiState.value = PrintUiState.Done
        }
    }

    companion object {
        const val ARG_PEDIDO_ID = "pedidoId"
    }
}
