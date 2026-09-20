package br.com.leitorcuponsfinancas.ui

import br.com.leitorcuponsfinancas.data.HistoryItemRow
import br.com.leitorcuponsfinancas.domain.ProductNormalizer

internal object HistorySearchFilter {

    fun filter(
        items: List<HistoryItemRow>,
        query: String,
        mode: HistorySearchMode,
    ): List<HistoryItemRow> {
        val normalizedQuery = ProductNormalizer.searchKey(query)
        if (normalizedQuery.isBlank()) return items

        return items.filter { item ->
            val candidates = listOfNotNull(
                item.fiscalDescription,
                item.productName,
            ).map(ProductNormalizer::searchKey)

            when (mode) {
                HistorySearchMode.STARTS_WITH ->
                    candidates.any { candidate -> candidate.startsWith(normalizedQuery) }

                HistorySearchMode.CONTAINS ->
                    candidates.any { candidate -> candidate.contains(normalizedQuery) }
            }
        }
    }
}
