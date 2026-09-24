package br.com.leitorcuponsfinancas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.leitorcuponsfinancas.data.AppDatabase
import br.com.leitorcuponsfinancas.data.HistoryItemRow
import br.com.leitorcuponsfinancas.data.ItemCorrectionResult
import br.com.leitorcuponsfinancas.data.ManualDeleteResult
import br.com.leitorcuponsfinancas.data.MerchantProductLinkEntity
import br.com.leitorcuponsfinancas.data.MerchantRepository
import br.com.leitorcuponsfinancas.data.ProductEntity
import br.com.leitorcuponsfinancas.data.ProductLinkResult
import br.com.leitorcuponsfinancas.data.ProductRepository
import br.com.leitorcuponsfinancas.data.ReceiptRepository
import br.com.leitorcuponsfinancas.data.TaxonomyLevel
import br.com.leitorcuponsfinancas.data.TaxonomyNodeEntity
import br.com.leitorcuponsfinancas.data.TaxonomyProductLinkEntity
import br.com.leitorcuponsfinancas.data.TaxonomyRepository
import br.com.leitorcuponsfinancas.data.UserProfileStore
import br.com.leitorcuponsfinancas.domain.ProductNormalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryPeriodType(val label: String) {
    WEEKLY("Semanal"),
    MONTHLY("Mensal"),
    QUARTERLY("Trimestral"),
    SEMIANNUAL("Semestral"),
    ANNUAL("Anual"),
}

enum class HistorySearchMode(val label: String) {
    STARTS_WITH("Início"),
    CONTAINS("Qualquer parte"),
}

data class HistoryDateRange(
    val start: LocalDate,
    val end: LocalDate,
    val label: String,
)

