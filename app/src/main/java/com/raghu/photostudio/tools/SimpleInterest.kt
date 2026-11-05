package com.raghu.photostudio.tools

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Village-style simple interest ("vaddi").
 *
 * The rate is said the way people say it at the shop: "₹2 for every ₹100, per month"
 * (so ₹2 vaddi on ₹10,000 is ₹200 a month). No percentages to type.
 *
 * Time is counted in calendar months from the day the money was given: 5 Jan → 5 Feb is one
 * month, whatever the length of January. Days left over after the last full month are charged
 * as a part of a month, counting a month as 30 days (the usual way: 15 days = half a month).
 */
object SimpleInterest {

    /** Days in a month when charging for leftover days. */
    const val DAYS_PER_MONTH = 30

    data class Month(
        /** 1, 2, 3 … */
        val number: Int,
        val from: LocalDate,
        val to: LocalDate,
        /** Days in this row when it's a part month; null for a full month. */
        val partDays: Int?,
        val interest: Double,
        /** Interest added up from month 1 to this row. */
        val interestSoFar: Double,
    )

    data class Result(
        val principal: Double,
        /** Rupees for every ₹100, per month. */
        val ratePer100: Double,
        val given: LocalDate,
        val returned: LocalDate,
        val fullMonths: Int,
        val extraDays: Int,
        val totalDays: Long,
        val interestPerMonth: Double,
        val months: List<Month>,
        val totalInterest: Double,
        val totalAmount: Double,
    ) {
        /** The same rate as a yearly percentage, e.g. ₹2 per 100 per month = 24% a year. */
        val percentPerYear: Double get() = ratePer100 * 12
    }

    /**
     * Returns null when the inputs can't make a calculation
     * (no amount, negative rate, or the return date is before the given date).
     */
    fun calculate(principal: Double, ratePer100: Double, given: LocalDate, returned: LocalDate): Result? {
        if (principal <= 0 || ratePer100 < 0 || returned.isBefore(given)) return null

        val perMonth = principal * ratePer100 / 100.0

        // Always step from the start date (not month-to-month) so 31 Jan → 28 Feb → 31 Mar, no drift.
        var full = 0
        while (!given.plusMonths((full + 1).toLong()).isAfter(returned)) full++
        val lastFullEnd = given.plusMonths(full.toLong())
        val extraDays = ChronoUnit.DAYS.between(lastFullEnd, returned).toInt()

        val rows = ArrayList<Month>(full + 1)
        var soFar = 0.0
        for (i in 0 until full) {
            soFar += perMonth
            rows += Month(
                number = i + 1,
                from = given.plusMonths(i.toLong()),
                to = given.plusMonths((i + 1).toLong()),
                partDays = null,
                interest = perMonth,
                interestSoFar = soFar,
            )
        }
        if (extraDays > 0) {
            val part = perMonth * extraDays / DAYS_PER_MONTH
            soFar += part
            rows += Month(
                number = full + 1,
                from = lastFullEnd,
                to = returned,
                partDays = extraDays,
                interest = part,
                interestSoFar = soFar,
            )
        }

        return Result(
            principal = principal,
            ratePer100 = ratePer100,
            given = given,
            returned = returned,
            fullMonths = full,
            extraDays = extraDays,
            totalDays = ChronoUnit.DAYS.between(given, returned),
            interestPerMonth = perMonth,
            months = rows,
            totalInterest = soFar,
            totalAmount = principal + soFar,
        )
    }
}
