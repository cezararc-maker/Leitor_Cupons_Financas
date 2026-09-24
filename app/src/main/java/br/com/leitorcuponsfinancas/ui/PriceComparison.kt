package br.com.leitorcuponsfinancas.ui

import br.com.leitorcuponsfinancas.data.HistoryItemRow
import java.time.LocalDate

data class MerchantPriceStats(
    val merchantName: String,
    val averageUnitPrice: Double,
    val lowestUnitPrice: Double,
    val highestUnitPrice: Double,
    val observations: Int,
    val lastDate: String?,
)

data class ProductPriceComparison(
    val productId: Long,
    val productName: String,
    val averageUnitPrice: Double,
    val lowestUnitPrice: Double,
    val highestUnitPrice: Double,
    val observations: Int,
    val merchants: List<MerchantPriceStats>,
    val recentPrices: List<PriceObservation>,
)

data class PriceObservation(
    val merchantName: String,
    val issuedDate: String?,
    val unitPrice: Double,
    val quantity: Double?,
    val totalAmount: Double?,
)

object PriceComparisonBuilder {

    fun build(
        rows: List<HistoryItemRow>,
        productId: Long,
    ): ProductPriceComparison? {
        val productRows = rows
            .filter { it.productId == productId }
            .mapNotNull { row ->
                val unitPrice = HomeAnalyticsBuilder.parseNumber(row.displayUnitPrice)
                    ?: return@mapNotNull null
                row to unitPrice
            }

        if (productRows.isEmpty()) return null

        val productName = productRows.first().first.productName ?: "Produto"

        val observations = productRows.map { (row, unitPrice) ->
            PriceObservation(
                merchantName = row.displayMerchantName ?: "Estabelecimento não identificado",
                issuedDate = row.issuedDate,
                unitPrice = unitPrice,
                quantity = HomeAnalyticsBuilder.parseNumber(row.displayQuantity),
                totalAmount = HomeAnalyticsBuilder.parseNumber(row.displayTotalAmount),
            )
        }

        val merchantStats = observations
            .groupBy { it.merchantName.trim().lowercase() }
            .map { (_, merchantRows) ->
                MerchantPriceStats(
                    merchantName = merchantRows.first().merchantName,
                    averageUnitPrice = merchantRows.map { it.unitPrice }.average(),
                    lowestUnitPrice = merchantRows.minOf { it.unitPrice },
                    highestUnitPrice = merchantRows.maxOf { it.unitPrice },
                    observations = merchantRows.size,
                    lastDate = merchantRows
                        .mapNotNull { it.issuedDate }
                        .maxOrNull(),
                )
            }
            .sortedWith(
                compareBy<MerchantPriceStats> { it.averageUnitPrice }
                    .thenByDescending { it.observations },
            )

        val prices = observations.map { it.unitPrice }

        return ProductPriceComparison(
            productId = productId,
            productName = productName,
            averageUnitPrice = prices.average(),
            lowestUnitPrice = prices.minOrNull() ?: 0.0,
            highestUnitPrice = prices.maxOrNull() ?: 0.0,
            observations = observations.size,
            merchants = merchantStats,
            recentPrices = observations
                .sortedByDescending { it.issuedDate.orEmpty() }
                .take(20),
        )
    }
}
