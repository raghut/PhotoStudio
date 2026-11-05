package com.raghu.photostudio.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateInputTest {

    @Test
    fun typedDigitsBecomeADate() {
        assertEquals(LocalDate.of(2026, 11, 5), DateInput.parse("05112026"))
        assertEquals(LocalDate.of(2024, 2, 29), DateInput.parse("29022024"))
    }

    @Test
    fun unfinishedOrImpossibleDatesAreNull() {
        assertNull(DateInput.parse(""))
        assertNull(DateInput.parse("051120"))
        assertNull(DateInput.parse("31022026"))
        assertNull(DateInput.parse("05132026"))
        assertNull(DateInput.parse("00112026"))
        assertNull(DateInput.parse("29022026"))
    }

    @Test
    fun roundTripsWithTheCalendar() {
        val d = LocalDate.of(2026, 1, 7)
        assertEquals("07012026", DateInput.digits(d))
        assertEquals(d, DateInput.parse(DateInput.digits(d)))
    }

    @Test
    fun cleanKeepsEightDigitsOnly() {
        assertEquals("05112026", DateInput.clean("05-11-2026"))
        assertEquals("05112026", DateInput.clean("0511202699"))
        assertEquals("0511", DateInput.clean("05/11"))
    }

    @Test
    fun dashesAndCursor() {
        assertEquals("05", DateInput.withDashes("05"))
        assertEquals("05-1", DateInput.withDashes("051"))
        assertEquals("05-11-2026", DateInput.withDashes("05112026"))
        // Every digit position maps into the dashed text and back.
        val digits = "05112026"
        for (o in 0..digits.length) {
            val t = DateInput.toDashed(o)
            assert(t <= DateInput.withDashes(digits).length)
            assertEquals(o, DateInput.toDigits(t, digits.length))
        }
        // Positions in a short value never go past its end.
        assertEquals(3, DateInput.toDigits(6, 3))
    }
}
