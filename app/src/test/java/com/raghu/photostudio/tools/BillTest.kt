package com.raghu.photostudio.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BillTest {

    private val labels = Bill.Labels(
        "Bill", "Date", "Customer", "Item", "Qty", "Rate", "Amount",
        "Subtotal", "Discount", "Total", "Received", "Balance", "Thank you!",
    )

    private fun bill(discount: Double = 0.0, received: Double? = null) = Bill(
        number = 7,
        date = LocalDate.of(2026, 11, 5),
        shopName = "Sri Photo Studio",
        shopDetails = "Main Road · 98480 12345",
        customerName = "Ravi",
        customerPhone = "",
        items = listOf(
            Bill.Item("Passport photos", 2.0, 50.0),
            Bill.Item("Xerox", 10.0, 2.0),
            Bill.Item("Lamination", 1.0, 30.5),
        ),
        discount = discount,
        received = received,
    )

    @Test
    fun sums() {
        val b = bill(discount = 10.0, received = 100.0)
        assertEquals(150.5, b.subtotal, 0.001)
        assertEquals(140.5, b.total, 0.001)
        assertEquals(40.5, b.balance!!, 0.001)
        assertNull(bill().balance)
    }

    @Test
    fun discountNeverMakesTotalNegative() {
        assertEquals(0.0, bill(discount = 1000.0).total, 0.0)
    }

    @Test
    fun whatsAppText() {
        val t = bill(discount = 10.0, received = 100.0).toText(labels)
        assertTrue(t.startsWith("*Sri Photo Studio*\nMain Road · 98480 12345\nBill #7 · Date: 05-11-2026\nCustomer: Ravi\n"))
        assertTrue(t.contains("1. Passport photos — 2 × ₹50 = ₹100\n"))
        assertTrue(t.contains("3. Lamination — 1 × ₹30.50 = ₹30.50\n"))
        assertTrue(t.contains("Discount: − ₹10\n*Total: ₹140.50*\nReceived: ₹100\n*Balance: ₹40.50*\nThank you!"))
    }

    @Test
    fun plainBillSkipsEmptyLines() {
        val t = bill().copy(customerName = "", shopDetails = "").toText(labels)
        assertFalse(t.contains("Customer"))
        assertFalse(t.contains("Discount"))
        assertFalse(t.contains("Balance"))
    }

    @Test
    fun phoneNumbersForWhatsApp() {
        assertEquals("919848012345", Bill.whatsAppNumber("98480 12345"))
        assertEquals("919848012345", Bill.whatsAppNumber("+91 98480-12345"))
        assertEquals("919848012345", Bill.whatsAppNumber("09848012345"))
        assertNull(Bill.whatsAppNumber("12345"))
        assertNull(Bill.whatsAppNumber(""))
    }
}
