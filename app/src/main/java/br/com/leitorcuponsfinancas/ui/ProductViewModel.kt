package br.com.leitorcuponsfinancas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.leitorcuponsfinancas.data.AppDatabase
import br.com.leitorcuponsfinancas.data.ProductEntity
import br.com.leitorcuponsfinancas.data.ProductRepository
import br.com.leitorcuponsfinancas.domain.ProductNormalizer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProductRepository(
        AppDatabase.getInstance(application).productDao(),
    )

    val products = repository.products.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
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
    ) {
        val cleanName = ProductNormalizer.displayName(name)
        if (cleanName.isBlank() || sector.isBlank() || category.isBlank()) return

        val now = System.currentTimeMillis()
        val product = ProductEntity(
            id = existing?.id ?: 0,
            fiscalDescription = fiscalDescription.trim().ifBlank { null },
            normalizedName = cleanName,
            sector = ProductNormalizer.displayName(sector),
            category = ProductNormalizer.displayName(category),
            subcategory = ProductNormalizer.displayName(subcategory).ifBlank { null },
            unit = unit.trim().uppercase().ifBlank { "UN" },
            notes = notes.trim().ifBlank { null },
            active = existing?.active ?: true,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )

        viewModelScope.launch {
            repository.save(product)
        }
    }

    fun deactivate(product: ProductEntity) {
        viewModelScope.launch {
            repository.deactivate(product.id)
        }
    }
}
