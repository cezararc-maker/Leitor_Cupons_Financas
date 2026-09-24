package br.com.leitorcuponsfinancas.ui

import br.com.leitorcuponsfinancas.data.HistoryItemRow

data class HistoryAdvancedFilter(
    val productId: Long? = null,
    val category: String? = null,
    val merchant: String? = null,
    val sourceType: String? = null,
    val minimumAmount: Double? = null,
    val maximumAmount: Double? = null,
    val onlyUnlinked: Boolean = false,
) {
    val activeCount: Int
        get() = listOf(
            productId != null,
            !category.isNullOrBlank(),
            !merchant.isNullOrBlank(),
            !sourceType.isNullOrBlank(),
            minimumAmount != null,
            maximumAmount != null,
            onlyUnlinked,
        ).count { it }
}

data class HistoryAnalytics(
    val totalSpent: Double = 0.0,
    val itemCount: Int = 0,
    val purchaseCount: Int = 0,
    val averagePurchase: Double = 0.0,
    val topProduct: String? = null,
    val topCategory: String? = null,
    val topMerchant: String? = null,
)

object HistoryAnalyticsBuilder {

    fun filter(
        rows: List<HistoryItemRow>,
        filter: HistoryAdvancedFilter,
    ): List<HistoryItemRow> = rows.filter { row ->
        val amount = HomeAnalyticsBuilder.parseNumber(row.displayTotalAmount)

        (filter.productId == null || row.productId == filter.productId) &&
            (filter.category.isNullOrBlank() ||
                row.category.equals(filter.category, ignoreCase = true)) &&
            (filter.merchant.isNullOrBlank() ||
                row.displayMerchantName.equals(filter.merchant, ignoreCase = true)) &&
            (filter.sourceType.isNullOrBlank() ||
                row.sourceType.equals(filter.sourceType, ignoreCase = true)) &&
            (filter.minimumAmount == null ||
                (amount != null && amount >= filter.minimumAmount)) &&
            (filter.maximumAmount == null ||
                (amount != null && amount <= filter.maximumAmount)) &&
            (!filter.onlyUnlinked || row.productId == null)
    }

    fun build(rows: List<HistoryItemRow>): HistoryAnalytics {
        if (rows.isEmpty()) return HistoryAnalytics()

        val total = rows.sumOf { HomeAnalyticsBuilder.parseNumber(it.displayTotalAmount) ?: 0.0 }
        val receipts = rows.groupBy { it.receiptId }

        val products = rows
            .mapNotNull { row ->
                row.productName
                    ?.takeIf(String::isNotBlank)
                    ?.let { it to (HomeAnalyticsBuilder.parseNumber(row.displayTotalAmount) ?: 0.0) }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() }

        val categories = rows
            .mapNotNull { row ->
                row.category
                    ?.takeIf(String::isNotBlank)
                    ?.let { it to (HomeAnalyticsBuilder.parseNumber(row.displayTotalAmount) ?: 0.0) }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() }

        val merchants = rows
            .mapNotNull { row ->
                row.displayMerchantName
                    ?.takeIf(String::isNotBlank)
                    ?.let { it to (HomeAnalyticsBuilder.parseNumber(row.displayTotalAmount) ?: 0.0) }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() }

        return HistoryAnalytics(
            totalSpent = total,
            itemCount = rows.size,
            purchaseCount = receipts.size,
            averagePurchase = if (receipts.isEmpty()) 0.0 else total / receipts.size,
            topProduct = products.maxByOrNull { it.value }?.key,
            topCategory = categories.maxByOrNull { it.value }?.key,
            topMerchant = merchants.maxByOrNull { it.value }?.key,
        )
    }
}
