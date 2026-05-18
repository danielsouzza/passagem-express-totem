package com.example.passagenexpress.feature.passenger.format

import com.example.passagenexpress.core.domain.model.TipoDocumento
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Visible mask helpers for the custom numeric keypad. Each helper exposes:
 *  - digitsOnly(display)  → strips formatting back to raw digits (for backend payloads)
 *  - applyMask(digits)    → projects raw digits into the display format the user sees
 *
 * Keeping these as pure top-level objects so the ViewModel can call them without owning
 * any Android dependencies.
 */
object DocumentoMask {
    private const val CPF_DIGITS = 11
    private const val RG_DIGITS = 15

    fun digitsOnly(display: String, tipo: TipoDocumento): String {
        val max = when (tipo) {
            TipoDocumento.CPF -> CPF_DIGITS
            TipoDocumento.RG -> RG_DIGITS
        }
        return display.filter(Char::isDigit).take(max)
    }

    fun applyMask(digits: String, tipo: TipoDocumento): String = when (tipo) {
        TipoDocumento.CPF -> maskCpf(digits)
        TipoDocumento.RG -> digits
    }

    fun isValid(digits: String, tipo: TipoDocumento): Boolean = when (tipo) {
        TipoDocumento.CPF -> digits.length == CPF_DIGITS
        TipoDocumento.RG -> digits.length in 5..RG_DIGITS
    }

    private fun maskCpf(digits: String): String {
        if (digits.isEmpty()) return ""
        val sb = StringBuilder()
        digits.forEachIndexed { i, c ->
            when (i) {
                3, 6 -> sb.append('.')
                9 -> sb.append('-')
            }
            sb.append(c)
        }
        return sb.toString()
    }
}

object PhoneMask {
    private const val MAX = 11

    fun digitsOnly(display: String): String = display.filter(Char::isDigit).take(MAX)

    /** Aplica máscara `(##) ####-####` ou `(##) #####-####` conforme a quantidade. */
    fun applyMask(input: String): String {
        val digits = input.filter(Char::isDigit).take(MAX)
        if (digits.isEmpty()) return ""
        val sb = StringBuilder()
        digits.forEachIndexed { i, c ->
            when (i) {
                0 -> sb.append('(')
                2 -> sb.append(") ")
                7 -> if (digits.length == 11) sb.append('-')
                6 -> if (digits.length <= 10) sb.append('-')
            }
            sb.append(c)
        }
        return sb.toString()
    }
}

object BirthdateMask {
    private const val MAX = 8

    fun digitsOnly(display: String): String = display.filter(Char::isDigit).take(MAX)

    fun applyMask(input: String): String {
        val digits = input.filter(Char::isDigit).take(MAX)
        if (digits.isEmpty()) return ""
        val sb = StringBuilder()
        digits.forEachIndexed { i, c ->
            if (i == 2 || i == 4) sb.append('/')
            sb.append(c)
        }
        return sb.toString()
    }

    fun format(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

/**
 * Parses a display value like "12/03/1990" into a LocalDate. Returns null when invalid
 * (incomplete digits, impossible day/month, or future date).
 */
fun parseBirthdate(display: String): LocalDate? {
    val digits = display.filter(Char::isDigit)
    if (digits.length != 8) return null
    val day = digits.substring(0, 2).toIntOrNull() ?: return null
    val month = digits.substring(2, 4).toIntOrNull() ?: return null
    val year = digits.substring(4, 8).toIntOrNull() ?: return null
    return runCatching { LocalDate.of(year, month, day) }
        .getOrNull()
        ?.takeIf { !it.isAfter(LocalDate.now()) }
}
