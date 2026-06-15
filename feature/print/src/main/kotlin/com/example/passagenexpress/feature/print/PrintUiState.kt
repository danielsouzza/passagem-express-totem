package com.example.passagenexpress.feature.print

/** Estágios da etapa final de impressão dos bilhetes. */
sealed interface PrintUiState {
    /** Confirmando o pagamento e materializando as passagens (`gerar-passagens`). */
    data object Preparing : PrintUiState

    /** Imprimindo a passagem [current] de [total]. */
    data class Printing(val current: Int, val total: Int) : PrintUiState

    /** Todas as passagens foram impressas. */
    data object Done : PrintUiState

    /**
     * Falha ao preparar ou imprimir. [current]/[total] indicam onde parou; ao tentar de
     * novo, retoma da passagem que faltou (não reimprime as anteriores).
     */
    data class Failed(
        val message: String,
        val current: Int,
        val total: Int,
    ) : PrintUiState
}
