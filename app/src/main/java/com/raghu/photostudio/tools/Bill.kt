package com.raghu.photostudio.tools

import java.time.LocalDate

/** A quick shop bill / receipt. Pure data and sums — drawing and sharing live elsewhere. */
data class Bill(
    val number: Int,
    val date: LocalDate,
    val shopName: String,
    /** Second header line: address, phone — whatever the shop wants under its name. */
    val shopDetails: String,
    val customerName: String,
    val customerPhone: String,
    val items: List<Item>,
    val discount: Double = 0.0,
    /** Money already received; null when not entered (then no balance line). */
    val received: Double? = null,
) {
    data class Item(val name: String, val qty: Double, val rate: Double) {
        val amount: Double get() = qty * rate
    }

    val subtotal: Double get() = items.sumOf { it.amount }
    val total: Double get() = (subtotal - discount).coerceAtLeast(0.0)
    val balance: Double? get() = received?.let { total - it }

    /** Words printed on the receipt, in the app's language. */
    data class Labels(
        val bill: String,
        val date: String,
        val customer: String,
        val item: String,
        val qty: String,
        val rate: String,
        val amount: String,
        val subtotal: String,
        val discount: String,
        val total: String,
        val received: String,
        val balance: String,
        val thanks: String,
    )

    /**
     * The bill as a WhatsApp message. *stars* make WhatsApp show bold text.
     * One line per item: "Passport photos — 2 × ₹50 = ₹100".
     */
    fun toText(l: Labels): String = buildString {
        if (shopName.isNotBlank()) appendLine("*${shopName.trim()}*")
        if (shopDetails.isNotBlank()) appendLine(shopDetails.trim())
        appendLine("${l.bill} #$number · ${l.date}: ${Money.date(date)}")
        if (customerName.isNotBlank()) appendLine("${l.customer}: ${customerName.trim()}")
        appendLine("------------------------------")
        items.forEachIndexed { i, it ->
            appendLine("${i + 1}. ${it.name.trim()} — ${Money.plain(it.qty)} × ${Money.rupees(it.rate)} = ${Money.rupees(it.amount)}")
        }
        appendLine("------------------------------")
        if (discount > 0) {
            appendLine("${l.subtotal}: ${Money.rupees(subtotal)}")
            appendLine("${l.discount}: − ${Money.rupees(discount)}")
        }
        appendLine("*${l.total}: ${Money.rupees(total)}*")
        val bal = balance
        if (received != null && bal != null) {
            appendLine("${l.received}: ${Money.rupees(received)}")
            appendLine("*${l.balance}: ${Money.rupees(bal)}*")
        }
        append(l.thanks)
    }

    companion object {
        /**
         * A phone number ready for a wa.me link: 10-digit Indian mobiles get "91" in front.
         * Null when it doesn't look like a phone number.
         */
        fun whatsAppNumber(phone: String): String? {
            val digits = phone.filter(Char::isDigit).trimStart('0')
            return when {
                digits.length == 10 -> "91$digits"
                digits.length == 12 && digits.startsWith("91") -> digits
                else -> null
            }
        }
    }
}
