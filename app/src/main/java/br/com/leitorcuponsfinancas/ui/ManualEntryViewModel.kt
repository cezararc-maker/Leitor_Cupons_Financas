package br.com.leitorcuponsfinancas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.leitorcuponsfinancas.data.AppDatabase
import br.com.leitorcuponsfinancas.data.MerchantProductLinkEntity
import br.com.leitorcuponsfinancas.data.MerchantSuggestion
import br.com.leitorcuponsfinancas.data.ProductEntity
import br.com.leitorcuponsfinancas.data.ProductRepository
import br.com.leitorcuponsfinancas.data.ReceiptRepository
import br.com.leitorcuponsfinancas.data.UserProfileStore
import br.com.leitorcuponsfinancas.domain.ProductNormalizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ManualEntrySaveState(
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class ManualEntryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val profileStore = UserProfileStore.getInstance(application)
    private val productRepository = ProductRepository(database.productDao())
    private val receiptRepository = ReceiptRepository(
        receiptDao = database.receiptDao(),
        productDao = database.productDao(),
        linkDao = database.merchantProductLinkDao(),
        merchantDao = database.merchantDao(),
    )

    val products: StateFlow<List<ProductEntity>> = productRepository.products.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val learnedLinks: StateFlow<List<MerchantProductLinkEntity>> =
        database.merchantProductLinkDao()
            .observeAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    val merchantSuggestions: StateFlow<List<MerchantSuggestion>> =
        database.receiptDao()
            .observeMerchantSuggestions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    private val _saveState = MutableStateFlow(ManualEntrySaveState())
    val saveState: StateFlow<ManualEntrySaveState> = _saveState.asStateFlow()

    fun save(
        merchantName: String,
        merchantCnpj: String,
        dateText: String,
        description: String,
        quantity: String,
        unit: String,
        unitPrice: String,
        productId: Long?,
    ) {
        if (_saveState.value.saving) return

        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val date = runCatching {
            LocalDate.parse(dateText.trim(), dateFormatter)
        }.getOrElse {
            _saveState.value = ManualEntrySaveState(
                error = "Data inválida. Use o formato DD/MM/AAAA.",
            )
            return
        }

        if (description.isBlank()) {
            _saveState.value = ManualEntrySaveState(error = "Informe a descrição do item.")
            return
        }
        if (productId == null) {
            _saveState.value = ManualEntrySaveState(
                error = "Vincule o lançamento a um produto mestre antes de salvar.",
            )
            return
        }
        val calculatedTotal = ManualEntryCalculator.calculateTotal(
            quantity = quantity,
            unitPrice = unitPrice,
        )
        if (calculatedTotal == null) {
            _saveState.value = ManualEntrySaveState(
                error = "Informe quantidade e valor válidos, maiores que zero.",
            )
            return
        }

        _saveState.value = ManualEntrySaveState(saving = true)

        viewModelScope.launch {
            try {
                val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                receiptRepository.saveManualPurchase(
                    merchantName = merchantName,
                    merchantCnpj = merchantCnpj,
                    issuedDate = "${date.format(dateFormatter)} $time",
                    description = description,
                    quantity = quantity,
                    unit = unit,
                    unitPrice = unitPrice,
                    productId = productId,
                    actor = profileStore.profile.value,
                )

                _saveState.value = ManualEntrySaveState(
                    message = "Lançamento manual salvo. Total: R$ ${
                        ManualEntryCalculator.formatMoney(calculatedTotal)
                    }.",
                )
            } catch (error: Exception) {
                _saveState.value = ManualEntrySaveState(
                    error = error.message ?: "Não foi possível salvar o lançamento manual.",
                )
            }
        }
    }

    fun createProductMaster(
        name: String,
        sector: String,
        category: String,
        subcategory: String,
        unit: String,
        onCreated: (ProductEntity) -> Unit,
        onError: (String) -> Unit,
    ) {
        val cleanName = ProductNormalizer.displayName(name)
        val cleanSector = ProductNormalizer.displayName(sector)
        val cleanCategory = ProductNormalizer.displayName(category)
        val cleanSubcategory = ProductNormalizer.displayName(subcategory)
        val cleanUnit = unit.trim().uppercase().ifBlank { "UN" }

        if (cleanName.isBlank() || cleanSector.isBlank() || cleanCategory.isBlank()) {
            onError("Informe nome, setor e categoria do produto mestre.")
            return
        }

        viewModelScope.launch {
            val duplicate = productRepository.findDuplicateName(cleanName)
            if (duplicate != null) {
                onCreated(duplicate)
                return@launch
            }

            val now = System.currentTimeMillis()
            val product = ProductEntity(
                normalizedName = cleanName,
                sector = cleanSector,
                category = cleanCategory,
                subcategory = cleanSubcategory.ifBlank { null },
                unit = cleanUnit,
                active = true,
                createdAt = now,
                updatedAt = now,
            )

            try {
                val id = productRepository.save(product)
                onCreated(product.copy(id = id))
            } catch (error: Exception) {
                onError(error.message ?: "Não foi possível criar o produto mestre.")
            }
        }
    }
    fun clearMessage() {
        _saveState.value = ManualEntrySaveState()
    }
}
