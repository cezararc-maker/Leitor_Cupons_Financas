package br.com.leitorcuponsfinancas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.leitorcuponsfinancas.data.AppDatabase
import br.com.leitorcuponsfinancas.data.MerchantEntity
import br.com.leitorcuponsfinancas.data.MerchantProductLinkEntity
import br.com.leitorcuponsfinancas.data.MerchantRepository
import br.com.leitorcuponsfinancas.data.MerchantSuggestion
import br.com.leitorcuponsfinancas.data.ProductEntity
import br.com.leitorcuponsfinancas.data.ProductRepository
import br.com.leitorcuponsfinancas.data.ReceiptRepository
import br.com.leitorcuponsfinancas.data.TaxonomyLevel
import br.com.leitorcuponsfinancas.data.TaxonomyNodeEntity
import br.com.leitorcuponsfinancas.data.TaxonomyProductLinkEntity
import br.com.leitorcuponsfinancas.data.TaxonomyRepository
import br.com.leitorcuponsfinancas.data.UserProfileStore
import br.com.leitorcuponsfinancas.domain.ProductNormalizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
    private val merchantRepository = MerchantRepository(database.merchantDao())
    private val taxonomyRepository = TaxonomyRepository(
        taxonomyDao = database.taxonomyDao(),
        productDao = database.productDao(),
    )
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

    val taxonomyNodes: StateFlow<List<TaxonomyNodeEntity>> =
        taxonomyRepository.nodes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val taxonomyProductLinks: StateFlow<List<TaxonomyProductLinkEntity>> =
        taxonomyRepository.productLinks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val merchants: StateFlow<List<MerchantEntity>> =
        database.merchantDao()
            .observeActive()
            .stateIn(
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
        database.merchantDao()
            .observeActive()
            .map { merchants ->
                merchants.map { merchant ->
                    MerchantSuggestion(
                        name = merchant.displayName,
                        cnpj = merchant.cnpjDigits,
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    private val _saveState = MutableStateFlow(ManualEntrySaveState())
    val saveState: StateFlow<ManualEntrySaveState> = _saveState.asStateFlow()

    init {
        viewModelScope.launch {
            taxonomyRepository.ensureBaseTaxonomy()
        }
    }

    fun save(
        merchantName: String,
        merchantCnpj: String,
        dateText: String,
        description: String,
        quantity: String,
        unit: String,
        unitPrice: String,
        productId: Long?,
        taxonomyNodeId: Long? = null,
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

                taxonomyNodeId?.let { nodeId ->
                    taxonomyRepository.linkProduct(
                        taxonomyNodeId = nodeId,
                        productId = productId,
                    )
                    val segment = taxonomyRepository.ancestry(nodeId)
                        .firstOrNull { it.level == TaxonomyLevel.SEGMENT.code }
                    if (segment != null && (merchantName.isNotBlank() || merchantCnpj.isNotBlank())) {
                        merchantRepository.resolveOrCreate(
                            name = merchantName.ifBlank { null },
                            cnpj = merchantCnpj.ifBlank { null },
                        )?.let { merchant ->
                            merchantRepository.setSegment(
                                merchant = merchant,
                                segmentNodeId = segment.id,
                            )
                        }
                    }
                }

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

    fun createProductMasterTaxonomy(
        name: String,
        taxonomyNodeId: Long,
        unit: String,
        onCreated: (ProductEntity) -> Unit,
        onError: (String) -> Unit,
    ) {
        val cleanName = ProductNormalizer.displayName(name)
        if (cleanName.isBlank()) {
            onError("Informe o nome do Produto Mestre.")
            return
        }

        viewModelScope.launch {
            val ancestry = taxonomyRepository.ancestry(taxonomyNodeId)
            val department = ancestry
                .firstOrNull { it.level == TaxonomyLevel.DEPARTMENT.code }
                ?.name
            val category = ancestry
                .firstOrNull { it.level == TaxonomyLevel.CATEGORY.code }
                ?.name
            val subcategory = ancestry
                .firstOrNull { it.level == TaxonomyLevel.SUBCATEGORY.code }
                ?.name

            if (department == null || category == null) {
                onError("Selecione pelo menos Departamento e Categoria.")
                return@launch
            }

            val duplicate = productRepository.findDuplicateName(cleanName)
            val product = if (duplicate != null) {
                duplicate
            } else {
                val now = System.currentTimeMillis()
                val candidate = ProductEntity(
                    normalizedName = cleanName,
                    sector = department,
                    category = category,
                    subcategory = subcategory,
                    unit = unit.trim().uppercase().ifBlank { "UN" },
                    active = true,
                    createdAt = now,
                    updatedAt = now,
                )
                try {
                    val id = productRepository.save(candidate)
                    candidate.copy(id = id)
                } catch (error: Exception) {
                    onError(error.message ?: "Não foi possível criar o Produto Mestre.")
                    return@launch
                }
            }

            taxonomyRepository.linkProduct(
                taxonomyNodeId = taxonomyNodeId,
                productId = product.id,
            )
            onCreated(product)
        }
    }

    fun linkProductTaxonomy(
        productId: Long,
        taxonomyNodeId: Long,
    ) {
        viewModelScope.launch {
            taxonomyRepository.linkProduct(
                taxonomyNodeId = taxonomyNodeId,
                productId = productId,
            )
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
