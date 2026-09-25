package br.com.leitorcuponsfinancas.domain

import br.com.leitorcuponsfinancas.data.TaxonomyLevel
import br.com.leitorcuponsfinancas.data.TaxonomyNodeEntity
import br.com.leitorcuponsfinancas.data.TaxonomyProductLinkEntity

object TaxonomySuggestionResolver {

    fun resolveNodeId(
        productId: Long,
        productLinks: List<TaxonomyProductLinkEntity>,
        nodes: List<TaxonomyNodeEntity>,
        merchantSegmentNodeId: Long?,
    ): Long? {
        val byId = nodes.associateBy { it.id }
        val linkedNodes = productLinks
            .asSequence()
            .filter { it.productId == productId }
            .mapNotNull { byId[it.taxonomyNodeId] }
            .filter { it.active }
            .toList()

        if (linkedNodes.isEmpty()) return null

        val compatible = if (merchantSegmentNodeId != null) {
            linkedNodes.filter {
                segmentIdFor(it, byId) == merchantSegmentNodeId
            }
        } else {
            val segmentIds = linkedNodes
                .mapNotNull { segmentIdFor(it, byId) }
                .distinct()

            if (segmentIds.size > 1) return null
            linkedNodes
        }

        return compatible
            .maxWithOrNull(
                compareBy<TaxonomyNodeEntity> { levelRank(it.level) }
                    .thenBy { ancestryDepth(it, byId) },
            )
            ?.id
    }

    private fun segmentIdFor(
        node: TaxonomyNodeEntity,
        byId: Map<Long, TaxonomyNodeEntity>,
    ): Long? {
        var current: TaxonomyNodeEntity? = node
        while (current != null) {
            if (current.level == TaxonomyLevel.SEGMENT.code) return current.id
            current = current.parentId?.let(byId::get)
        }
        return null
    }

    private fun ancestryDepth(
        node: TaxonomyNodeEntity,
        byId: Map<Long, TaxonomyNodeEntity>,
    ): Int {
        var depth = 0
        var current: TaxonomyNodeEntity? = node
        while (current != null) {
            depth += 1
            current = current.parentId?.let(byId::get)
        }
        return depth
    }

    private fun levelRank(level: String): Int = when (level) {
        TaxonomyLevel.SUBCATEGORY.code -> 4
        TaxonomyLevel.CATEGORY.code -> 3
        TaxonomyLevel.DEPARTMENT.code -> 2
        TaxonomyLevel.SEGMENT.code -> 1
        else -> 0
    }
}
