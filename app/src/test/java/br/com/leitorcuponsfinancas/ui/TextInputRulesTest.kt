package br.com.leitorcuponsfinancas.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TextInputRulesTest {

    @Test
    fun capitalizesFirstLetterAndPreservesRemainingText() {
        assertEquals(
            "Detergente neutro",
            TextInputRules.capitalizeFirstLetter("detergente neutro"),
        )
    }

    @Test
    fun capitalizesFirstLetterAfterLeadingSpaces() {
        assertEquals(
            "  Mercado Central",
            TextInputRules.capitalizeFirstLetter("  mercado Central"),
        )
    }

    @Test
    fun keepsNumbersAndCodesUntouchedWhenThereIsNoLetter() {
        assertEquals(
            "12345-67",
            TextInputRules.capitalizeFirstLetter("12345-67"),
        )
    }

    @Test
    fun supportsAccentedLetters() {
        assertEquals(
            "Água mineral",
            TextInputRules.capitalizeFirstLetter("água mineral"),
        )
    }
}
