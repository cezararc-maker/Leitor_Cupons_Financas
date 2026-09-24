package br.com.leitorcuponsfinancas.ui

import br.com.leitorcuponsfinancas.data.HistoryItemRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PriceComparisonBuilderTest {

    @Test
    fun comparesSameMasterProductAcrossMerchants() {
        val rows = listOf(
            row(1, 10, "Mercado A", "5.50"),
            row(2, 11, "Mercado A", "6.00"),
            row(3, 12, "Mercado B", "4.90"),
        )

        val comparison = PriceComparisonBuilder.build(rows, productId = 100)

        assertNotNull(comparison)
        comparison!!
        assertEquals(4.90, comparison.lowestUnitPrice, 0.001)
        assertEquals(6.00, comparison.highestUnitPrice, 0.001)
        assertEquals("Mercado B", comparison.merchants.first().merchantName)
        assertEquals(3, comparison.observations)
    }

    private fun row(
        itemId: Long,
        receiptId: Long,
        merchant: String,
        unitPrice: String,
    ) = HistoryItemRow(
        itemId = itemId,
        receiptId = receiptId,
        issuedDate = "2026-09-${20 + itemId}",
        issuedAt = null,
        merchantName = merchant,
        merchantCnpj = null,
        receiptNumber = null,
        receiptSeries = null,
        sourceType = "NFCE",
        createdByName = null,
        fiscalDescription = "MACARRAO TESTE",
        itemCode = itemId.toString(),
        quantity = "1",
        unit = "UN",
        unitPrice = unitPrice,
        totalAmount = unitPrice,
        productId = 100,
        productName = "Macarrão",
        sector = "Alimentação",
        category = "Mercado",
        subcategory = "Massas",
        correctedDescription = null,
        correctedQuantity = null,
        correctedUnit = null,
        correctedUnitPrice = null,
        correctedTotalAmount = null,
        correctedByName = null,
        correctedAt = null,
    )
}
