package com.raghu.photostudio.tools

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToLong

/** Money and number formatting the Indian way: ₹1,00,000 not ₹100,000. */
object Money {

    /** ₹1,23,456 — paise shown only when there are some (₹1,234.50). */
    fun rupees(amount: Double): String = "₹" + number(amount)

    /** 1,23,456 or 1,234.50 — rounded to paise. */
    fun number(amount: Double): String {
        val paiseTotal = (abs(amount) * 100).roundToLong()
        val whole = paiseTotal / 100
        val paise = paiseTotal % 100
        val sign = if (amount < 0 && paiseTotal != 0L) "-" else ""
        val grouped = groupIndian(whole.toString())
        return if (paise == 0L) sign + grouped else sign + grouped + "." + paise.toString().padStart(2, '0')
    }

    /** A rate like 2 → "2", 1.5 → "1.5", 1.25 → "1.25". */
    fun plain(value: Double): String {
        val r = (value * 100).roundToLong() / 100.0
        return if (r == r.toLong().toDouble()) r.toLong().toString()
        else r.toString().trimEnd('0').trimEnd('.')
    }

    /** "123456" → "1,23,456": last three digits, then groups of two. */
    private fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val last3 = digits.takeLast(3)
        var rest = digits.dropLast(3)
        val parts = ArrayList<String>()
        while (rest.length > 2) {
            parts.add(0, rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) parts.add(0, rest)
        return parts.joinToString(",") + "," + last3
    }

    /** Parses what was typed in an amount box: allows "10,000", "1.5", spaces. Null if not a number. */
    fun parse(text: String): Double? =
        text.replace(",", "").replace(" ", "").replace("₹", "").toDoubleOrNull()

    private val dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    /** 05-11-2026 — the way dates are written in the notebook. */
    fun date(d: LocalDate): String = d.format(dateFormat)
}