data class HistoryLinkState(
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

data class HistoryEditState(
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

data class HistoryDeleteState(
    val deleting: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val userProfileStore = UserProfileStore.getInstance(application)
    private val receiptRepository = ReceiptRepository(
        receiptDao = database.receiptDao(),
        productDao = database.productDao(),
        linkDao = database.merchantProductLinkDao(),
        merchantDao = database.merchantDao(),
    )
    private val productRepository = ProductRepository(database.productDao())
    private val taxonomyRepository = TaxonomyRepository(
        taxonomyDao = database.taxonomyDao(),
        productDao = database.productDao(),
    )
    private val merchantRepository = MerchantRepository(database.merchantDao())

    private val _periodType = MutableStateFlow(HistoryPeriodType.MONTHLY)
    val periodType: StateFlow<HistoryPeriodType> = _periodType.asStateFlow()

    private val _anchorDate = MutableStateFlow(LocalDate.now())
    private val _selectedDay = MutableStateFlow<LocalDate?>(null)

    val dateRange: StateFlow<HistoryDateRange> = combine(
        _periodType,
        _anchorDate,
        _selectedDay,
    ) { type, anchor, selectedDay ->
        buildSelectedRange(type, anchor, selectedDay)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildSelectedRange(
            HistoryPeriodType.MONTHLY,
            LocalDate.now(),
            null,
        ),
    )

    val items: StateFlow<List<HistoryItemRow>> = combine(
        _periodType,
        _anchorDate,
        _selectedDay,
    ) { type, anchor, selectedDay ->
        buildSelectedRange(type, anchor, selectedDay)
    }.flatMapLatest { range ->
        receiptRepository.observeHistory(
            startDate = range.start.toString(),
            endDate = range.end.toString(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val chartBars: StateFlow<List<HistoryChartBar>> = combine(
        _periodType,
        _anchorDate,
        _selectedDay,
    ) { type, anchor, selectedDay ->
        Triple(type, anchor, selectedDay)
    }.flatMapLatest { state ->
        val type = state.first
        val anchor = state.second
        val selectedDay = state.third
        val window = HistoryChartBuilder.buildWindow(type, anchor)

        receiptRepository.observeHistory(
            startDate = window.start.toString(),
            endDate = window.end.toString(),
        ).map { rows ->
            HistoryChartBuilder.buildBars(
                type = type,
                anchor = anchor,
                selectedDay = selectedDay,
                window = window,
                rows = rows,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchMode = MutableStateFlow(HistorySearchMode.CONTAINS)
    val searchMode: StateFlow<HistorySearchMode> = _searchMode.asStateFlow()

    private val _advancedFilter = MutableStateFlow(HistoryAdvancedFilter())
    val advancedFilter: StateFlow<HistoryAdvancedFilter> = _advancedFilter.asStateFlow()

    val filteredItems: StateFlow<List<HistoryItemRow>> = combine(
        items,
        _searchQuery,
        _searchMode,
        _advancedFilter,
    ) { currentItems, query, mode, filter ->
        val searched = HistorySearchFilter.filter(
            items = currentItems,
            query = query,
            mode = mode,
        )
        HistoryAnalyticsBuilder.filter(
            rows = searched,
            filter = filter,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val analytics: StateFlow<HistoryAnalytics> =
        filteredItems
            .map(HistoryAnalyticsBuilder::build)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HistoryAnalytics(),
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

    val learnedLinks: StateFlow<List<MerchantProductLinkEntity>> =
        database.merchantProductLinkDao()
            .observeAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    private val _linkState = MutableStateFlow(HistoryLinkState())
    val linkState: StateFlow<HistoryLinkState> = _linkState.asStateFlow()

    private val _editState = MutableStateFlow(HistoryEditState())
    val editState: StateFlow<HistoryEditState> = _editState.asStateFlow()

    private val _deleteState = MutableStateFlow(HistoryDeleteState())
    val deleteState: StateFlow<HistoryDeleteState> = _deleteState.asStateFlow()

    init {
        viewModelScope.launch {
            taxonomyRepository.ensureBaseTaxonomy()
        }
    }

    fun selectPeriod(type: HistoryPeriodType) {
        _periodType.value = type
        _selectedDay.value = null
    }

    fun selectChartBar(bar: HistoryChartBar) {
        _anchorDate.value = bar.start
        _selectedDay.value = if (_periodType.value == HistoryPeriodType.WEEKLY) {
            bar.start
        } else {
            null
        }
    }

    fun updateSearchQuery(value: String) {
        _searchQuery.value = value
    }

    fun selectSearchMode(mode: HistorySearchMode) {
        _searchMode.value = mode
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun updateAdvancedFilter(filter: HistoryAdvancedFilter) {
        _advancedFilter.value = filter
    }

    fun clearAdvancedFilter() {
        _advancedFilter.value = HistoryAdvancedFilter()
    }

    fun previousPeriod() {
        _anchorDate.value = shiftAnchor(
            _anchorDate.value,
            _periodType.value,
            -1,
        )
        _selectedDay.value = _selectedDay.value?.let { selected ->
            if (_periodType.value == HistoryPeriodType.WEEKLY) {
                selected.minusWeeks(1)
            } else {
                null
            }
        }
    }

    fun nextPeriod() {
        _anchorDate.value = shiftAnchor(
            _anchorDate.value,
            _periodType.value,
            1,
        )
        _selectedDay.value = _selectedDay.value?.let { selected ->
            if (_periodType.value == HistoryPeriodType.WEEKLY) {
                selected.plusWeeks(1)
            } else {
                null
            }
        }
    }

    fun saveItemCorrection(
        item: HistoryItemRow,
        description: String,
        quantity: String,
        unit: String,
        unitPrice: String,
        totalAmount: String,
    ) {
        if (_editState.value.saving) return

        _editState.value = HistoryEditState(saving = true)

        viewModelScope.launch {
            when (
                val result = receiptRepository.saveItemCorrection(
                    item = item,
                    description = description,
                    quantity = quantity,
                    unit = unit,
                    unitPrice = unitPrice,
                    totalAmount = totalAmount,
                    actor = userProfileStore.profile.value,
                )
            ) {
                is ItemCorrectionResult.Success -> {
                    _editState.value = HistoryEditState(
                        message = if (result.hasCorrection) {
                            "Correção salva. Os dados originais da NFC-e foram preservados."
                        } else {
                            "Os valores informados são iguais aos dados originais da NFC-e."
                        },
                    )
                }

                is ItemCorrectionResult.Error -> {
                    _editState.value = HistoryEditState(error = result.message)
                }
            }
        }
    }

    fun restoreOriginalItem(item: HistoryItemRow) {
        if (_editState.value.saving) return

        _editState.value = HistoryEditState(saving = true)

        viewModelScope.launch {
            when (val result = receiptRepository.clearItemCorrection(item.itemId)) {
                is ItemCorrectionResult.Success -> {
                    _editState.value = HistoryEditState(
                        message = "Correções removidas. O item voltou aos dados originais da NFC-e.",
                    )
                }

                is ItemCorrectionResult.Error -> {
                    _editState.value = HistoryEditState(error = result.message)
                }
            }
        }
    }

    fun clearEditMessage() {
        _editState.value = HistoryEditState()
    }

    fun createProductAndLink(
        item: HistoryItemRow,
        name: String,
        sector: String,
        category: String,
        subcategory: String,
        unit: String,
        taxonomyNodeId: Long? = null,
    ) {
        val cleanName = ProductNormalizer.displayName(name)
        val cleanSector = ProductNormalizer.displayName(sector)
        val cleanCategory = ProductNormalizer.displayName(category)

        if (cleanName.isBlank() || cleanSector.isBlank() || cleanCategory.isBlank()) {
            _linkState.value = HistoryLinkState(
                error = "Informe nome, setor e categoria para criar o produto mestre.",
            )
            return
        }

        if (_linkState.value.saving) return
        _linkState.value = HistoryLinkState(saving = true)

        viewModelScope.launch {
            val duplicate = productRepository.findDuplicateName(cleanName)

            if (duplicate != null) {
                applyTaxonomy(
                    item = item,
                    productId = duplicate.id,
                    taxonomyNodeId = taxonomyNodeId,
                )
                when (
                    val result = receiptRepository.linkHistoryItem(
                        item = item,
                        productId = duplicate.id,
                    )
                ) {
                    is ProductLinkResult.Success -> {
                        _linkState.value = HistoryLinkState(
                            message = buildString {
                                append("O produto mestre \"${duplicate.normalizedName}\" já existia e foi utilizado.")
                                append(" Itens atualizados: ${result.updatedItems}.")
                            },
                        )
                    }

                    is ProductLinkResult.Error -> {
                        _linkState.value = HistoryLinkState(error = result.message)
                    }
                }
                return@launch
            }

            val now = System.currentTimeMillis()
            val product = ProductEntity(
                fiscalDescription = item.fiscalDescription.takeIf {
                    item.sourceType != "MANUAL"
                },
                normalizedName = cleanName,
                sector = cleanSector,
                category = cleanCategory,
                subcategory = ProductNormalizer.displayName(subcategory).ifBlank { null },
                unit = unit.trim().uppercase().ifBlank { item.displayUnit ?: "UN" },
                active = true,
                createdAt = now,
                updatedAt = now,
            )

            try {
                val newId = productRepository.save(product)
                val created = product.copy(id = newId)
                applyTaxonomy(
                    item = item,
                    productId = newId,
                    taxonomyNodeId = taxonomyNodeId,
                )

                when (
                    val result = receiptRepository.linkHistoryItem(
                        item = item,
                        productId = newId,
                    )
                ) {
                    is ProductLinkResult.Success -> {
                        _linkState.value = HistoryLinkState(
                            message = buildString {
                                append("Produto mestre \"${created.normalizedName}\" criado e vinculado.")
                                append(" Itens atualizados: ${result.updatedItems}.")
                            },
                        )
                    }

                    is ProductLinkResult.Error -> {
                        _linkState.value = HistoryLinkState(error = result.message)
                    }
                }
            } catch (error: Exception) {
                _linkState.value = HistoryLinkState(
                    error = error.message ?: "Não foi possível criar o produto mestre.",
                )
            }
        }
    }

    fun createProductAndLinkTaxonomy(
        item: HistoryItemRow,
        name: String,
        taxonomyNodeId: Long,
        unit: String,
    ) {
        if (_linkState.value.saving) return
        _linkState.value = HistoryLinkState(saving = true)

        viewModelScope.launch {
            val ancestry = taxonomyRepository.ancestry(taxonomyNodeId)
            val department = ancestry
                .firstOrNull { it.level == TaxonomyLevel.DEPARTMENT.code }
                ?.name
                ?: "Outros"
            val category = ancestry
                .firstOrNull { it.level == TaxonomyLevel.CATEGORY.code }
                ?.name
                ?: ancestry.lastOrNull()?.name
                ?: "Outros"
            val subcategory = ancestry
                .firstOrNull { it.level == TaxonomyLevel.SUBCATEGORY.code }
                ?.name

            val cleanName = ProductNormalizer.displayName(name)
            if (cleanName.isBlank()) {
                _linkState.value = HistoryLinkState(
                    error = "Informe o nome do Produto Mestre.",
                )
                return@launch
            }

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
                    sector = department,
                    category = category,
                    subcategory = subcategory,
                    unit = unit.trim().uppercase().ifBlank { item.displayUnit ?: "UN" },
                    active = true,
                    createdAt = now,
                    updatedAt = now,
                )

                try {
                    val id = productRepository.save(candidate)
                    candidate.copy(id = id)
                } catch (error: Exception) {
                    _linkState.value = HistoryLinkState(
                        error = error.message ?: "Não foi possível criar o Produto Mestre.",
                    )
                    return@launch
                }
            }

            applyTaxonomy(
                item = item,
                productId = product.id,
                taxonomyNodeId = taxonomyNodeId,
            )

            when (
                val result = receiptRepository.linkHistoryItem(
                    item = item,
                    productId = product.id,
                )
            ) {
                is ProductLinkResult.Success -> {
                    _linkState.value = HistoryLinkState(
                        message = if (duplicate != null) {
                            "O Produto Mestre existente foi reutilizado e classificado na taxonomia."
                        } else {
                            "Produto Mestre criado, classificado e vinculado."
                        },
                    )
                }

                is ProductLinkResult.Error -> {
                    _linkState.value = HistoryLinkState(error = result.message)
                }
            }
        }
    }

    fun deleteManualItem(item: HistoryItemRow) {
        if (_deleteState.value.deleting) return

        _deleteState.value = HistoryDeleteState(deleting = true)

        viewModelScope.launch {
            when (val result = receiptRepository.deleteManualHistoryItem(item)) {
                ManualDeleteResult.Success -> {
                    _deleteState.value = HistoryDeleteState(
                        message = "Lançamento manual excluído do histórico.",
                    )
                }

                is ManualDeleteResult.Error -> {
                    _deleteState.value = HistoryDeleteState(
                        error = result.message,
                    )
                }
            }
        }
    }

    fun clearDeleteMessage() {
        _deleteState.value = HistoryDeleteState()
    }

    fun linkItem(
        item: HistoryItemRow,
        product: ProductEntity,
        taxonomyNodeId: Long? = null,
    ) {
        if (_linkState.value.saving) return

        _linkState.value = HistoryLinkState(saving = true)

        viewModelScope.launch {
            applyTaxonomy(
                item = item,
                productId = product.id,
                taxonomyNodeId = taxonomyNodeId,
            )

            when (
                val result = receiptRepository.linkHistoryItem(
                    item = item,
                    productId = product.id,
                )
            ) {
                is ProductLinkResult.Success -> {
                    _linkState.value = HistoryLinkState(
                        message = buildString {
                            append('"')
                            append(item.fiscalDescription)
                            append('"')
                            append(" vinculado a ")
                            append('"')
                            append(product.normalizedName)
                            append('"')
                            append(". Itens atualizados no histórico: ")
                            append(result.updatedItems)
                            append(".")
                        },
                    )
                }

                is ProductLinkResult.Error -> {
                    _linkState.value = HistoryLinkState(
                        error = result.message,
                    )
                }
            }
        }
    }

    private suspend fun applyTaxonomy(
        item: HistoryItemRow,
        productId: Long,
        taxonomyNodeId: Long?,
    ) {
        val nodeId = taxonomyNodeId ?: return

        taxonomyRepository.linkProduct(
            taxonomyNodeId = nodeId,
            productId = productId,
        )

        val segment = taxonomyRepository.ancestry(nodeId)
            .firstOrNull { it.level == TaxonomyLevel.SEGMENT.code }
            ?: return

        val merchantId = item.merchantId ?: return
        val merchant = database.merchantDao().findById(merchantId) ?: return

        if (merchant.segmentNodeId == null) {
            merchantRepository.setSegment(
                merchant = merchant,
                segmentNodeId = segment.id,
                source = "TAXONOMY",
            )
        }
    }

    fun clearLinkMessage() {
        _linkState.value = HistoryLinkState()
    }

    private fun buildSelectedRange(
        type: HistoryPeriodType,
        anchor: LocalDate,
        selectedDay: LocalDate?,
    ): HistoryDateRange {
        if (type == HistoryPeriodType.WEEKLY && selectedDay != null) {
            val week = buildRange(type, anchor)
            if (!selectedDay.isBefore(week.start) && !selectedDay.isAfter(week.end)) {
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                return HistoryDateRange(
                    start = selectedDay,
                    end = selectedDay,
                    label = selectedDay.format(formatter),
                )
            }
        }

        return buildRange(type, anchor)
    }
    private fun buildRange(
        type: HistoryPeriodType,
        anchor: LocalDate,
    ): HistoryDateRange {
        val displayDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        return when (type) {
            HistoryPeriodType.WEEKLY -> {
                val start = anchor.with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY),
                )
                val end = start.plusDays(6)
                HistoryDateRange(
                    start = start,
                    end = end,
                    label = "${start.format(displayDate)} a ${end.format(displayDate)}",
                )
            }

            HistoryPeriodType.MONTHLY -> {
                val start = anchor.withDayOfMonth(1)
                val end = anchor.withDayOfMonth(anchor.lengthOfMonth())
                HistoryDateRange(
                    start = start,
                    end = end,
                    label = "%02d/%04d".format(anchor.monthValue, anchor.year),
                )
            }

            HistoryPeriodType.QUARTERLY -> {
                val quarter = ((anchor.monthValue - 1) / 3) + 1
                val startMonth = ((quarter - 1) * 3) + 1
                val start = LocalDate.of(anchor.year, startMonth, 1)
                val end = start.plusMonths(3).minusDays(1)
                HistoryDateRange(
                    start = start,
                    end = end,
                    label = "${quarter}º trimestre/${anchor.year}",
                )
            }

            HistoryPeriodType.SEMIANNUAL -> {
                val semester = if (anchor.monthValue <= 6) 1 else 2
                val startMonth = if (semester == 1) 1 else 7
                val start = LocalDate.of(anchor.year, startMonth, 1)
                val end = start.plusMonths(6).minusDays(1)
                HistoryDateRange(
                    start = start,
                    end = end,
                    label = "${semester}º semestre/${anchor.year}",
                )
            }

            HistoryPeriodType.ANNUAL -> {
                val start = LocalDate.of(anchor.year, 1, 1)
                val end = LocalDate.of(anchor.year, 12, 31)
                HistoryDateRange(
                    start = start,
                    end = end,
                    label = anchor.year.toString(),
                )
            }
        }
    }

    private fun shiftAnchor(
        anchor: LocalDate,
        type: HistoryPeriodType,
        direction: Long,
    ): LocalDate = when (type) {
        HistoryPeriodType.WEEKLY -> anchor.plusWeeks(direction)
        HistoryPeriodType.MONTHLY -> anchor.plusMonths(direction)
        HistoryPeriodType.QUARTERLY -> anchor.plusMonths(3 * direction)
        HistoryPeriodType.SEMIANNUAL -> anchor.plusMonths(6 * direction)
        HistoryPeriodType.ANNUAL -> anchor.plusYears(direction)
    }
}
