package br.com.leitorcuponsfinancas.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductNormalizerTest {

    @Test
    fun displayName_trimsAndCollapsesWhitespace() {
        assertEquals(
            "Arroz Tipo 1 5kg",
            ProductNormalizer.displayName("  Arroz   Tipo 1  5kg  "),
        )
    }

    @Test
    fun searchKey_removesAccentsAndUppercases() {
        assertEquals(
            "ACUCAR CRISTAL",
            ProductNormalizer.searchKey("Açúcar Cristal"),
        )
    }
}
