package com.example.passagenexpress.core.printer.usb

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.passagenexpress.core.printer.escpos.Pdf417Raster
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * PDF → ESC/POS. Cada página é renderizada em alta resolução, cortada nas margens brancas (o
 * boarding pass pode vir em A4 com muita borda — sem o corte o conteúdo ficaria minúsculo na
 * bobina), reescalada pra largura imprimível e enviada em faixas raster.
 */
internal object PdfTicketRenderer {

    /** Resolução de renderização antes do corte/escala (px na largura da página). */
    private const val RENDER_WIDTH = 1240

    fun render(context: Context, pdf: ByteArray, targetWidth: Int): ByteArray {
        val file = File.createTempFile("ticket", ".pdf", context.cacheDir)
        try {
            file.writeBytes(pdf)
            val out = ByteArrayOutputStream()
            out.write(byteArrayOf(0x1B, 0x40)) // reset
            out.write(byteArrayOf(0x1B, 0x61, 0x01)) // centraliza
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    check(renderer.pageCount > 0) { "PDF sem páginas" }
                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            val scale = RENDER_WIDTH.toFloat() / page.width
                            val h = (page.height * scale).toInt().coerceAtLeast(1)
                            val full = Bitmap.createBitmap(RENDER_WIDTH, h, Bitmap.Config.ARGB_8888)
                            full.eraseColor(Color.WHITE) // PDF tem fundo transparente
                            page.render(full, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                            val cropped = cropWhite(full) ?: return@use // página em branco
                            val ratio = targetWidth.toFloat() / cropped.width
                            val scaled = Bitmap.createScaledBitmap(
                                cropped,
                                targetWidth,
                                (cropped.height * ratio).toInt().coerceAtLeast(1),
                                true,
                            )
                            out.write(Pdf417Raster.toRasterStrips(scaled))
                            out.write(byteArrayOf(0x1B, 0x64, 0x02)) // 2 linhas entre páginas
                        }
                    }
                }
            }
            out.write(byteArrayOf(0x1B, 0x64, 0x03, 0x1D, 0x56, 0x00)) // avança e corta
            return out.toByteArray()
        } finally {
            file.delete()
        }
    }

    /** Recorta o retângulo com conteúdo (pixels não-brancos) + pequena margem; null se vazio. */
    private fun cropWhite(src: Bitmap): Bitmap? {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1
        for (y in 0 until h) {
            for (x in 0 until w) {
                val p = pixels[y * w + x]
                val luma = (((p shr 16) and 0xFF) * 299 + ((p shr 8) and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
                if (luma < 200) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < 0) return null
        val pad = 8
        val left = (minX - pad).coerceAtLeast(0)
        val top = (minY - pad).coerceAtLeast(0)
        val right = (maxX + pad).coerceAtMost(w - 1)
        val bottom = (maxY + pad).coerceAtMost(h - 1)
        return Bitmap.createBitmap(src, left, top, right - left + 1, bottom - top + 1)
    }
}
