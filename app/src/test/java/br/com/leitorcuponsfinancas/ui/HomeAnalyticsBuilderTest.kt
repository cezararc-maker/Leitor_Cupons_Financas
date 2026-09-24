package br.com.leitorcuponsfinancas.ui

import br.com.leitorcuponsfinancas.data.HistoryItemRow
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeAnalyticsBuilderTest {

    @Test
    fun parsesBrazilianMoneyFormats() {
        assertEquals(1234.56, HomeAnalyticsBuilder.parseNumber("R$ 1.234,56")!!, 0.001)
        assertEquals(39.90, HomeAnalyticsBuilder.parseNumber("39,90")!!, 0.001)
        assertEquals(12.5, HomeAnalyticsBuilder.parseNumber("12.50")!!, 0.001)
    }

    @Test
    fun buildsMerchantAndProductRankingsWithoutCountingItemsAsPurchases() {
        val rows = listOf(
            row(
                itemId = 1,
                receiptId = 10,
                merchant = "Mercado Central",
                product = "Arroz",
                quantity = "2",
                unitPrice = "10,00",
                total = "20,00",
            ),
            row(
                itemId = 2,
                receiptId = 10,
                merchant = "Mercado Central",
                product = "Feijão",
                quantity = "1",
                unitPrice = "8,00",
                total = "8,00",
            ),
            row(
                itemId = 3,
                receiptId = 11,
                merchant = "Farmácia Boa",
                product = "Shampoo",
                quantity = "1",
                unitPrice = "35,00",
                total = "35,00",
            ),
            row(
                itemId = 4,
                receiptId = 12,
                merchant = "Mercado Central",
                product = "Arroz",
                quantity = "3",
                unitPrice = "11,00",
                total = "33,00",
            ),
        )

        val analytics = HomeAnalyticsBuilder.build(rows)

        assertEquals(96.0, analytics.totalSpent, 0.001)
        assertEquals(3, analytics.purchaseCount)
        assertEquals("Mercado Central", analytics.merchantsBySpend.first().name)
        assertEquals(2, analytics.merchantsByFrequency.first().purchaseCount)
        assertEquals("Arroz", analytics.productsMostPurchased.first().name)
        assertEquals(5.0, analytics.productsMostPurchased.first().quantity, 0.001)
        assertEquals("Shampoo", analytics.productsMostExpensive.first().name)
        assertEquals(35.0, analytics.productsMostExpensive.first().highestUnitPrice, 0.001)
    }

    private fun row(
        itemId: Long,
        receiptId: Long,
        merchant: String,
        product: String,
        quantity: String,
        unitPrice: String,
        total: String,
    ) = HistoryItemRow(
        itemId = itemId,
        receiptId = receiptId,
        issuedDate = "2026-09-23",
        issuedAt = null,
        merchantName = merchant,
        merchantCnpj = null,
        receiptNumber = null,
        receiptSeries = null,
        sourceType = "MANUAL",
        createdByName = null,
        fiscalDescription = product,
        itemCode = null,
        quantity = quantity,
        unit = "UN",
        unitPrice = unitPrice,
        totalAmount = total,
        productId = itemId,
        productName = product,
        sector = "Teste",
        category = "Teste",
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
