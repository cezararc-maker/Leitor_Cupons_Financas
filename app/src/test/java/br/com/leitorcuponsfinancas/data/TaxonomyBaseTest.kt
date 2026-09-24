package br.com.leitorcuponsfinancas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxonomyBaseTest {

    @Test
    fun marketContainsHortifrutiAndPharmacyDoesNot() {
        val base = TaxonomyRepository.BASE_TAXONOMY
        val hortifruti = base.firstOrNull {
            it.stableKey == "market.food.produce"
        }

        assertNotNull(hortifruti)
        assertEquals("market.food", hortifruti?.parentStableKey)
        assertTrue(
            base.any {
                it.stableKey == "market.food.produce.fruit" &&
                    it.parentStableKey == "market.food.produce"
            },
        )
        assertFalse(
            base.any {
                it.parentStableKey?.startsWith("pharmacy") == true &&
                    it.name.equals("Hortifruti", ignoreCase = true)
            },
        )
    }

    @Test
    fun pharmacyContainsFirstAidAndMarketDoesNot() {
        val base = TaxonomyRepository.BASE_TAXONOMY

        assertTrue(
            base.any {
                it.stableKey == "pharmacy.health.first_aid" &&
                    it.name == "Curativos e primeiros socorros"
            },
        )
        assertFalse(
            base.any {
                it.parentStableKey?.startsWith("market") == true &&
                    it.name.contains("Curativos", ignoreCase = true)
            },
        )
    }

    @Test
    fun hierarchyMovesFromBroadToSpecific() {
        assertEquals(TaxonomyLevel.DEPARTMENT, TaxonomyLevel.SEGMENT.nextOrNull())
        assertEquals(TaxonomyLevel.CATEGORY, TaxonomyLevel.DEPARTMENT.nextOrNull())
        assertEquals(TaxonomyLevel.SUBCATEGORY, TaxonomyLevel.CATEGORY.nextOrNull())
        assertEquals(null, TaxonomyLevel.SUBCATEGORY.nextOrNull())
    }
}
