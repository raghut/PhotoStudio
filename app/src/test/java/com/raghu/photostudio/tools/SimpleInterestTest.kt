package com.raghu.photostudio.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SimpleInterestTest {

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Test
    fun twoRupeesOnTenThousandForTenMonths() {
        val r = SimpleInterest.calculate(10_000.0, 2.0, d(2026, 1, 5), d(2026, 11, 5))!!
        assertEquals(200.0, r.interestPerMonth, 0.001)
        assertEquals(10, r.fullMonths)
        assertEquals(0, r.extraDays)
        assertEquals(10, r.months.size)
        assertEquals(2_000.0, r.totalInterest, 0.001)
        assertEquals(12_000.0, r.totalAmount, 0.001)
        assertEquals(24.0, r.percentPerYear, 0.001)
    }

    @Test
    fun leftoverDaysAreAPartOfAThirtyDayMonth() {
        // 3 months and 15 days at ₹1.5 on ₹20,000 → ₹300/month → 900 + 150
        val r = SimpleInterest.calculate(20_000.0, 1.5, d(2026, 2, 10), d(2026, 5, 25))!!
        assertEquals(3, r.fullMonths)
        assertEquals(15, r.extraDays)
        assertEquals(4, r.months.size)
        assertEquals(15, r.months.last().partDays)
        assertEquals(150.0, r.months.last().interest, 0.001)
        assertEquals(1_050.0, r.totalInterest, 0.001)
        assertEquals(21_050.0, r.months.last().interestSoFar + 20_000.0, 0.001)
    }

    @Test
    fun monthEndsDoNotDrift() {
        // 31 Jan → 28 Feb → 31 Mar: two full months exactly.
        val r = SimpleInterest.calculate(1_000.0, 1.0, d(2026, 1, 31), d(2026, 3, 31))!!
        assertEquals(2, r.fullMonths)
        assertEquals(0, r.extraDays)
        assertEquals(d(2026, 2, 28), r.months[0].to)
    }

    @Test
    fun lessThanAMonth() {
        val r = SimpleInterest.calculate(5_000.0, 2.0, d(2026, 11, 1), d(2026, 11, 11))!!
        assertEquals(0, r.fullMonths)
        assertEquals(10, r.extraDays)
        assertEquals(100.0 * 10 / 30, r.totalInterest, 0.001)
    }

    @Test
    fun sameDayIsNoInterest() {
        val r = SimpleInterest.calculate(5_000.0, 2.0, d(2026, 11, 1), d(2026, 11, 1))!!
        assertEquals(0.0, r.totalInterest, 0.0)
        assertEquals(0, r.months.size)
    }

    @Test
    fun badInputsGiveNothing() {
        assertNull(SimpleInterest.calculate(0.0, 2.0, d(2026, 1, 1), d(2026, 2, 1)))
        assertNull(SimpleInterest.calculate(1000.0, 2.0, d(2026, 2, 1), d(2026, 1, 1)))
    }

    @Test
    fun indianNumberFormat() {
        assertEquals("₹1,00,000", Money.rupees(100_000.0))
        assertEquals("₹12,34,567.50", Money.rupees(1_234_567.5))
        assertEquals("₹999", Money.rupees(999.0))
        assertEquals("₹33.33", Money.rupees(100.0 / 3))
        assertEquals("1.5", Money.plain(1.5))
        assertEquals("2", Money.plain(2.0))
        assertEquals(10_000.0, Money.parse("10,000")!!, 0.0)
        assertEquals("05-11-2026", Money.date(d(2026, 11, 5)))
    }
}
