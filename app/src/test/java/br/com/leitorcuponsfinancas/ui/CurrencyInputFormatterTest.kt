package br.com.leitorcuponsfinancas.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class CurrencyInputFormatterTest {

    @Test
    fun typesMoneyFromCentsTowardIntegers() {
        var value = CurrencyInputFormatter.fromTyping("7")
        assertEquals("0,07", value)

        value = CurrencyInputFormatter.fromTyping(value + "5")
        assertEquals("0,75", value)

        value = CurrencyInputFormatter.fromTyping(value + "2")
        assertEquals("7,52", value)

        value = CurrencyInputFormatter.fromTyping(value + "3")
        assertEquals("75,23", value)
    }

    @Test
    fun groupsThousandsWhileTyping() {
        assertEquals(
            "1.234,56",
            CurrencyInputFormatter.fromTyping("123456"),
        )
        assertEquals(
            "1.234.567,89",
            CurrencyInputFormatter.fromTyping("123456789"),
        )
    }

    @Test
    fun clearsWhenOnlyFormattingZerosRemain() {
        assertEquals("", CurrencyInputFormatter.fromTyping("0,0"))
        assertEquals("", CurrencyInputFormatter.fromTyping(""))
    }

    @Test
    fun formatsValuesAlreadyStoredInDatabaseOrOcr() {
        assertEquals("5,00", CurrencyInputFormatter.fromStoredDecimal("5"))
        assertEquals("39,90", CurrencyInputFormatter.fromStoredDecimal("39.90"))
        assertEquals("1.234,56", CurrencyInputFormatter.fromStoredDecimal("1.234,56"))
    }

    @Test
    fun parsesBrazilianFormattedMoneyForPersistence() {
        assertEquals(
            BigDecimal("75.23"),
            CurrencyInputFormatter.parse("75,23"),
        )
        assertEquals(
            BigDecimal("1234.56"),
            CurrencyInputFormatter.parse("R$ 1.234,56"),
        )
        assertNull(CurrencyInputFormatter.parse(""))
    }

    @Test
    fun showsZeroValueBeforeFirstDigitWithoutPersistingIt() {
        assertEquals("0,00", CurrencyInputFormatter.displayValue(""))
        assertEquals("75,23", CurrencyInputFormatter.displayValue("75,23"))
    }

    @Test
    fun growsValueFromVisibleZeroBaseline() {
        var displayed = CurrencyInputFormatter.displayValue("")
        assertEquals("0,00", displayed)

        var value = CurrencyInputFormatter.fromTyping(displayed + "2")
        assertEquals("0,02", value)

        displayed = CurrencyInputFormatter.displayValue(value)
        value = CurrencyInputFormatter.fromTyping(displayed + "3")
        assertEquals("0,23", value)
    }

}
