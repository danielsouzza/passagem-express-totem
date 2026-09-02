package com.example.passagenexpress.core.domain.model

data class TotemConfig(
    val subdomain: String,
    val portoId: Long,
    val portoSlug: String,
    val portoNome: String,
    val municipioCodigo: String,
    val municipioNome: String,
    val defaultLanguage: AppLanguage,
    val setupComplete: Boolean,
    /** VID/PID da impressora USB escolhida no setup; null = não configurada. */
    val printerVendorId: Int? = null,
    val printerProductId: Int? = null,
    /** Largura da bobina da impressora (define colunas/dots do bilhete). Default 58mm. */
    val printerPaperWidth: PaperWidth = PaperWidth.MM58,
    /** PIN do operador para acessar as configurações; vazio = não definido (totens antigos). */
    val operatorPin: String = "",
    /** Embarcação onde o totem está a bordo; null = não vinculado (não filtra viagens). */
    val embarcacaoId: Long? = null,
    val embarcacaoNome: String = "",
) {
    companion object {
        /** Número mínimo de dígitos do PIN de acesso às configurações. */
        const val MinPinLength = 4

        val Empty = TotemConfig(
            subdomain = "",
            portoId = 0L,
            portoSlug = "",
            portoNome = "",
            municipioCodigo = "",
            municipioNome = "",
            defaultLanguage = AppLanguage.PtBr,
            setupComplete = false,
            printerVendorId = null,
            printerProductId = null,
            printerPaperWidth = PaperWidth.MM58,
            operatorPin = "",
            embarcacaoId = null,
            embarcacaoNome = "",
        )
    }
}

enum class AppLanguage(val tag: String) {
    PtBr("pt-BR"),
    EnUs("en-US"),
}

/**
 * Largura da bobina térmica. [columns] = colunas na Fonte A (12 dots/char); [dots] = largura
 * imprimível em pontos a 203dpi. O rendering do bilhete (texto + raster do PDF417/logo) se adapta
 * a partir daqui.
 */
enum class PaperWidth(val columns: Int, val dots: Int) {
    MM58(columns = 32, dots = 384),
    // 42 colunas (não 48): a linha justificada de valores (rótulo + espaços + valor) com 48 estoura
    // a área imprimível do 80mm e o valor quebra pra linha de baixo. 42 é a largura Fonte A segura.
    MM80(columns = 42, dots = 576),
}
