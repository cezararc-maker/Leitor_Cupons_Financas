package br.com.leitorcuponsfinancas.domain

import br.com.leitorcuponsfinancas.data.ProductEntity

data class ProductDuplicateCandidate(
    val product: ProductEntity,
    val similarity: Int,
)

object ProductDuplicateDetector {

    fun findCandidates(
        input: String,
        products: List<ProductEntity>,
        limit: Int = 3,
    ): List<ProductDuplicateCandidate> {
        val key = ProductNormalizer.searchKey(input)
        if (key.isBlank()) return emptyList()

        val inputTokens = tokens(key)

        return products
            .mapNotNull { product ->
                val candidateKey = ProductNormalizer.searchKey(product.normalizedName)
                if (candidateKey.isBlank()) return@mapNotNull null

                val score = score(
                    inputKey = key,
                    inputTokens = inputTokens,
                    candidateKey = candidateKey,
                    candidateTokens = tokens(candidateKey),
                )

                score
                    .takeIf { it >= 55 }
                    ?.let { ProductDuplicateCandidate(product, it) }
            }
            .sortedWith(
                compareByDescending<ProductDuplicateCandidate> { it.similarity }
                    .thenBy { it.product.normalizedName.lowercase() },
            )
            .take(limit)
    }

    private fun score(
        inputKey: String,
        inputTokens: Set<String>,
        candidateKey: String,
        candidateTokens: Set<String>,
    ): Int {
        if (inputKey == candidateKey) return 100

        val intersection = inputTokens.intersect(candidateTokens).size
        val union = inputTokens.union(candidateTokens).size
        val jaccard = if (union == 0) 0.0 else intersection.toDouble() / union

        var score = (jaccard * 100.0).toInt()

        if (
            inputKey.contains(candidateKey) ||
            candidateKey.contains(inputKey)
        ) {
            score += 20
        }

        if (
            inputTokens.isNotEmpty() &&
            candidateTokens.isNotEmpty() &&
            (
                inputTokens.containsAll(candidateTokens) ||
                    candidateTokens.containsAll(inputTokens)
                )
        ) {
            score += 10
        }

        return score.coerceAtMost(99)
    }

    private fun tokens(value: String): Set<String> =
        value.split(' ')
            .map(String::trim)
            .filter { it.length >= 3 }
            .toSet()
}
