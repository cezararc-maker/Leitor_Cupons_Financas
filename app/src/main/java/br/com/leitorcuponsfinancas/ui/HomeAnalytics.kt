package br.com.leitorcuponsfinancas.ui

import br.com.leitorcuponsfinancas.data.HistoryItemRow
import kotlin.math.abs

data class MerchantAnalytics(
    val name: String,
    val totalSpent: Double,
    val purchaseCount: Int,
)

data class ProductAnalytics(
    val name: String,
    val quantity: Double,
    val totalSpent: Double,
    val purchaseCount: Int,
    val highestUnitPrice: Double,
)

data class HomeAnalytics(
    val totalSpent: Double = 0.0,
    val purchaseCount: Int = 0,
    val merchantsBySpend: List<MerchantAnalytics> = emptyList(),
    val merchantsByFrequency: List<MerchantAnalytics> = emptyList(),
    val productsMostPurchased: List<ProductAnalytics> = emptyList(),
    val productsMostExpensive: List<ProductAnalytics> = emptyList(),
)

object HomeAnalyticsBuilder {

    fun build(rows: List<HistoryItemRow>): HomeAnalytics {
        if (rows.isEmpty()) return HomeAnalytics()

        val totalSpent = rows.sumOf { parseNumber(it.displayTotalAmount) ?: 0.0 }
        val purchaseCount = rows.map { it.receiptId }.distinct().size

        val merchants = rows
            .filter { !it.merchantName.isNullOrBlank() }
            .groupBy { it.merchantName!!.trim().lowercase() }
            .map { (_, merchantRows) ->
                MerchantAnalytics(
                    name = merchantRows.first().merchantName!!.trim(),
                    totalSpent = merchantRows.sumOf {
                        parseNumber(it.displayTotalAmount) ?: 0.0
                    },
                    purchaseCount = merchantRows.map { it.receiptId }.distinct().size,
                )
            }

        val productGroups = rows.groupBy { row ->
            (row.productName ?: row.displayDescription)
                .trim()
                .ifBlank { "Produto não identificado" }
                .lowercase()
        }

        val products = productGroups.map { (_, productRows) ->
            ProductAnalytics(
                name = (productRows.first().productName ?: productRows.first().displayDescription)
                    .trim()
                    .ifBlank { "Produto não identificado" },
                quantity = productRows.sumOf {
                    parseNumber(it.displayQuantity)?.takeIf { value -> value > 0 } ?: 1.0
                },
                totalSpent = productRows.sumOf {
                    parseNumber(it.displayTotalAmount) ?: 0.0
                },
                purchaseCount = productRows.map { it.receiptId }.distinct().size,
                highestUnitPrice = productRows.maxOfOrNull {
                    parseNumber(it.displayUnitPrice) ?: 0.0
                } ?: 0.0,
            )
        }

        return HomeAnalytics(
            totalSpent = totalSpent,
            purchaseCount = purchaseCount,
            merchantsBySpend = merchants
                .sortedWith(
                    compareByDescending<MerchantAnalytics> { it.totalSpent }
                        .thenByDescending { it.purchaseCount },
                )
                .take(5),
            merchantsByFrequency = merchants
                .sortedWith(
                    compareByDescending<MerchantAnalytics> { it.purchaseCount }
                        .thenByDescending { it.totalSpent },
                )
                .take(5),
            productsMostPurchased = products
                .sortedWith(
                    compareByDescending<ProductAnalytics> { it.quantity }
                        .thenByDescending { it.purchaseCount }
                        .thenByDescending { it.totalSpent },
                )
                .take(5),
            productsMostExpensive = products
                .sortedWith(
                    compareByDescending<ProductAnalytics> { it.highestUnitPrice }
                        .thenByDescending { it.totalSpent },
                )
                .take(5),
        )
    }

    internal fun parseNumber(raw: String?): Double? {
        val text = raw
            ?.trim()
            ?.replace("R$", "", ignoreCase = true)
            ?.replace(" ", "")
            ?.replace(Regex("[^0-9,.-]"), "")
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val lastComma = text.lastIndexOf(',')
        val lastDot = text.lastIndexOf('.')

        val normalized = when {
            lastComma >= 0 && lastDot >= 0 -> {
                if (lastComma > lastDot) {
                    text.replace(".", "").replace(',', '.')
                } else {
                    text.replace(",", "")
                }
            }
            lastComma >= 0 -> text.replace('.', ' ').replace(" ", "").replace(',', '.')
            text.count { it == '.' } > 1 -> {
                val decimalIndex = text.lastIndexOf('.')
                text.substring(0, decimalIndex).replace(".", "") + text.substring(decimalIndex)
            }
            else -> text
        }

        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() && abs(it) < 1_000_000_000 }
    }
}
