package com.example.passagenexpress.core.printer.escpos

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * Acumulador de bytes ESC/POS com os comandos usados pelo bilhete. O alvo é uma 58mm
 * genérica (384 dots, ~32 colunas na Fonte A). Texto é encodado em CP850 (PC-850
 * Multilingual) pra sair com acento — code page selecionada no [reset] via `ESC t`.
 */
internal class EscPosWriter(
    private val charset: Charset = runCatching { Charset.forName("IBM850") }.getOrElse { Charsets.US_ASCII },
) {
    private val out = ByteArrayOutputStream()

    fun bytes(): ByteArray = out.toByteArray()

    fun raw(vararg b: Int): EscPosWriter = apply { b.forEach { out.write(it and 0xFF) } }

    fun raw(b: ByteArray): EscPosWriter = apply { out.write(b) }

    /** `ESC @` (init) + seleciona code page PC-850 (`ESC t 2`). */
    fun reset(): EscPosWriter = raw(0x1B, 0x40).raw(0x1B, 0x74, 0x02)

    fun align(a: Align): EscPosWriter = raw(0x1B, 0x61, a.n)

    fun bold(on: Boolean): EscPosWriter = raw(0x1B, 0x45, if (on) 1 else 0)

    fun underline(on: Boolean): EscPosWriter = raw(0x1B, 0x2D, if (on) 1 else 0)

    /** `GS ! n` — magnitude de largura/altura (1 = normal, 2 = dobro). */
    fun size(width: Int = 1, height: Int = 1): EscPosWriter {
        val w = (width.coerceIn(1, 8) - 1) shl 4
        val h = (height.coerceIn(1, 8) - 1)
        return raw(0x1D, 0x21, w or h)
    }

    fun text(s: String): EscPosWriter = raw(s.toByteArray(charset))

    fun line(s: String = ""): EscPosWriter = text(s).raw(0x0A)

    fun feed(lines: Int): EscPosWriter = raw(0x1B, 0x64, lines.coerceIn(0, 255))

    /** Corte total (`GS V 0`); inofensivo em impressoras sem guilhotina. */
    fun cut(): EscPosWriter = feed(3).raw(0x1D, 0x56, 0x00)

    companion object {
        /** Colunas na Fonte A do 58mm. */
        const val COLUMNS = 32
    }

    enum class Align(val n: Int) { LEFT(0), CENTER(1), RIGHT(2) }
}
