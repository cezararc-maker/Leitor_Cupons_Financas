package br.com.leitorcuponsfinancas.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PurchaseDateFormatterTest {

    @Test
    fun insertsDateSeparatorsWhileTyping() {
        assertEquals("2", PurchaseDateFormatter.fromTyping("2"))
        assertEquals("23", PurchaseDateFormatter.fromTyping("23"))
        assertEquals("23/0", PurchaseDateFormatter.fromTyping("230"))
        assertEquals("23/09", PurchaseDateFormatter.fromTyping("2309"))
        assertEquals("23/09/2", PurchaseDateFormatter.fromTyping("23092"))
        assertEquals("23/09/2026", PurchaseDateFormatter.fromTyping("23092026"))
    }

    @Test
    fun keepsOnlyEightDateDigits() {
        assertEquals(
            "23/09/2026",
            PurchaseDateFormatter.fromTyping("23/09/202699"),
        )
    }

    @Test
    fun parsesAndFormatsBrazilianDate() {
        val date = LocalDate.of(2026, 9, 23)

        assertEquals(date, PurchaseDateFormatter.parse("23/09/2026"))
        assertEquals("23/09/2026", PurchaseDateFormatter.format(date))
        assertNull(PurchaseDateFormatter.parse("31/02/2026"))
    }
}
