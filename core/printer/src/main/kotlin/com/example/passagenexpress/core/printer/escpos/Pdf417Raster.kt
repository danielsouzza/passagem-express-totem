package com.example.passagenexpress.core.printer.escpos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

/**
 * Converte o PNG do PDF417 (vindo como data URI base64 no map do bilhete) em um comando
 * raster ESC/POS `GS v 0`. O backend já gera a imagem pronta — aqui só decodifica,
 * reescala pra largura da bobina e binariza.
 */
internal object Pdf417Raster {

    /** Largura máxima imprimível de uma 58mm (em dots). Múltiplo de 8. */
    private const val MAX_WIDTH = 384

    /**
     * @return bytes ESC/POS prontos pra enviar, ou null se a imagem não pôde ser decodificada.
     */
    fun toRaster(dataUri: String?, maxWidth: Int = MAX_WIDTH): ByteArray? {
        if (dataUri.isNullOrBlank()) return null
        val bitmap = decode(dataUri) ?: return null
        // PDF417: só reduz (nunca amplia) — ampliar borraria o código de barras.
        val scaled = scaleToWidth(bitmap, maxWidth, allowUpscale = false)
        return rasterize(scaled)
    }

    /**
     * Converte um PNG/JPG cru (ex.: logomarca baixada por HTTP) em raster ESC/POS.
     * @param upscale se true, amplia imagens menores que [maxWidth] até essa largura (usado no
     *   logo, que costuma vir pequeno do backend). Barcodes NÃO devem ampliar.
     * @return bytes prontos, ou null se não decodificar.
     */
    fun toRasterFromBytes(
        bytes: ByteArray?,
        maxWidth: Int = MAX_WIDTH,
        upscale: Boolean = false,
    ): ByteArray? {
        if (bytes == null || bytes.isEmpty()) return null
        val bitmap = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
            ?: return null
        return rasterize(scaleToWidth(bitmap, maxWidth, allowUpscale = upscale))
    }

    private fun decode(dataUri: String): Bitmap? = runCatching {
        // "data:image/png;base64, iVBOR..." — corta o prefixo e tira espaços/quebras.
        val base64 = dataUri.substringAfter(",", dataUri).filterNot { it.isWhitespace() }
        val raw = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(raw, 0, raw.size)
    }.getOrNull()

    private fun scaleToWidth(src: Bitmap, maxWidth: Int, allowUpscale: Boolean): Bitmap {
        if (src.width == maxWidth) return src
        // Sem ampliar: imagem já cabe → mantém.
        if (!allowUpscale && src.width < maxWidth) return src
        val ratio = maxWidth.toFloat() / src.width
        val height = (src.height * ratio).toInt().coerceAtLeast(1)
        // Ampliação (logo) usa filtro bilinear pra suavizar; redução mantém nearest.
        return Bitmap.createScaledBitmap(src, maxWidth, height, allowUpscale)
    }

    private fun rasterize(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerRow = (width + 7) / 8
        val data = ByteArray(bytesPerRow * height)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val px = pixels[y * width + x]
                val a = (px ushr 24) and 0xFF
                val r = (px ushr 16) and 0xFF
                val g = (px ushr 8) and 0xFF
                val b = px and 0xFF
                val luma = (r * 299 + g * 587 + b * 114) / 1000
                // Pixel preto = transparente conta como branco; luma baixa = imprime.
                val black = a > 32 && luma < 128
                if (black) {
                    data[y * bytesPerRow + (x / 8)] =
                        (data[y * bytesPerRow + (x / 8)].toInt() or (0x80 ushr (x % 8))).toByte()
                }
            }
        }
        val header = byteArrayOf(
            0x1D, 0x76, 0x30, 0x00,
            (bytesPerRow and 0xFF).toByte(), ((bytesPerRow ushr 8) and 0xFF).toByte(),
            (height and 0xFF).toByte(), ((height ushr 8) and 0xFF).toByte(),
        )
        return header + data
    }
}
