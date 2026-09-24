package br.com.leitorcuponsfinancas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.leitorcuponsfinancas.data.AppDatabase
import br.com.leitorcuponsfinancas.data.HistoryItemRow
import br.com.leitorcuponsfinancas.data.ProductEntity
import br.com.leitorcuponsfinancas.data.ProductLinkResult
import br.com.leitorcuponsfinancas.data.ProductRepository
import br.com.leitorcuponsfinancas.data.ReceiptRepository
import br.com.leitorcuponsfinancas.domain.ProductNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReviewActionState(
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val productRepository = ProductRepository(database.productDao())
    private val receiptRepository = ReceiptRepository(
        receiptDao = database.receiptDao(),
        productDao = database.productDao(),
        linkDao = database.merchantProductLinkDao(),
        merchantDao = database.merchantDao(),
    )

    val items: StateFlow<List<HistoryItemRow>> =
        database.receiptDao()
            .observeItemsNeedingReview()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    val products: StateFlow<List<ProductEntity>> =
        productRepository.products
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    val learnedLinks =
        database.merchantProductLinkDao()
            .observeAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    private val _actionState = MutableStateFlow(ReviewActionState())
    val actionState: StateFlow<ReviewActionState> = _actionState.asStateFlow()

    fun clearMessage() {
        _actionState.value = ReviewActionState()
    }

    fun link(
        item: HistoryItemRow,
        product: ProductEntity,
    ) {
        if (_actionState.value.saving) return
        _actionState.value = ReviewActionState(saving = true)

        viewModelScope.launch {
            when (
                val result = receiptRepository.linkHistoryItem(
                    item = item,
                    productId = product.id,
                )
            ) {
                is ProductLinkResult.Success -> {
                    _actionState.value = ReviewActionState(
                        message = "\"${item.displayDescription}\" foi vinculado a \"${product.normalizedName}\". ${result.updatedItems} item(ns) atualizado(s).",
                    )
                }

                is ProductLinkResult.Error -> {
                    _actionState.value = ReviewActionState(error = result.message)
                }
            }
        }
    }

    fun createAndLink(
        item: HistoryItemRow,
        name: String,
        sector: String,
        category: String,
        subcategory: String,
        unit: String,
    ) {
        if (_actionState.value.saving) return

        val cleanName = ProductNormalizer.displayName(name)
        val cleanSector = ProductNormalizer.displayName(sector)
        val cleanCategory = ProductNormalizer.displayName(category)

        if (cleanName.isBlank() || cleanSector.isBlank() || cleanCategory.isBlank()) {
            _actionState.value = ReviewActionState(
                error = "Informe Produto Mestre, setor e categoria.",
            )
            return
        }

        _actionState.value = ReviewActionState(saving = true)

        viewModelScope.launch {
            val duplicate = productRepository.findDuplicateName(cleanName)
            val product = if (duplicate != null) {
                duplicate
            } else {
                val now = System.currentTimeMillis()
                val candidate = ProductEntity(
                    fiscalDescription = item.fiscalDescription.takeIf {
                        item.sourceType != "MANUAL"
                    },
                    normalizedName = cleanName,
                    sector = cleanSector,
                    category = cleanCategory,
                    subcategory = ProductNormalizer.displayName(subcategory).ifBlank { null },
                    unit = unit.trim().uppercase().ifBlank { item.displayUnit ?: "UN" },
                    createdAt = now,
                    updatedAt = now,
                )

                try {
                    val id = productRepository.save(candidate)
                    candidate.copy(id = id)
                } catch (error: Exception) {
                    _actionState.value = ReviewActionState(
                        error = error.message ?: "Não foi possível criar o Produto Mestre.",
                    )
                    return@launch
                }
            }

            when (
                val result = receiptRepository.linkHistoryItem(
                    item = item,
                    productId = product.id,
                )
            ) {
                is ProductLinkResult.Success -> {
                    _actionState.value = ReviewActionState(
                        message = if (duplicate != null) {
                            "O Produto Mestre \"${product.normalizedName}\" já existia e foi reutilizado."
                        } else {
                            "Produto Mestre \"${product.normalizedName}\" criado e vinculado."
                        },
                    )
                }

                is ProductLinkResult.Error -> {
                    _actionState.value = ReviewActionState(error = result.message)
                }
            }
        }
    }
}
