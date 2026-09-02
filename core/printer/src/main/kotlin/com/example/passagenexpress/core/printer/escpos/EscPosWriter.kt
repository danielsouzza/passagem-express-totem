package com.example.passagenexpress.core.printer.escpos

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.Normalizer

/**
 * Acumulador de bytes ESC/POS com os comandos usados pelo bilhete. O alvo é uma 58mm
 * genérica (384 dots, ~32 colunas na Fonte A). **A impressora do totem só aceita ASCII** e
 * trava com bytes fora de `0x20–0x7E`, então todo texto passa por [toAscii] antes de ser
 * encodado: acentos PT são transliterados (á→a, ç→c, ã→a…) e qualquer outro caractere vira
 * espaço. O [charset] é mantido só por segurança — após o sanitize a saída já é ASCII puro.
 */
internal class EscPosWriter(
    /** Colunas da Fonte A pra esta bobina (58mm=32, 80mm=48). */
    val columns: Int = COLUMNS,
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

    fun text(s: String): EscPosWriter = raw(toAscii(s).toByteArray(charset))

    fun line(s: String = ""): EscPosWriter = text(s).raw(0x0A)

    fun feed(lines: Int): EscPosWriter = raw(0x1B, 0x64, lines.coerceIn(0, 255))

    /** Corte total (`GS V 0`); inofensivo em impressoras sem guilhotina. */
    fun cut(): EscPosWriter = feed(3).raw(0x1D, 0x56, 0x00)

    companion object {
        /** Colunas na Fonte A do 58mm. */
        const val COLUMNS = 32

        /**
         * Reduz qualquer string a ASCII imprimível (`0x20–0x7E`, + `\n`/`\t`), que é o único
         * conjunto que a impressora do totem aceita sem travar:
         *  1. Símbolos tipográficos comuns (travessão, aspas curvas, reticências, nbsp, bullet)
         *     viram seus equivalentes ASCII;
         *  2. `NFKD` decompõe acentos/ordinais/ligaduras (á→a+◌́, º→o, ½→1/2…);
         *  3. marcas diacríticas combinantes são descartadas (sobra a letra-base);
         *  4. qualquer outro não-ASCII vira espaço — preserva a largura/alinhamento da coluna
         *     sem arriscar um byte que derrube a impressora.
         */
        fun toAscii(s: String): String {
            val pre = buildString(s.length) {
                for (ch in s) {
                    append(
                        when (ch) {
                            '–', '—', '−' -> "-"        // – — −
                            '“', '”', '„', '«', '»' -> "\"" // “ ” „ « »
                            '‘', '’', '‚', '`' -> "'"  // ‘ ’ ‚ `
                            '…' -> "..."                          // …
                            ' ' -> " "                            // nbsp
                            '•', '·' -> "-"                  // • ·
                            else -> ch.toString()
                        }
                    )
                }
            }
            val decomposed = Normalizer.normalize(pre, Normalizer.Form.NFKD)
            return buildString(decomposed.length) {
                for (ch in decomposed) {
                    when {
                        ch == '\n' || ch == '\t' -> append(ch)
                        ch.code in 0x20..0x7E -> append(ch)
                        ch.code in 0x0300..0x036F -> {} // diacrítico combinante — descarta
                        else -> append(' ')
                    }
                }
            }
        }
    }

    enum class Align(val n: Int) { LEFT(0), CENTER(1), RIGHT(2) }
}
