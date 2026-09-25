package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.leitorcuponsfinancas.data.HistoryItemRow
import br.com.leitorcuponsfinancas.data.ProductEntity
import br.com.leitorcuponsfinancas.domain.ProductNormalizer
import br.com.leitorcuponsfinancas.domain.ProductSuggestionEngine
import br.com.leitorcuponsfinancas.domain.SmartProductSuggestion
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = viewModel(),
) {
    val periodType by historyViewModel.periodType.collectAsStateWithLifecycle()
    val dateRange by historyViewModel.dateRange.collectAsStateWithLifecycle()
    val chartBars by historyViewModel.chartBars.collectAsStateWithLifecycle()
    val historyItems by historyViewModel.items.collectAsStateWithLifecycle()
    val filteredItems by historyViewModel.filteredItems.collectAsStateWithLifecycle()
    val searchQuery by historyViewModel.searchQuery.collectAsStateWithLifecycle()
    val searchMode by historyViewModel.searchMode.collectAsStateWithLifecycle()
    val advancedFilter by historyViewModel.advancedFilter.collectAsStateWithLifecycle()
    val analytics by historyViewModel.analytics.collectAsStateWithLifecycle()
    val products by historyViewModel.products.collectAsStateWithLifecycle()
    val taxonomyNodes by historyViewModel.taxonomyNodes.collectAsStateWithLifecycle()
    val taxonomyProductLinks by historyViewModel.taxonomyProductLinks.collectAsStateWithLifecycle()
    val learnedLinks by historyViewModel.learnedLinks.collectAsStateWithLifecycle()
    val linkState by historyViewModel.linkState.collectAsStateWithLifecycle()
    val editState by historyViewModel.editState.collectAsStateWithLifecycle()
    val deleteState by historyViewModel.deleteState.collectAsStateWithLifecycle()

    var linkingItem by remember { mutableStateOf<HistoryItemRow?>(null) }
    var editingItem by remember { mutableStateOf<HistoryItemRow?>(null) }
    var deletingItem by remember { mutableStateOf<HistoryItemRow?>(null) }
    var reviewingItemId by rememberSaveable { mutableStateOf<Long?>(null) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var sortMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(HistorySortMode.DEFAULT) }
    var filterDialogOpen by rememberSaveable { mutableStateOf(false) }

    val sortedItems = remember(filteredItems, sortMode) {
        sortHistoryItems(filteredItems, sortMode)
    }

    val unrecognizedItems = sortedItems.filter { it.productId == null }

    val total = filteredItems
        .mapNotNull { it.displayTotalAmount?.toBigDecimalOrNull() }
        .fold(BigDecimal.ZERO, BigDecimal::add)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Histórico e Gastos",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = {
                        searchOpen = !searchOpen
                        if (!searchOpen) historyViewModel.clearSearch()
                    },
                ) {
                    Text(
                        text = if (searchOpen) "×" else "⌕",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HistoryPeriodType.entries.forEach { type ->
                    if (type == periodType) {
                        Button(onClick = { historyViewModel.selectPeriod(type) }) {
                            Text(type.label)
                        }
                    } else {
                        OutlinedButton(onClick = { historyViewModel.selectPeriod(type) }) {
                            Text(type.label)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = historyViewModel::previousPeriod) {
                    Text("‹")
                }

                Text(
                    text = dateRange.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )

                OutlinedButton(onClick = historyViewModel::nextPeriod) {
                    Text("›")
                }
            }
        }
        item {
            HistoryPeriodChart(
                bars = chartBars,
                onSelect = historyViewModel::selectChartBar,
            )
        }

        if (searchOpen) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 320.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier
                                .height(46.dp)
                                .padding(start = 16.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "⌕",
                                style = MaterialTheme.typography.titleMedium,
                            )

                            BasicTextField(
                                value = searchQuery,
                                onValueChange = {
                                    historyViewModel.updateSearchQuery(
                                        TextInputRules.capitalizeFirstLetter(it),
                                    )
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isBlank()) {
                                        Text(
                                            text = "Pesquisar item...",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                    innerTextField()
                                },
                            )

                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = historyViewModel::clearSearch) {
                                    Text("×")
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = searchMode == HistorySearchMode.STARTS_WITH,
                            onClick = {
                                historyViewModel.selectSearchMode(HistorySearchMode.STARTS_WITH)
                            },
                            label = { Text("Início") },
                        )
                        FilterChip(
                            selected = searchMode == HistorySearchMode.CONTAINS,
                            onClick = {
                                historyViewModel.selectSearchMode(HistorySearchMode.CONTAINS)
                            },
                            label = { Text("Qualquer parte") },
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                OutlinedButton(
                    onClick = { filterDialogOpen = true },
                ) {
                    Text(
                        if (advancedFilter.activeCount == 0) {
                            "Filtros"
                        } else {
                            "Filtros (${advancedFilter.activeCount})"
                        },
                    )
                }

                Box {
                    OutlinedButton(
                        onClick = { sortMenuExpanded = true },
                    ) {
                        Text("Ordenar: ${sortMode.label}")
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                    ) {
                        HistorySortMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label) },
                                onClick = {
                                    sortMode = mode
                                    sortMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Resumo analítico",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Total: R$ ${formatHistoryMoney(BigDecimal.valueOf(analytics.totalSpent))}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text("Compras: ${analytics.purchaseCount} • Itens: ${analytics.itemCount}")
                    Text(
                        "Média por compra: R$ ${formatHistoryMoney(BigDecimal.valueOf(analytics.averagePurchase))}",
                    )
                    analytics.topProduct?.let { Text("Produto com maior gasto: $it") }
                    analytics.topCategory?.let { Text("Categoria com maior gasto: $it") }
                    analytics.topMerchant?.let { Text("Estabelecimento com maior gasto: $it") }

                    if (searchQuery.isNotBlank() || advancedFilter.activeCount > 0) {
                        Text(
                            text = "Exibindo ${filteredItems.size} de ${historyItems.size} itens do período.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Text(
                        text = "Sem Produto Mestre: ${unrecognizedItems.size}",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    if (unrecognizedItems.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { reviewingItemId = unrecognizedItems.first().itemId },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Revisar não reconhecidos (${unrecognizedItems.size})")
                        }
                    }
                }
            }
        }

        linkState.message?.let { message ->
            item {
                HistoryMessageCard(
                    title = "Vínculo aprendido",
                    message = message,
                )
            }
        }

        linkState.error?.let { error ->
            item {
                HistoryMessageCard(
                    title = "Não foi possível vincular",
                    message = error,
                )
            }
        }

        editState.message?.let { message ->
            item {
                HistoryMessageCard(
                    title = "Item atualizado",
                    message = message,
                )
            }
        }

        editState.error?.let { error ->
            item {
                HistoryMessageCard(
                    title = "Não foi possível editar o item",
                    message = error,
                )
            }
        }

        deleteState.message?.let { message ->
            item {
                HistoryMessageCard(
                    title = "Lançamento excluído",
                    message = message,
                )
            }
        }

        deleteState.error?.let { error ->
            item {
                HistoryMessageCard(
                    title = "Não foi possível excluir",
                    message = error,
                )
            }
        }

        if (filteredItems.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "Nenhum item neste período."
                            } else {
                                "Nenhum item encontrado."
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "Use as setas ou altere o tipo de período para consultar outros lançamentos."
                            } else {
                                "Tente outro termo ou altere o tipo de pesquisa."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        items(
            items = sortedItems,
            key = { it.itemId },
        ) { item ->
            HistoryItemCard(
                item = item,
                onEdit = {
                    historyViewModel.clearEditMessage()
                    editingItem = item
                },
                onEditLink = {
                    historyViewModel.clearLinkMessage()
                    linkingItem = item
                },
                onDelete = {
                    historyViewModel.clearDeleteMessage()
                    deletingItem = item
                },
            )
        }
    }

    if (filterDialogOpen) {
        HistoryAdvancedFilterDialog(
            current = advancedFilter,
            rows = historyItems,
            products = products,
            onDismiss = { filterDialogOpen = false },
            onApply = { filter ->
                historyViewModel.updateAdvancedFilter(filter)
                filterDialogOpen = false
            },
            onClear = {
                historyViewModel.clearAdvancedFilter()
                filterDialogOpen = false
            },
        )
    }

    linkingItem?.let { item ->
        val smartSuggestion = remember(item.itemId, products, learnedLinks) {
            ProductSuggestionEngine.suggest(
                description = item.displayDescription,
                unit = item.displayUnit,
                products = products,
                learnedLinks = learnedLinks,
            )
        }

        TaxonomyProductLinkDialog(
            item = item,
            products = products,
            nodes = taxonomyNodes,
            productLinks = taxonomyProductLinks,
            smartSuggestion = smartSuggestion,
            saving = linkState.saving,
            onDismiss = { linkingItem = null },
            onSelect = { product, taxonomyNodeId ->
                historyViewModel.linkItem(
                    item = item,
                    product = product,
                    taxonomyNodeId = taxonomyNodeId,
                )
                linkingItem = null
            },
            onCreate = { name, taxonomyNodeId, unit ->
                historyViewModel.createProductAndLinkTaxonomy(
                    item = item,
                    name = name,
                    taxonomyNodeId = taxonomyNodeId,
                    unit = unit,
                )
                linkingItem = null
            },
        )
    }

    editingItem?.let { item ->
        EditHistoryItemDialog(
            item = item,
            saving = editState.saving,
            onDismiss = { editingItem = null },
            onEditLink = {
                historyViewModel.clearLinkMessage()
                linkingItem = item
                editingItem = null
            },
            onRestoreOriginal = {
                historyViewModel.restoreOriginalItem(item)
                editingItem = null
            },
            onSave = { description, quantity, unit, unitPrice, totalAmount ->
                historyViewModel.saveItemCorrection(
                    item = item,
                    description = description,
                    quantity = quantity,
                    unit = unit,
                    unitPrice = unitPrice,
                    totalAmount = totalAmount,
                )
                editingItem = null
            },
        )
    }

    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = {
                if (!deleteState.deleting) deletingItem = null
            },
            title = { Text("Excluir lançamento manual?") },
            text = {
                Text(
                    text = "O item \"${item.displayDescription}\" será removido do Histórico e Gastos. Esta ação não afeta o cadastro mestre de produtos.",
                )
            },
            confirmButton = {
                Button(
                    enabled = !deleteState.deleting,
                    onClick = {
                        historyViewModel.deleteManualItem(item)
                        deletingItem = null
                    },
                ) {
                    Text(if (deleteState.deleting) "Excluindo..." else "Excluir")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !deleteState.deleting,
                    onClick = { deletingItem = null },
                ) {
                    Text("Cancelar")
                }
            },
        )
    }

    reviewingItemId?.let { itemId ->
        val reviewItem = unrecognizedItems
            .firstOrNull { it.itemId == itemId }
            ?: unrecognizedItems.firstOrNull()

        reviewItem?.let { currentItem ->
            val reviewIndex = unrecognizedItems.indexOfFirst { it.itemId == currentItem.itemId }
            val nextItemId = unrecognizedItems
                .getOrNull(reviewIndex + 1)
                ?.itemId

            val smartSuggestion = remember(currentItem.itemId, products, learnedLinks) {
                ProductSuggestionEngine.suggest(
                    description = currentItem.displayDescription,
                    unit = currentItem.displayUnit,
                    products = products,
                    learnedLinks = learnedLinks,
                )
            }

            TaxonomyProductLinkDialog(
                item = currentItem,
                products = products,
                nodes = taxonomyNodes,
                productLinks = taxonomyProductLinks,
                smartSuggestion = smartSuggestion,
                saving = linkState.saving,
                onDismiss = { reviewingItemId = null },
                onSelect = { product, taxonomyNodeId ->
                    historyViewModel.linkItem(
                        item = currentItem,
                        product = product,
                        taxonomyNodeId = taxonomyNodeId,
                    )
                    reviewingItemId = nextItemId
                },
                onCreate = { name, taxonomyNodeId, unit ->
                    historyViewModel.createProductAndLinkTaxonomy(
                        item = currentItem,
                        name = name,
                        taxonomyNodeId = taxonomyNodeId,
                        unit = unit,
                    )
                    reviewingItemId = nextItemId
                },
            )
        }
    }
}

