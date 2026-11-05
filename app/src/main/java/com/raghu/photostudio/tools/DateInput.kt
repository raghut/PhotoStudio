package com.raghu.photostudio.tools

import java.time.DateTimeException
import java.time.LocalDate

/**
 * A date typed on the number keyboard as 8 digits, day-month-year: "05112026" = 05-11-2026.
 * The screen shows the dashes itself, so only digits are ever typed or stored.
 */
object DateInput {

    const val LENGTH = 8

    /** Keeps only digits, at most 8 — what the text box accepts. */
    fun clean(typed: String): String = typed.filter(Char::isDigit).take(LENGTH)

    /** "05112026" → 5 Nov 2026. Null while still typing, or for a date that doesn't exist (31-02-2026). */
    fun parse(digits: String): LocalDate? {
        if (digits.length != LENGTH || !digits.all(Char::isDigit)) return null
        val day = digits.substring(0, 2).toInt()
        val month = digits.substring(2, 4).toInt()
        val year = digits.substring(4, 8).toInt()
        if (year !in 1900..2200) return null
        return try {
            LocalDate.of(year, month, day)
        } catch (e: DateTimeException) {
            null
        }
    }

    /** 5 Nov 2026 → "05112026", for filling the box from the calendar or with today's date. */
    fun digits(date: LocalDate): String =
        date.dayOfMonth.toString().padStart(2, '0') +
            date.monthValue.toString().padStart(2, '0') +
            date.year.toString().padStart(4, '0')

    /** "051120" → "05-11-20": dashes go in as soon as the next digit is typed. */
    fun withDashes(digits: String): String = buildString {
        digits.forEachIndexed { i, c ->
            if (i == 2 || i == 4) append('-')
            append(c)
        }
    }

    /** Cursor position in the digits → position in the dashed text. */
    fun toDashed(offset: Int): Int = when {
        offset <= 2 -> offset
        offset <= 4 -> offset + 1
        else -> offset + 2
    }

    /** Cursor position in the dashed text → position in the digits. */
    fun toDigits(offset: Int, digitCount: Int): Int = when {
        offset <= 2 -> offset
        offset <= 5 -> offset - 1
        else -> offset - 2
    }.coerceIn(0, digitCount)
}
