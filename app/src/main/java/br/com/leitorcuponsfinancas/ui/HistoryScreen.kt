package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = viewModel(),
) {
    val periodType by historyViewModel.periodType.collectAsStateWithLifecycle()
    val dateRange by historyViewModel.dateRange.collectAsStateWithLifecycle()
    val historyItems by historyViewModel.items.collectAsStateWithLifecycle()
    val filteredItems by historyViewModel.filteredItems.collectAsStateWithLifecycle()
    val searchQuery by historyViewModel.searchQuery.collectAsStateWithLifecycle()
    val searchMode by historyViewModel.searchMode.collectAsStateWithLifecycle()
    val products by historyViewModel.products.collectAsStateWithLifecycle()
    val linkState by historyViewModel.linkState.collectAsStateWithLifecycle()

    var linkingItem by remember { mutableStateOf<HistoryItemRow?>(null) }
    var reviewingItemId by rememberSaveable { mutableStateOf<Long?>(null) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }

    val unrecognizedItems = filteredItems.filter { it.productId == null }

    val total = filteredItems
        .mapNotNull { it.totalAmount?.toBigDecimalOrNull() }
        .fold(BigDecimal.ZERO, BigDecimal::add)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Button(onClick = onBack) {
                Text("Voltar")
            }
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
                                onValueChange = historyViewModel::updateSearchQuery,
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
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Resumo do período",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (searchQuery.isBlank()) {
                        Text("Itens: ${historyItems.size}")
                    } else {
                        Text("Resultados: ${filteredItems.size} de ${historyItems.size} itens")
                    }
                    Text("Total exibido: R$ ${formatHistoryMoney(total)}")
                    Text(
                        text = "Não vinculados exibidos: ${unrecognizedItems.size}",
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
            items = filteredItems,
            key = { it.itemId },
        ) { item ->
            HistoryItemCard(
                item = item,
                onLink = {
                    historyViewModel.clearLinkMessage()
                    linkingItem = item
                },
            )
        }
    }

    linkingItem?.let { item ->
        ProductLinkDialog(
            item = item,
            products = products,
            onDismiss = { linkingItem = null },
            onSelect = { product ->
                historyViewModel.linkItem(item, product)
                linkingItem = null
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

            UnrecognizedReviewDialog(
                item = currentItem,
                position = reviewIndex + 1,
                total = unrecognizedItems.size,
                products = products,
                onDismiss = { reviewingItemId = null },
                onSkip = { reviewingItemId = nextItemId },
                onSelect = { product ->
                    historyViewModel.linkItem(currentItem, product)
                    reviewingItemId = nextItemId
                },
            )
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: HistoryItemRow,
    onLink: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val header = listOfNotNull(
                item.issuedAt?.take(10),
                item.merchantName,
            ).joinToString(" • ")

            if (header.isNotBlank()) {
                Text(
                    text = header,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Text(
                text = item.fiscalDescription,
                style = MaterialTheme.typography.titleMedium,
            )

            item.itemCode?.let {
                Text(
                    text = "Código no estabelecimento: $it",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Text(
                text = listOfNotNull(
                    item.quantity?.let { "Qtd.: ${formatHistoryNumber(it)}" },
                    item.unit?.let { "UN: $it" },
                    item.unitPrice?.let { "Unit.: R$ ${formatHistoryMoney(it)}" },
                    item.totalAmount?.let { "Total: R$ ${formatHistoryMoney(it)}" },
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodyMedium,
            )

            if (item.productId != null) {
                Text(
                    text = buildString {
                        append("Produto: ")
                        append(item.productName ?: "Produto cadastrado")
                        item.sector?.let { append(" • $it") }
                        item.category?.let { append(" • $it") }
                        item.subcategory?.let { append(" • $it") }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = "Produto ainda não reconhecido.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(
                    onClick = onLink,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Vincular a produto")
                }
            }
        }
    }
}

@Composable
private fun UnrecognizedReviewDialog(
    item: HistoryItemRow,
    position: Int,
    total: Int,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    onSelect: (ProductEntity) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Revisar não reconhecidos")
                Text(
                    text = "$position de $total",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = item.fiscalDescription,
                    style = MaterialTheme.typography.titleMedium,
                )

                item.itemCode?.let {
                    Text(
                        text = "Código no estabelecimento: $it",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                item.merchantName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Text(
                    text = "Escolha o produto mestre. O vínculo também será reaplicado aos itens equivalentes deste estabelecimento.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (products.isEmpty()) {
                    Text("Nenhum produto mestre cadastrado.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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
    onDismiss: () -> Unit,
    onSelect: (ProductEntity) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Vincular produto")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = item.fiscalDescription,
                    style = MaterialTheme.typography.titleSmall,
                )

                if (products.isEmpty()) {
                    Text(
                        "Nenhum produto cadastrado. Volte à tela de produtos e cadastre um produto primeiro.",
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
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
