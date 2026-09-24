package br.com.leitorcuponsfinancas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.leitorcuponsfinancas.data.AppDatabase
import br.com.leitorcuponsfinancas.data.MerchantProductLinkEntity
import br.com.leitorcuponsfinancas.data.ProductEntity
import br.com.leitorcuponsfinancas.data.ProductRepository
import br.com.leitorcuponsfinancas.data.TaxonomyLevel
import br.com.leitorcuponsfinancas.data.TaxonomyNodeEntity
import br.com.leitorcuponsfinancas.data.TaxonomyProductLinkEntity
import br.com.leitorcuponsfinancas.data.TaxonomyRepository
import br.com.leitorcuponsfinancas.domain.ProductNormalizer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = ProductRepository(
        database.productDao(),
    )
    private val taxonomyRepository = TaxonomyRepository(
        taxonomyDao = database.taxonomyDao(),
        productDao = database.productDao(),
    )

    val products = repository.products.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val taxonomyNodes = taxonomyRepository.nodes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList<TaxonomyNodeEntity>(),
    )

    val taxonomyProductLinks = taxonomyRepository.productLinks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList<TaxonomyProductLinkEntity>(),
    )

    init {
        viewModelScope.launch {
            taxonomyRepository.ensureBaseTaxonomy()
        }
    }

    val learnedLinks = database.merchantProductLinkDao()
        .observeAll()
        .map { links ->
            links.groupBy { it.productId }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap<Long, List<MerchantProductLinkEntity>>(),
        )

    fun save(
        existing: ProductEntity?,
        name: String,
        fiscalDescription: String,
        sector: String,
        category: String,
        subcategory: String,
        unit: String,
        notes: String,
    ): String? {
        val cleanName = ProductNormalizer.displayName(name)
        if (cleanName.isBlank() || sector.isBlank() || category.isBlank()) {
            return "Informe nome, setor e categoria."
        }

        val duplicate = products.value.firstOrNull { candidate ->
            candidate.id != (existing?.id ?: 0L) &&
                ProductNormalizer.searchKey(candidate.normalizedName) ==
                    ProductNormalizer.searchKey(cleanName)
        }

        if (duplicate != null) {
            return "Já existe um produto mestre chamado \"${duplicate.normalizedName}\"."
        }

        val now = System.currentTimeMillis()
        val canonicalSector = canonicalClassification(
            typed = sector,
            existingValues = products.value.map { it.sector },
        )
        val canonicalCategory = canonicalClassification(
            typed = category,
            existingValues = products.value.map { it.category },
        )
        val canonicalSubcategory = canonicalClassification(
            typed = subcategory,
            existingValues = products.value.mapNotNull { it.subcategory },
        ).ifBlank { null }

        val product = ProductEntity(
            id = existing?.id ?: 0,
            fiscalDescription = fiscalDescription.trim().ifBlank { null },
            normalizedName = cleanName,
            sector = canonicalSector,
            category = canonicalCategory,
            subcategory = canonicalSubcategory,
            unit = unit.trim().uppercase().ifBlank { "UN" },
            notes = notes.trim().ifBlank { null },
            active = existing?.active ?: true,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )

        viewModelScope.launch {
            repository.save(product)
        }

        return null
    }

    fun saveTaxonomy(
        existing: ProductEntity?,
        name: String,
        fiscalDescription: String,
        taxonomyNodeId: Long,
        unit: String,
        notes: String,
    ): String? {
        val cleanName = ProductNormalizer.displayName(name)
        if (cleanName.isBlank()) return "Informe o nome do Produto Mestre."

        val duplicate = products.value.firstOrNull { candidate ->
            candidate.id != (existing?.id ?: 0L) &&
                ProductNormalizer.searchKey(candidate.normalizedName) ==
                    ProductNormalizer.searchKey(cleanName)
        }
        if (duplicate != null) {
            return "Já existe um Produto Mestre chamado \"${duplicate.normalizedName}\"."
        }

        val byId = taxonomyNodes.value.associateBy { it.id }
        val ancestry = mutableListOf<TaxonomyNodeEntity>()
        var current = byId[taxonomyNodeId]
        while (current != null) {
            ancestry += current
            current = current.parentId?.let(byId::get)
        }
        val path = ancestry.reversed()

        if (path.none { it.level == TaxonomyLevel.SEGMENT.code }) {
            return "Selecione uma classificação válida da taxonomia."
        }

        val department = path
            .firstOrNull { it.level == TaxonomyLevel.DEPARTMENT.code }
            ?.name
            ?: return "Selecione pelo menos um Departamento."

        val category = path
            .firstOrNull { it.level == TaxonomyLevel.CATEGORY.code }
            ?.name
            ?: path.lastOrNull()?.name
            ?: return "Selecione uma Categoria."

        val subcategory = path
            .firstOrNull { it.level == TaxonomyLevel.SUBCATEGORY.code }
            ?.name

        val now = System.currentTimeMillis()
        val product = ProductEntity(
            id = existing?.id ?: 0,
            fiscalDescription = fiscalDescription.trim().ifBlank { null },
            normalizedName = cleanName,
            sector = department,
            category = category,
            subcategory = subcategory,
            unit = unit.trim().uppercase().ifBlank { "UN" },
            notes = notes.trim().ifBlank { null },
            active = existing?.active ?: true,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )

        viewModelScope.launch {
            val id = repository.save(product)
            taxonomyRepository.linkProduct(
                taxonomyNodeId = taxonomyNodeId,
                productId = id,
            )
        }

        return null
    }

    fun deactivate(product: ProductEntity) {
        viewModelScope.launch {
            repository.deactivate(product.id)
        }
    }

    private fun canonicalClassification(
        typed: String,
        existingValues: List<String>,
    ): String {
        val clean = ProductNormalizer.displayName(typed)
        if (clean.isBlank()) return ""

        val key = ProductNormalizer.searchKey(clean)
        return existingValues.firstOrNull {
            ProductNormalizer.searchKey(it) == key
        } ?: clean
    }
}
