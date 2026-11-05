package com.raghu.photostudio.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChitFundTest {

    private fun input(month: Int = 14, auction: Double = 20_000.0) =
        ChitFund.Input(300_000.0, 20, 20, 4.0, month, auction)

    @Test
    fun websiteDefaultExample() {
        val (r, p) = ChitFund.calculate(input())
        assertNull(p)
        r!!
        assertEquals(15_000.0, r.monthlyContribution, 0.001)
        assertEquals(12_000.0, r.commission, 0.001)
        assertEquals(400.0, r.dividendPerMember, 0.001)
        assertEquals(14_600.0, r.netPayablePerMember, 0.001)
        assertEquals(280_000.0, r.prizeAmount, 0.001)
        assertEquals(6, r.remainingMonths)
        // 8000 / (280000 × 6/12) × 100 — website shows 5.71% and ₹0.48
        assertEquals("5.71", "%.2f".format(r.annualRatePercent))
        assertEquals("0.48", "%.2f".format(r.ratePer100PerMonth))
    }

    @Test
    fun lastMonthHasNoInterestRate() {
        val (r, _) = ChitFund.calculate(input(month = 20))
        assertEquals(0, r!!.remainingMonths)
        assertNull(r.annualRatePercent)
        assertNull(r.ratePer100PerMonth)
    }

    @Test
    fun badInputs() {
        assertEquals(ChitFund.Problem.AUCTION_MONTH, ChitFund.calculate(input(month = 21)).second)
        assertEquals(ChitFund.Problem.AUCTION_MONTH, ChitFund.calculate(input(month = 0)).second)
        assertEquals(ChitFund.Problem.AUCTION_TOO_BIG, ChitFund.calculate(input(auction = 300_000.0)).second)
        assertEquals(
            ChitFund.Problem.MISSING,
            ChitFund.calculate(ChitFund.Input(0.0, 20, 20, 4.0, 1, 0.0)).second,
        )
    }
}
