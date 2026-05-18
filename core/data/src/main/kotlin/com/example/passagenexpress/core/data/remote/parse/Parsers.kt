package com.example.passagenexpress.core.data.remote.parse

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd[ HH:mm[:ss]]")
private val DATE_FORMATTER_ISO = DateTimeFormatter.ISO_LOCAL_DATE
private val DATE_FORMATTER_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm[:ss]")

/** Parses backend money string ("R$ 1.234,56" or "1234,56" or numeric "1234.56"). */
fun String?.parseMoney(): Double {
    if (isNullOrBlank()) return 0.0
    val sanitized = trim()
        .removePrefix("R$")
        .replace(" ", "")
        .replace(".", "")
        .replace(",", ".")
    return sanitized.toDoubleOrNull() ?: 0.0
}

/**
 * Backend may send:
 *  - "yyyy-MM-dd"
 *  - "yyyy-MM-dd HH:mm[:ss]"        (Laravel toDateTimeString)
 *  - "yyyy-MM-ddTHH:mm:ss[.SSSSSS][Z]" (Laravel Carbon toJson / ISO)
 *  - "dd/MM/yyyy"                    (BR display format)
 */
fun String?.parseLocalDate(): LocalDate? {
    if (isNullOrBlank()) return null
    val datePart = trim()
        .substringBefore(' ')
        .substringBefore('T')
    return runCatching { LocalDate.parse(datePart, DATE_FORMATTER_ISO) }.getOrNull()
        ?: runCatching { LocalDate.parse(datePart, DATE_FORMATTER_BR) }.getOrNull()
}

/** Parses HH:mm or HH:mm:ss; if input is "yyyy-MM-dd HH:mm[:ss]" extracts the time portion. */
fun String?.parseLocalTime(): LocalTime? {
    if (isNullOrBlank()) return null
    val timePart = if (contains(' ')) substringAfter(' ') else this!!
    return runCatching { LocalTime.parse(timePart, TIME_FORMATTER) }.getOrNull()
}

fun String?.parseLocalDateTime(): LocalDateTime? {
    if (isNullOrBlank()) return null
    return runCatching { LocalDateTime.parse(trim(), DATE_TIME_FORMATTER) }.getOrNull()
}

/** "HH:MM" or integer minutes ("90" or 90). */
fun String?.parseTempoViagemMinutos(): Int {
    if (isNullOrBlank()) return 0
    val text = trim()
    if (text.contains(':')) {
        val parts = text.split(':')
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }
    return text.toIntOrNull() ?: 0
}
