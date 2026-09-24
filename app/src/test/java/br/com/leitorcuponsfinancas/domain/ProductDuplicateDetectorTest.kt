package br.com.leitorcuponsfinancas.domain

import br.com.leitorcuponsfinancas.data.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductDuplicateDetectorTest {

    @Test
    fun flagsBrandVariationWhenRootProductAlreadyExists() {
        val products = listOf(
            ProductEntity(
                id = 1,
                normalizedName = "Macarrão",
                sector = "Alimentação",
                category = "Mercado",
            ),
        )

        val candidates = ProductDuplicateDetector.findCandidates(
            input = "Macarrão Renata",
            products = products,
        )

        assertTrue(candidates.isNotEmpty())
        assertEquals(1L, candidates.first().product.id)
        assertTrue(candidates.first().similarity >= 55)
    }

    @Test
    fun exactDuplicateGetsHighestScore() {
        val products = listOf(
            ProductEntity(
                id = 2,
                normalizedName = "Arroz",
                sector = "Alimentação",
                category = "Mercado",
            ),
        )

        val candidate = ProductDuplicateDetector.findCandidates(
            input = "arroz",
            products = products,
        ).single()

        assertEquals(100, candidate.similarity)
    }

    @Test
    fun unrelatedProductsAreNotSuggested() {
        val products = listOf(
            ProductEntity(
                id = 3,
                normalizedName = "Sabonete",
                sector = "Higiene",
                category = "Cuidados pessoais",
            ),
        )

        val candidates = ProductDuplicateDetector.findCandidates(
            input = "Macarrão",
            products = products,
        )

        assertTrue(candidates.isEmpty())
    }
}
