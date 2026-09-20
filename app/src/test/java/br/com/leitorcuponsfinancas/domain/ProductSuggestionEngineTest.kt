package br.com.leitorcuponsfinancas.domain

import br.com.leitorcuponsfinancas.data.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductSuggestionEngineTest {

    @Test
    fun suggestsExistingProductFromDescription() {
        val arroz = ProductEntity(
            id = 1,
            normalizedName = "Arroz",
            sector = "Alimentação",
            category = "Mercado",
        )

        val result = ProductSuggestionEngine.suggest(
            description = "ARROZ TIO LAUTERIOT1",
            unit = "UN",
            products = listOf(arroz),
            learnedLinks = emptyList(),
        )

        assertTrue(result is SmartProductSuggestion.ExistingProduct)
        assertEquals(1L, (result as SmartProductSuggestion.ExistingProduct).product.id)
    }

    @Test
    fun suggestsNewPotatoMasterProduct() {
        val result = ProductSuggestionEngine.suggest(
            description = "BATATA LAVADA KG",
            unit = "KG",
            products = emptyList(),
            learnedLinks = emptyList(),
        )

        assertTrue(result is SmartProductSuggestion.NewProduct)
        val newProduct = result as SmartProductSuggestion.NewProduct
        assertEquals("Batata", newProduct.name)
        assertEquals("Hortifruti", newProduct.category)
        assertEquals("KG", newProduct.unit)
    }

    @Test
    fun usesExistingInstantNoodlesInsteadOfSuggestingDuplicate() {
        val existing = ProductEntity(
            id = 7,
            normalizedName = "Macarrão instantâneo",
            sector = "Alimentação",
            category = "Mercado",
            subcategory = "Massas",
        )

        val result = ProductSuggestionEngine.suggest(
            description = "MAC.NISSIN LAMEN",
            unit = "UN",
            products = listOf(existing),
            learnedLinks = emptyList(),
        )

        assertTrue(result is SmartProductSuggestion.ExistingProduct)
        assertEquals(
            7L,
            (result as SmartProductSuggestion.ExistingProduct).product.id,
        )
    }

    @Test
    fun suggestsInstantNoodlesForNissin() {
        val result = ProductSuggestionEngine.suggest(
            description = "NISSIN LAMEN GALINHA 85G",
            unit = "UN",
            products = emptyList(),
            learnedLinks = emptyList(),
        )

        assertTrue(result is SmartProductSuggestion.NewProduct)
        assertEquals(
            "Macarrão instantâneo",
            (result as SmartProductSuggestion.NewProduct).name,
        )
    }

    @Test
    fun suggestsChocolate() {
        val result = ProductSuggestionEngine.suggest(
            description = "CHOCOLATE AO LEITE 90G",
            unit = "UN",
            products = emptyList(),
            learnedLinks = emptyList(),
        )

        assertTrue(result is SmartProductSuggestion.NewProduct)
        val newProduct = result as SmartProductSuggestion.NewProduct
        assertEquals("Chocolate", newProduct.name)
        assertEquals("Doces", newProduct.subcategory)
    }
}
