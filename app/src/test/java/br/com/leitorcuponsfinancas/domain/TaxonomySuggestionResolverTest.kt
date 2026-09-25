package br.com.leitorcuponsfinancas.domain

import br.com.leitorcuponsfinancas.data.TaxonomyLevel
import br.com.leitorcuponsfinancas.data.TaxonomyNodeEntity
import br.com.leitorcuponsfinancas.data.TaxonomyProductLinkEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaxonomySuggestionResolverTest {

    @Test
    fun choosesDeepestClassificationInsideMerchantSegment() {
        val nodes = nodes()
        val links = listOf(
            TaxonomyProductLinkEntity(
                id = 1,
                taxonomyNodeId = 12,
                productId = 100,
            ),
            TaxonomyProductLinkEntity(
                id = 2,
                taxonomyNodeId = 13,
                productId = 100,
            ),
            TaxonomyProductLinkEntity(
                id = 3,
                taxonomyNodeId = 22,
                productId = 100,
            ),
        )

        val result = TaxonomySuggestionResolver.resolveNodeId(
            productId = 100,
            productLinks = links,
            nodes = nodes,
            merchantSegmentNodeId = 10,
        )

        assertEquals(13L, result)
    }

    @Test
    fun respectsLockedMerchantSegmentWhenProductExistsInMoreThanOneSegment() {
        val nodes = nodes()
        val links = listOf(
            TaxonomyProductLinkEntity(
                id = 1,
                taxonomyNodeId = 13,
                productId = 100,
            ),
            TaxonomyProductLinkEntity(
                id = 2,
                taxonomyNodeId = 22,
                productId = 100,
            ),
        )

        val result = TaxonomySuggestionResolver.resolveNodeId(
            productId = 100,
            productLinks = links,
            nodes = nodes,
            merchantSegmentNodeId = 20,
        )

        assertEquals(22L, result)
    }

    @Test
    fun avoidsGuessingSegmentWhenProductBelongsToMultipleSegments() {
        val nodes = nodes()
        val links = listOf(
            TaxonomyProductLinkEntity(
                id = 1,
                taxonomyNodeId = 13,
                productId = 100,
            ),
            TaxonomyProductLinkEntity(
                id = 2,
                taxonomyNodeId = 22,
                productId = 100,
            ),
        )

        val result = TaxonomySuggestionResolver.resolveNodeId(
            productId = 100,
            productLinks = links,
            nodes = nodes,
            merchantSegmentNodeId = null,
        )

        assertNull(result)
    }

    @Test
    fun resolvesSingleSegmentWithoutMerchantClassification() {
        val nodes = nodes()
        val links = listOf(
            TaxonomyProductLinkEntity(
                id = 1,
                taxonomyNodeId = 12,
                productId = 100,
            ),
            TaxonomyProductLinkEntity(
                id = 2,
                taxonomyNodeId = 13,
                productId = 100,
            ),
        )

        val result = TaxonomySuggestionResolver.resolveNodeId(
            productId = 100,
            productLinks = links,
            nodes = nodes,
            merchantSegmentNodeId = null,
        )

        assertEquals(13L, result)
    }

    private fun nodes(): List<TaxonomyNodeEntity> = listOf(
        node(10, "segment.market", null, TaxonomyLevel.SEGMENT, "Mercado"),
        node(11, "market.cleaning", 10, TaxonomyLevel.DEPARTMENT, "Limpeza"),
        node(12, "market.cleaning.kitchen", 11, TaxonomyLevel.CATEGORY, "Cozinha"),
        node(13, "market.cleaning.kitchen.detergent", 12, TaxonomyLevel.SUBCATEGORY, "Detergentes"),
        node(20, "segment.other", null, TaxonomyLevel.SEGMENT, "Outros"),
        node(21, "other.cleaning", 20, TaxonomyLevel.DEPARTMENT, "Limpeza"),
        node(22, "other.cleaning.detergent", 21, TaxonomyLevel.CATEGORY, "Detergentes"),
    )

    private fun node(
        id: Long,
        stableKey: String,
        parentId: Long?,
        level: TaxonomyLevel,
        name: String,
    ) = TaxonomyNodeEntity(
        id = id,
        stableKey = stableKey,
        parentId = parentId,
        level = level.code,
        name = name,
        searchKey = name.lowercase(),
        active = true,
    )
}
