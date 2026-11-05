package com.raghu.photostudio.tools

/**
 * Chit fund ("chitti") auction calculator — the same formula as the Tool Basket website
 * (handy-finance-tools, src/routes/chit-advanced/calculations.ts).
 *
 * Example: ₹3,00,000 chit, 20 members, 20 months, 4% commission. In month 14 the winner bids
 * ₹20,000 off. The winner takes ₹2,80,000; the foreman keeps ₹12,000 commission; the remaining
 * ₹8,000 is shared by all 20 members (₹400 each), so everyone pays ₹14,600 instead of ₹15,000.
 */
object ChitFund {

    data class Input(
        val chitAmount: Double,
        val members: Int,
        val months: Int,
        val commissionPercent: Double,
        /** The month this auction happens in (1 … months). */
        val auctionMonth: Int,
        /** The discount the winner bids off the chit amount. */
        val auctionAmount: Double,
    )

    data class Result(
        /** What each member normally pays a month: chit ÷ members. */
        val monthlyContribution: Double,
        /** Foreman's commission: chit × commission %. */
        val commission: Double,
        /** Auction amount less commission, shared by all members. */
        val dividendTotal: Double,
        val dividendPerMember: Double,
        /** What each member actually pays this month. */
        val netPayablePerMember: Double,
        /** What the winner takes home: chit − auction amount. */
        val prizeAmount: Double,
        val remainingMonths: Int,
        /**
         * The winner's cost of taking the money early, as yearly interest.
         * Null in the last month (no months left, so no interest to work out).
         */
        val annualRatePercent: Double?,
    ) {
        /** Same rate in village terms: rupees for every ₹100, per month. */
        val ratePer100PerMonth: Double? get() = annualRatePercent?.div(12)
    }

    enum class Problem { MISSING, AUCTION_MONTH, AUCTION_TOO_BIG, COMMISSION }

    /** Returns either a [Result] or the first [Problem] with the inputs. */
    fun calculate(i: Input): Pair<Result?, Problem?> {
        if (i.chitAmount <= 0 || i.members < 1 || i.months < 1) return null to Problem.MISSING
        if (i.commissionPercent < 0 || i.commissionPercent > 100) return null to Problem.COMMISSION
        if (i.auctionMonth < 1 || i.auctionMonth > i.months) return null to Problem.AUCTION_MONTH
        if (i.auctionAmount < 0 || i.auctionAmount >= i.chitAmount) return null to Problem.AUCTION_TOO_BIG

        val monthly = i.chitAmount / i.members
        val commission = i.chitAmount * i.commissionPercent / 100
        val remaining = i.months - i.auctionMonth
        val dividendTotal = i.auctionAmount - commission
        val dividend = dividendTotal / i.members
        val prize = i.chitAmount - i.auctionAmount

        val annual = if (remaining > 0) dividendTotal / (prize * remaining / 12.0) * 100 else null

        return Result(
            monthlyContribution = monthly,
            commission = commission,
            dividendTotal = dividendTotal,
            dividendPerMember = dividend,
            netPayablePerMember = monthly - dividend,
            prizeAmount = prize,
            remainingMonths = remaining,
            annualRatePercent = annual,
        ) to null
    }
}
