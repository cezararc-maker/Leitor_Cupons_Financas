package br.com.leitorcuponsfinancas.ui

import br.com.leitorcuponsfinancas.data.HistoryItemRow
import org.junit.Assert.assertEquals
import org.junit.Test

class HistorySearchFilterTest {

    @Test
    fun containsModeMatchesIntermediateLettersInRealTime() {
        val items = listOf(
            row(1, "ARROZ"),
            row(2, "FEIJÃO"),
            row(3, "TRIGO"),
        )

        assertEquals(
            listOf("FEIJÃO", "TRIGO"),
            HistorySearchFilter.filter(
                items = items,
                query = "i",
                mode = HistorySearchMode.CONTAINS,
            ).map { it.fiscalDescription },
        )

        assertEquals(
            listOf("TRIGO"),
            HistorySearchFilter.filter(
                items = items,
                query = "ig",
                mode = HistorySearchMode.CONTAINS,
            ).map { it.fiscalDescription },
        )
    }

    @Test
    fun containsModeMatchesSharedSuffix() {
        val items = listOf(
            row(1, "JOAOZINHO"),
            row(2, "ZEZINHO"),
            row(3, "MARIA"),
        )

        assertEquals(
            listOf("JOAOZINHO", "ZEZINHO"),
            HistorySearchFilter.filter(
                items = items,
                query = "zinho",
                mode = HistorySearchMode.CONTAINS,
            ).map { it.fiscalDescription },
        )
    }

    @Test
    fun startsWithModeOnlyMatchesBeginning() {
        val items = listOf(
            row(1, "ARROZ BRANCO"),
            row(2, "FARINHA DE ARROZ"),
        )

        assertEquals(
            listOf("ARROZ BRANCO"),
            HistorySearchFilter.filter(
                items = items,
                query = "arroz",
                mode = HistorySearchMode.STARTS_WITH,
            ).map { it.fiscalDescription },
        )
    }

    @Test
    fun searchIgnoresCaseAndAccentsAndAlsoUsesLinkedProductName() {
        val items = listOf(
            row(1, "FEIJAO CARIOCA", productName = "Feijão"),
            row(2, "ITEM 123", productName = "Pão de queijo"),
        )

        assertEquals(
            listOf("FEIJAO CARIOCA"),
            HistorySearchFilter.filter(
                items = items,
                query = "feijão",
                mode = HistorySearchMode.CONTAINS,
            ).map { it.fiscalDescription },
        )

        assertEquals(
            listOf("ITEM 123"),
            HistorySearchFilter.filter(
                items = items,
                query = "pao",
                mode = HistorySearchMode.STARTS_WITH,
            ).map { it.fiscalDescription },
        )
    }

    private fun row(
        id: Long,
        description: String,
        productName: String? = null,
    ) = HistoryItemRow(
        itemId = id,
        receiptId = 1,
        issuedDate = "2026-09-20",
        issuedAt = "20/09/2026 10:00:00",
        merchantName = "Mercado Teste",
        merchantCnpj = "12345678000190",
        receiptNumber = "1",
        receiptSeries = "1",
        fiscalDescription = description,
        itemCode = id.toString(),
        quantity = "1",
        unit = "UN",
        unitPrice = "1.00",
        totalAmount = "1.00",
        productId = productName?.let { id },
        productName = productName,
        sector = null,
        category = null,
        subcategory = null,
        correctedDescription = null,
        correctedQuantity = null,
        correctedUnit = null,
        correctedUnitPrice = null,
        correctedTotalAmount = null,
        correctedAt = null,
    )
}