@Composable
private fun HistoryPeriodChart(
    bars: List<HistoryChartBar>,
    onSelect: (HistoryChartBar) -> Unit,
) {
    if (bars.isEmpty()) return

    val selectedIndex = bars.indexOfFirst { it.selected }
        .takeIf { it >= 0 }
        ?: 0
    val firstVisible = maxOf(0, selectedIndex - 2)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = firstVisible,
    )
    val maxTotal = bars.maxOfOrNull { it.total } ?: BigDecimal.ZERO

    LaunchedEffect(selectedIndex, bars.size) {
        if (bars.isNotEmpty()) {
            listState.animateScrollToItem(maxOf(0, selectedIndex - 2))
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Gastos por período",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Toque para filtrar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                )
            }

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(
                    items = bars,
                    key = { it.key },
                ) { bar ->
                    val ratio = if (maxTotal > BigDecimal.ZERO) {
                        (bar.total.toDouble() / maxTotal.toDouble())
                            .coerceIn(0.0, 1.0)
                    } else {
                        0.0
                    }
                    val barHeight = (10.0 + 58.0 * ratio).dp
                    val barColor = if (bar.selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
                    }

                    Column(
                        modifier = Modifier
                            .width(56.dp)
                            .clickable { onSelect(bar) }
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = formatCompactChartValue(bar.total),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (bar.selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                        )

                        Box(
                            modifier = Modifier
                                .height(72.dp)
                                .width(28.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Surface(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(barHeight),
                                shape = RoundedCornerShape(7.dp),
                                color = barColor,
                            ) {}
                        }

                        Text(
                            text = bar.label,
                            style = if (bar.selected) {
                                MaterialTheme.typography.labelMedium
                            } else {
                                MaterialTheme.typography.bodySmall
                            },
                            color = if (bar.selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun formatCompactChartValue(value: BigDecimal): String {
    if (value == BigDecimal.ZERO) return "R$ 0"

    val absolute = value.abs()
    val number = if (absolute >= BigDecimal("1000")) {
        value
            .divide(BigDecimal("1000"), 1, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
            .replace(".", ",") + "k"
    } else {
        val scale = if (absolute < BigDecimal("100")) 1 else 0
        value
            .setScale(scale, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
            .replace(".", ",")
    }

    return "R$ $number"
}

@Composable
private fun HistoryItemCard(
    item: HistoryItemRow,
    onEdit: () -> Unit,
    onEditLink: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember(item.itemId) { mutableStateOf(false) }

    val sourceLabel = if (item.sourceType == "MANUAL") "Manual" else "NFC-e"
    val header = listOfNotNull(
        item.issuedAt?.take(10),
        item.merchantName,
        sourceLabel,
    ).joinToString(" • ")

    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    if (header.isNotBlank()) {
                        Text(
                            text = header,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        )
                    }

                    Text(
                        text = item.displayDescription,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Text(
                            text = "⋮",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar item") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (item.productId == null) {
                                        "Vincular / editar vínculo"
                                    } else {
                                        "Editar vinculação"
                                    },
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onEditLink()
                            },
                        )

                        if (item.sourceType == "MANUAL") {
                            DropdownMenuItem(
                                text = { Text("Excluir") },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = listOfNotNull(
                            item.displayQuantity?.let { "Qtd. ${formatHistoryNumber(it)}" },
                            item.displayUnit?.let { it },
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    item.displayUnitPrice?.let { price ->
                        Text(
                            text = buildString {
                                append("R$ ${formatHistoryMoney(price)}")
                                item.displayUnit?.let { append(" / $it") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item.displayTotalAmount?.let { total ->
                    Text(
                        text = "R$ ${formatHistoryMoney(total)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            if (item.manuallyEdited) {
                Text(
                    text = buildString {
                        append("✎ Corrigido")
                        item.correctedByName?.let { append(" por $it") }
                        item.correctedAt?.let { append(" em ${formatAuditDateTime(it)}") }
                        append(" • original preservado")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            val auxiliary = buildList {
                item.itemCode?.let { add("Cód. $it") }
                item.createdByName?.let { add("Incluído por $it") }
            }.joinToString(" • ")

            if (auxiliary.isNotBlank()) {
                Text(
                    text = auxiliary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                )
            }

            if (item.productId != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                ) {
                    Text(
                        text = buildString {
                            append("Produto mestre: ")
                            append(item.productName ?: "Produto cadastrado")
                            item.sector?.let { append(" • $it") }
                            item.category?.let { append(" • $it") }
                            item.subcategory?.let { append(" • $it") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            } else {
                Text(
                    text = "Sem vínculo com produto mestre",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onEditLink,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Vincular a produto")
                }
            }
        }
    }
}
@Composable
private fun EditHistoryItemDialog(
    item: HistoryItemRow,
    saving: Boolean,
    onDismiss: () -> Unit,
    onEditLink: () -> Unit,
    onRestoreOriginal: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit,
) {
    var description by remember(item.itemId, item.correctedAt) {
        mutableStateOf(item.displayDescription)
    }
    var quantity by remember(item.itemId, item.correctedAt) {
        mutableStateOf(item.displayQuantity?.replace(".", ",").orEmpty())
    }
    var unit by remember(item.itemId, item.correctedAt) {
        mutableStateOf(item.displayUnit.orEmpty())
    }
    var unitPrice by remember(item.itemId, item.correctedAt) {
        mutableStateOf(CurrencyInputFormatter.fromStoredDecimal(item.displayUnitPrice))
    }
    var totalAmount by remember(item.itemId, item.correctedAt) {
        mutableStateOf(CurrencyInputFormatter.fromStoredDecimal(item.displayTotalAmount))
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Editar item") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        text = if (item.sourceType == "MANUAL") {
                            "A edição ficará registrada com usuário e data."
                        } else {
                            "Os dados originais da NFC-e serão preservados. A correção altera apenas a visualização e os relatórios do aplicativo."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (item.sourceType != "MANUAL") {
                    item {
                        Text(
                            text = "Original da NFC-e: ${item.fiscalDescription}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                item {
                    OutlinedButton(
                        enabled = !saving,
                        onClick = onEditLink,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (item.productId == null) {
                                "Vincular a produto mestre"
                            } else {
                                "Editar vinculação: ${item.productName ?: "Produto cadastrado"}"
                            },
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = TextInputRules.capitalizeFirstLetter(it) },
                        label = { Text("Descrição") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantidade") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unidade") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    CurrencyTextField(
                        value = unitPrice,
                        onValueChange = { unitPrice = it },
                        label = { Text("Valor unitário") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    CurrencyTextField(
                        value = totalAmount,
                        onValueChange = { totalAmount = it },
                        label = { Text("Valor total") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = description.isNotBlank() && !saving,
                onClick = { onSave(description, quantity, unit, unitPrice, totalAmount) },
            ) {
                Text(if (saving) "Salvando..." else "Salvar correção")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.manuallyEdited) {
                    TextButton(
                        enabled = !saving,
                        onClick = onRestoreOriginal,
                    ) {
                        Text("Restaurar original")
                    }
                }
                TextButton(
                    enabled = !saving,
                    onClick = onDismiss,
                ) {
                    Text("Cancelar")
                }
            }
        },
    )
}
@Composable
private fun UnrecognizedReviewDialog(
    item: HistoryItemRow,
    position: Int,
    total: Int,
    products: List<ProductEntity>,
    smartSuggestion: SmartProductSuggestion?,
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    onSelect: (ProductEntity) -> Unit,
    onCreateSuggested: (SmartProductSuggestion.NewProduct) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("Revisar não reconhecidos")
                Text(
                    text = "$position de $total",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 470.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text = item.fiscalDescription,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                item.itemCode?.let { code ->
                    item {
                        Text(
                            text = "Código no estabelecimento: $code",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                item.merchantName?.let { merchant ->
                    item {
                        Text(
                            text = merchant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                smartSuggestion?.let { suggestion ->
                    item {
                        SmartSuggestionCard(
                            suggestion = suggestion,
                            onUseExisting = onSelect,
                            onCreateNew = onCreateSuggested,
                        )
                    }
                }

                item {
                    Text(
                        text = "Escolha o produto mestre. O vínculo também será reaplicado aos itens equivalentes deste estabelecimento.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (products.isEmpty()) {
                    item {
                        Text("Nenhum produto mestre cadastrado.")
                    }
                } else {
                    items(
                        items = products,
                        key = { it.id },
                    ) { product ->
                        OutlinedButton(
                            onClick = { onSelect(product) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(product.normalizedName)
                                Text(
                                    text = listOfNotNull(
                                        product.sector,
                                        product.category,
                                        product.subcategory,
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSkip,
                enabled = position < total,
            ) {
                Text("Pular")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        },
    )
}

@Composable
private fun ProductLinkDialog(
    item: HistoryItemRow,
    products: List<ProductEntity>,
    smartSuggestion: SmartProductSuggestion?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSelect: (ProductEntity) -> Unit,
    onCreateProduct: (String, String, String, String, String) -> Unit,
) {
    var creatingNew by remember(item.itemId) { mutableStateOf(false) }
    var name by remember(item.itemId) { mutableStateOf(item.displayDescription) }
    var sector by remember(item.itemId) { mutableStateOf("") }
    var category by remember(item.itemId) { mutableStateOf("") }
    var subcategory by remember(item.itemId) { mutableStateOf("") }
    var unit by remember(item.itemId) {
        mutableStateOf(item.displayUnit.orEmpty().ifBlank { "UN" })
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = {
            Text(
                when {
                    creatingNew -> "Criar produto mestre"
                    item.productId != null -> "Editar vinculação"
                    else -> "Vincular produto"
                },
            )
        },
        text = {
            if (creatingNew) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 470.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            text = "O novo produto mestre será criado e este item será vinculado automaticamente.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = TextInputRules.capitalizeFirstLetter(it) },
                            label = { Text("Nome do produto *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = sector,
                            onValueChange = { sector = TextInputRules.capitalizeFirstLetter(it) },
                            label = { Text("Setor *") },
                            placeholder = { Text("Ex.: Alimentação") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = TextInputRules.capitalizeFirstLetter(it) },
                            label = { Text("Categoria *") },
                            placeholder = { Text("Ex.: Mercado") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = subcategory,
                            onValueChange = { subcategory = TextInputRules.capitalizeFirstLetter(it) },
                            label = { Text("Subcategoria (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unidade") },
                            placeholder = { Text("UN, KG, L...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 470.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            text = item.displayDescription,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }

                    item.productName?.let { current ->
                        item {
                            Text(
                                text = "Vínculo atual: $current",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    smartSuggestion?.let { suggestion ->
                        item {
                            SmartSuggestionCard(
                                suggestion = suggestion,
                                onUseExisting = onSelect,
                                onCreateNew = { newSuggestion ->
                                    onCreateProduct(
                                        newSuggestion.name,
                                        newSuggestion.sector,
                                        newSuggestion.category,
                                        newSuggestion.subcategory.orEmpty(),
                                        newSuggestion.unit,
                                    )
                                },
                            )
                        }
                    }

                    if (smartSuggestion !is SmartProductSuggestion.ExistingProduct) {
                        item {
                            OutlinedButton(
                                enabled = !saving,
                                onClick = { creatingNew = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Criar novo produto mestre")
                            }
                        }
                    }

                    if (products.isEmpty()) {
                        item {
                            Text(
                                text = "Ainda não há produtos mestres cadastrados. Crie um novo produto acima.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = if (item.productId == null) {
                                    "Selecione um produto mestre existente:"
                                } else {
                                    "Selecione outro produto para trocar a vinculação:"
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        items(
                            items = products,
                            key = { it.id },
                        ) { product ->
                            OutlinedButton(
                                enabled = !saving,
                                onClick = { onSelect(product) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(
                                        if (product.id == item.productId) {
                                            "${product.normalizedName} • atual"
                                        } else {
                                            product.normalizedName
                                        },
                                    )
                                    Text(
                                        text = listOfNotNull(
                                            product.sector,
                                            product.category,
                                            product.subcategory,
                                        ).joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }

                        if (smartSuggestion is SmartProductSuggestion.ExistingProduct) {
                            item {
                                TextButton(
                                    enabled = !saving,
                                    onClick = { creatingNew = true },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Não é esse produto? Criar outro")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (creatingNew) {
                Button(
                    enabled = !saving &&
                        name.isNotBlank() &&
                        sector.isNotBlank() &&
                        category.isNotBlank(),
                    onClick = {
                        onCreateProduct(
                            name,
                            sector,
                            category,
                            subcategory,
                            unit,
                        )
                    },
                ) {
                    Text(if (saving) "Criando..." else "Criar e vincular")
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    if (creatingNew) {
                        creatingNew = false
                    } else {
                        onDismiss()
                    }
                },
            ) {
                Text(if (creatingNew) "Voltar" else "Cancelar")
            }
        },
    )
}

@Composable
private fun SmartSuggestionCard(
    suggestion: SmartProductSuggestion,
    onUseExisting: (ProductEntity) -> Unit,
    onCreateNew: (SmartProductSuggestion.NewProduct) -> Unit,
) {
    val confidenceLabel = when {
        suggestion.confidence >= 90 -> "Alta confiança"
        suggestion.confidence >= 75 -> "Boa sugestão"
        else -> "Sugestão possível"
    }

    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Sugestão • $confidenceLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                when (suggestion) {
                    is SmartProductSuggestion.ExistingProduct -> {
                        Text(
                            text = suggestion.product.normalizedName,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = listOfNotNull(
                                suggestion.product.sector,
                                suggestion.product.category,
                                suggestion.product.subcategory,
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    is SmartProductSuggestion.NewProduct -> {
                        Text(
                            text = suggestion.name,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = listOfNotNull(
                                suggestion.sector,
                                suggestion.category,
                                suggestion.subcategory,
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            when (suggestion) {
                is SmartProductSuggestion.ExistingProduct -> {
                    TextButton(
                        onClick = { onUseExisting(suggestion.product) },
                    ) {
                        Text("Usar")
                    }
                }

                is SmartProductSuggestion.NewProduct -> {
                    TextButton(
                        onClick = { onCreateNew(suggestion) },
                    ) {
                        Text("Criar")
                    }
                }
            }
        }
    }
}
@Composable
private fun HistoryMessageCard(
    title: String,
    message: String,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message)
        }
    }
}

private fun formatHistoryMoney(value: BigDecimal): String = value
    .setScale(2, RoundingMode.HALF_UP)
    .toPlainString()
    .replace(".", ",")

private fun formatHistoryMoney(value: String): String =
    value.toBigDecimalOrNull()
        ?.let(::formatHistoryMoney)
        ?: value

private fun formatHistoryNumber(value: String): String =
    value.toBigDecimalOrNull()
        ?.stripTrailingZeros()
        ?.toPlainString()
        ?.replace(".", ",")
        ?: value

private fun formatAuditDateTime(timestamp: Long): String =
    DateTimeFormatter
        .ofPattern("dd/MM/yyyy 'às' HH:mm")
        .format(
            Instant
                .ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault()),
        )

private enum class HistorySortMode(val label: String) {
    DEFAULT("Mais recentes"),
    NAME_ASC("A–Z"),
    NAME_DESC("Z–A"),
    VALUE_ASC("Menor valor"),
    VALUE_DESC("Maior valor"),
}

private fun sortHistoryItems(
    items: List<HistoryItemRow>,
    mode: HistorySortMode,
): List<HistoryItemRow> = when (mode) {
    HistorySortMode.DEFAULT -> items
    HistorySortMode.NAME_ASC -> items.sortedBy {
        ProductNormalizer.searchKey(it.displayDescription)
    }
    HistorySortMode.NAME_DESC -> items.sortedByDescending {
        ProductNormalizer.searchKey(it.displayDescription)
    }
    HistorySortMode.VALUE_ASC -> items.sortedWith(
        compareBy<HistoryItemRow> { it.displayTotalAmount?.toBigDecimalOrNull() == null }
            .thenBy { it.displayTotalAmount?.toBigDecimalOrNull() ?: BigDecimal.ZERO },
    )
    HistorySortMode.VALUE_DESC -> items.sortedWith(
        compareBy<HistoryItemRow> { it.displayTotalAmount?.toBigDecimalOrNull() == null }
            .thenByDescending { it.displayTotalAmount?.toBigDecimalOrNull() ?: BigDecimal.ZERO },
    )
}
