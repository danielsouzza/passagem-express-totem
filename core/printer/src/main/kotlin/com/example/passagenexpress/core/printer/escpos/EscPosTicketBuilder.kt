package com.example.passagenexpress.core.printer.escpos

import com.example.passagenexpress.core.domain.model.BilheteMapeado
import com.example.passagenexpress.core.printer.escpos.EscPosWriter.Align
import com.example.passagenexpress.core.printer.escpos.EscPosWriter.Companion.COLUMNS

/**
 * Monta os bytes ESC/POS de um bilhete, espelhando o layout do bilhete real
 * (`app/bilhete-1740205.pdf`): cabeçalho da agência/empresa, embarcação, trecho, embarque,
 * viagem, tabela de valores, formas de pagamento, passageiros, número do BP-e, PDF417,
 * código de barras textual, validação, tributos e observações.
 */
internal object EscPosTicketBuilder {

    fun build(bilhete: BilheteMapeado): ByteArray {
        val w = EscPosWriter()
        w.reset()

        // --- Agência ---
        w.align(Align.CENTER).bold(true)
        bilhete.agencia.nome.ifBlankNull()?.let { w.lineWrapped(it) }
        w.bold(false)
        bilhete.agencia.cnpj.ifBlankNull()?.let { w.lineWrapped("CNPJ: $it") }
        bilhete.agencia.endereco1.ifBlankNull()?.let { w.lineWrapped(it) }
        bilhete.agencia.endereco2.ifBlankNull()?.let { w.lineWrapped(it) }
        w.line()

        // --- Empresa (transportadora) ---
        bilhete.empresa.nome.ifBlankNull()?.let { w.lineWrapped(it) }
        bilhete.empresa.cnpjCpf.ifBlankNull()?.let {
            val ie = bilhete.empresa.ie.ifBlankNull()?.let { ie -> "  IE: $ie" }.orEmpty()
            w.lineWrapped("CNPJ: $it$ie")
        }
        bilhete.empresa.endereco.ifBlankNull()?.let { w.lineWrapped(it) }
        w.lineWrapped("Documento Auxiliar do Bilhete de Passagem Eletronico")
        w.sep()

        // --- Embarcação (destaque) ---
        bilhete.embarcacao.nome.ifBlankNull()?.let {
            w.align(Align.CENTER).bold(true).size(width = 2, height = 2)
            w.line(it)
            w.size().bold(false)
        }

        // --- Trecho / Embarque ---
        w.align(Align.LEFT)
        bilhete.trecho.portoOrigem.ifBlankNull()?.let { w.lineWrapped("Porto: $it") }
        w.lineWrapped("Origem: ${bilhete.trecho.municipioOrigem}")
        w.lineWrapped("Destino: ${bilhete.trecho.municipioDestino}")
        w.lineWrapped("Data: ${bilhete.embarque.data}   Horario: ${bilhete.embarque.horario}")
        bilhete.comodo.ifBlankNull()?.let { w.lineWrapped(it) }
        bilhete.viagem.linha.ifBlankNull()?.let { w.lineWrapped("Linha: $it") }
        bilhete.viagem.tipo.ifBlankNull()?.let { w.lineWrapped("Tipo: $it") }
        bilhete.embarque.observacao.ifBlankNull()?.let { w.lineWrapped(it) }
        w.sep()

        // --- Valores ---
        w.row("Tarifa", bilhete.valores.tarifa)
        w.row("Taxa de Embarque", bilhete.valores.taxaEmbarque)
        w.row("Taxa Adicional", bilhete.valores.taxaAdicional)
        w.row("Valor Total", bilhete.valores.valorTotal)
        w.row("Desconto", bilhete.valores.desconto)
        w.bold(true).row("Valor a Pagar", bilhete.valores.valorAPagar).bold(false)
        w.sep()

        // --- Formas de pagamento ---
        if (bilhete.formasPagamento.isNotEmpty()) {
            bilhete.formasPagamento.forEach { (forma, valor) -> w.row(forma, valor) }
            bilhete.troco.ifBlankNull()?.let { w.row("Troco", it) }
            w.sep()
        }

        // --- Passageiros ---
        bilhete.passageiros.forEach { w.lineWrapped(it) }
        if (bilhete.passageiros.isNotEmpty()) w.line()

        // --- Número do bilhete ---
        bilhete.bilhete.ifBlankNull()?.let {
            w.align(Align.CENTER).bold(true).lineWrapped(it).bold(false)
        }

        // --- PDF417 ---
        Pdf417Raster.toRaster(bilhete.pdf417)?.let {
            w.align(Align.CENTER).feed(1).raw(it).feed(1)
        }
        bilhete.codigoBarras.ifBlankNull()?.let { w.align(Align.CENTER).lineWrapped(it) }

        // --- Validação / Tributos ---
        w.align(Align.CENTER)
        bilhete.validacao.ifBlankNull()?.let { w.line().lineWrapped(it) }
        bilhete.tributos.ifBlankNull()?.let { w.line().bold(true).lineWrapped(it).bold(false) }

        // --- Observações ---
        if (bilhete.observacoes.isNotEmpty()) {
            w.align(Align.LEFT).line()
            bilhete.observacoes.forEach { w.lineWrapped(it) }
        }

        w.cut()
        return w.bytes()
    }

    /** Ticket de teste curto (botão "Testar impressão" no setup). */
    fun buildTest(): ByteArray {
        val w = EscPosWriter()
        w.reset()
        w.align(Align.CENTER).bold(true).size(2, 2).line("TESTE")
        w.size().bold(false)
        w.line("Passagem Express")
        w.lineWrapped("Impressora configurada com sucesso.")
        w.line("Acentuacao: cao, coracao, ja")
        w.sep()
        w.align(Align.LEFT)
        w.row("Tarifa", "121,00")
        w.row("Total", "121,00")
        w.cut()
        return w.bytes()
    }

    // ---- helpers de layout (32 colunas) ----

    /** Linha "label .... valor" preenchida até [COLUMNS]. */
    private fun EscPosWriter.row(label: String, value: String): EscPosWriter {
        val v = value.trim()
        val space = (COLUMNS - label.length - v.length).coerceAtLeast(1)
        return line(label + " ".repeat(space) + v)
    }

    private fun EscPosWriter.sep(): EscPosWriter =
        align(Align.LEFT).line("-".repeat(COLUMNS))

    /** Quebra texto em linhas de no máximo [COLUMNS] respeitando palavras. */
    private fun EscPosWriter.lineWrapped(text: String): EscPosWriter {
        val words = text.trim().split(Regex("\\s+"))
        val sb = StringBuilder()
        for (word in words) {
            when {
                sb.isEmpty() -> sb.append(word)
                sb.length + 1 + word.length <= COLUMNS -> sb.append(' ').append(word)
                else -> {
                    line(sb.toString())
                    sb.setLength(0)
                    sb.append(word)
                }
            }
            // Palavra maior que a linha: quebra dura.
            while (sb.length > COLUMNS) {
                line(sb.substring(0, COLUMNS))
                val rest = sb.substring(COLUMNS)
                sb.setLength(0)
                sb.append(rest)
            }
        }
        if (sb.isNotEmpty()) line(sb.toString())
        return this
    }

    private fun String?.ifBlankNull(): String? = this?.takeIf { it.isNotBlank() }
}
