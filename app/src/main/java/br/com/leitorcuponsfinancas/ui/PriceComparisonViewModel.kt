package br.com.leitorcuponsfinancas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.leitorcuponsfinancas.data.AppDatabase
import br.com.leitorcuponsfinancas.data.ProductEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class PriceComparisonViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val today = LocalDate.now()
    private val start = today.minusMonths(12)

    val products: StateFlow<List<ProductEntity>> =
        database.productDao()
            .observeActive()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    private val rows = database.receiptDao()
        .observeHistory(
            startDate = start.toString(),
            endDate = today.toString(),
        )

    private val _selectedProductId = MutableStateFlow<Long?>(null)
    val selectedProductId: StateFlow<Long?> = _selectedProductId.asStateFlow()

    val comparison: StateFlow<ProductPriceComparison?> =
        combine(
            rows,
            _selectedProductId,
        ) { history, productId ->
            productId?.let { PriceComparisonBuilder.build(history, it) }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

    fun selectProduct(productId: Long?) {
        _selectedProductId.value = productId
    }
}
