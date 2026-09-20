package br.com.leitorcuponsfinancas.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManualEntryCalculatorTest {

    @Test
    fun calculatesUnits() {
        val total = ManualEntryCalculator.calculateTotal("10", "1,50")
        assertEquals("15,00", total?.let(ManualEntryCalculator::formatMoney))
    }

    @Test
    fun calculatesKilogramsWithDecimalQuantity() {
        val total = ManualEntryCalculator.calculateTotal("0,750", "39,90")
        assertEquals("29,93", total?.let(ManualEntryCalculator::formatMoney))
    }

    @Test
    fun acceptsStoredDecimalPoint() {
        val total = ManualEntryCalculator.calculateTotal("1.5", "2.40")
        assertEquals("3,60", total?.let(ManualEntryCalculator::formatMoney))
    }

    @Test
    fun rejectsZeroOrInvalidQuantity() {
        assertNull(ManualEntryCalculator.calculateTotal("0", "10,00"))
        assertNull(ManualEntryCalculator.calculateTotal("abc", "10,00"))
    }
}
