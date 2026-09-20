package br.com.leitorcuponsfinancas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.leitorcuponsfinancas.data.AppDatabase
import br.com.leitorcuponsfinancas.data.HistoryItemRow
import br.com.leitorcuponsfinancas.data.ItemCorrectionResult
import br.com.leitorcuponsfinancas.data.ManualDeleteResult
import br.com.leitorcuponsfinancas.data.ProductEntity
import br.com.leitorcuponsfinancas.data.ProductLinkResult
import br.com.leitorcuponsfinancas.data.ProductRepository
import br.com.leitorcuponsfinancas.data.ReceiptRepository
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
    )
    private val productRepository = ProductRepository(database.productDao())

    private val _periodType = MutableStateFlow(HistoryPeriodType.MONTHLY)
    val periodType: StateFlow<HistoryPeriodType> = _periodType.asStateFlow()

    private val _anchorDate = MutableStateFlow(LocalDate.now())

    val dateRange: StateFlow<HistoryDateRange> = combine(
        _periodType,
        _anchorDate,
    ) { type, anchor ->
        buildRange(type, anchor)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildRange(HistoryPeriodType.MONTHLY, LocalDate.now()),
    )

    val items: StateFlow<List<HistoryItemRow>> = combine(
        _periodType,
        _anchorDate,
    ) { type, anchor ->
        buildRange(type, anchor)
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchMode = MutableStateFlow(HistorySearchMode.CONTAINS)
    val searchMode: StateFlow<HistorySearchMode> = _searchMode.asStateFlow()

    val filteredItems: StateFlow<List<HistoryItemRow>> = combine(
        items,
        _searchQuery,
        _searchMode,
    ) { currentItems, query, mode ->
        HistorySearchFilter.filter(
            items = currentItems,
            query = query,
            mode = mode,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val products: StateFlow<List<ProductEntity>> = productRepository.products.stateIn(
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

    fun selectPeriod(type: HistoryPeriodType) {
        _periodType.value = type
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

    fun previousPeriod() {
        _anchorDate.value = shiftAnchor(
            _anchorDate.value,
            _periodType.value,
            -1,
        )
    }

    fun nextPeriod() {
        _anchorDate.value = shiftAnchor(
            _anchorDate.value,
            _periodType.value,
            1,
        )
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
    ) {
        if (_linkState.value.saving) return

        _linkState.value = HistoryLinkState(saving = true)

        viewModelScope.launch {
            when (
                val result = receiptRepository.linkHistoryItem(
                    item = item,
                    productId = product.id,
                )
            ) {
                is ProductLinkResult.Success -> {
                    _linkState.value = HistoryLinkState(
                        message = buildString {
                            append("\"${item.fiscalDescription}\" vinculado a \"${product.normalizedName}\".")
                            append(" Itens atualizados no histórico: ${result.updatedItems}.")
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

    fun clearLinkMessage() {
        _linkState.value = HistoryLinkState()
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
