package br.com.leitorcuponsfinancas.ui

import br.com.leitorcuponsfinancas.data.HistoryItemRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryAnalyticsBuilderTest {

    @Test
    fun filtersByProductMerchantSourceAndValue() {
        val rows = listOf(
            row(1, 10, 1, "Macarrão", "Mercado A", "NFCE", "10.00"),
            row(2, 11, 2, "Arroz", "Mercado B", "MANUAL", "20.00"),
        )

        val filtered = HistoryAnalyticsBuilder.filter(
            rows,
            HistoryAdvancedFilter(
                productId = 1,
                merchant = "Mercado A",
                sourceType = "NFCE",
                minimumAmount = 5.0,
                maximumAmount = 15.0,
            ),
        )

        assertEquals(1, filtered.size)
        assertEquals("Macarrão", filtered.first().productName)
    }

    @Test
    fun buildsFinancialSummary() {
        val rows = listOf(
            row(1, 10, 1, "Macarrão", "Mercado A", "NFCE", "10.00"),
            row(2, 10, 2, "Arroz", "Mercado A", "NFCE", "20.00"),
            row(3, 11, 1, "Macarrão", "Mercado B", "NFCE", "15.00"),
        )

        val analytics = HistoryAnalyticsBuilder.build(rows)

        assertEquals(45.0, analytics.totalSpent, 0.001)
        assertEquals(3, analytics.itemCount)
        assertEquals(2, analytics.purchaseCount)
        assertEquals(22.5, analytics.averagePurchase, 0.001)
        assertEquals("Macarrão", analytics.topProduct)
        assertTrue(analytics.topMerchant != null)
    }

    private fun row(
        itemId: Long,
        receiptId: Long,
        productId: Long,
        product: String,
        merchant: String,
        source: String,
        total: String,
    ) = HistoryItemRow(
        itemId = itemId,
        receiptId = receiptId,
        issuedDate = "2026-09-24",
        issuedAt = null,
        merchantName = merchant,
        merchantCnpj = null,
        receiptNumber = null,
        receiptSeries = null,
        sourceType = source,
        createdByName = null,
        fiscalDescription = product,
        itemCode = null,
        quantity = "1",
        unit = "UN",
        unitPrice = total,
        totalAmount = total,
        productId = productId,
        productName = product,
        sector = "Alimentação",
        category = "Mercado",
        subcategory = null,
        correctedDescription = null,
        correctedQuantity = null,
        correctedUnit = null,
        correctedUnitPrice = null,
        correctedTotalAmount = null,
        correctedByName = null,
        correctedAt = null,
    )
}
