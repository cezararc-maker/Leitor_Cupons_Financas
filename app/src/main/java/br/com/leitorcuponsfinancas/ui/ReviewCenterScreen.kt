package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.leitorcuponsfinancas.data.HistoryItemRow
import br.com.leitorcuponsfinancas.data.ProductEntity
import br.com.leitorcuponsfinancas.domain.ProductSuggestionEngine
import br.com.leitorcuponsfinancas.domain.SmartProductSuggestion

@Composable
fun ReviewCenterScreen(
    modifier: Modifier = Modifier,
    reviewViewModel: ReviewViewModel = viewModel(),
) {
    val reviewItems by reviewViewModel.items.collectAsStateWithLifecycle()
    val products by reviewViewModel.products.collectAsStateWithLifecycle()
    val learnedLinks by reviewViewModel.learnedLinks.collectAsStateWithLifecycle()
    val actionState by reviewViewModel.actionState.collectAsStateWithLifecycle()

    var chooseFor by remember { mutableStateOf<HistoryItemRow?>(null) }
    var createFor by remember { mutableStateOf<HistoryItemRow?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHero(
                title = "Central de revisão",
                subtitle = if (reviewItems.isEmpty()) {
                    "Tudo classificado. Nenhum item precisa de revisão."
                } else {
                    "${reviewItems.size} item(ns) ainda não possuem Produto Mestre."
                },
                icon = Icons.Default.FactCheck,
            )
        }

        actionState.message?.let { message ->
            item {
                AnimatedInfoCard(
                    visible = true,
                    title = "Aprendizado salvo",
                    message = message,
                    accent = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        actionState.error?.let { error ->
            item {
                AnimatedInfoCard(
                    visible = true,
                    title = "Não foi possível concluir",
                    message = error,
                    accent = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (reviewItems.isEmpty()) {
            item {
                FlowSectionCard(
                    title = "Base organizada",
                    subtitle = "Novos itens sem classificação aparecerão aqui automaticamente.",
                    icon = Icons.Default.Inventory2,
                ) {
                    Text(
                        "A Central de revisão concentra apenas o que precisa da sua atenção, sem interromper a importação das compras.",
                    )
                }
            }
        } else {
            items(reviewItems, key = { it.itemId }) { item ->
                val suggestion = remember(
                    item.itemId,
                    products,
                    learnedLinks,
                ) {
                    ProductSuggestionEngine.suggest(
                        description = item.displayDescription,
                        unit = item.displayUnit,
                        products = products,
                        learnedLinks = learnedLinks,
                    )
                }

                ReviewItemCard(
                    item = item,
                    suggestion = suggestion,
                    saving = actionState.saving,
                    onAcceptSuggestion = { product ->
                        reviewViewModel.clearMessage()
                        reviewViewModel.link(item, product)
                    },
                    onChoose = {
                        reviewViewModel.clearMessage()
                        chooseFor = item
                    },
                    onCreate = {
                        reviewViewModel.clearMessage()
                        createFor = item
                    },
                )
            }
        }
    }

    chooseFor?.let { item ->
        ProductChooserDialog(
            products = products,
            item = item,
            onDismiss = { chooseFor = null },
            onSelect = { product ->
                chooseFor = null
                reviewViewModel.link(item, product)
            },
        )
    }

    createFor?.let { item ->
        ReviewCreateProductDialog(
            item = item,
            products = products,
            onDismiss = { createFor = null },
            onCreate = { name, sector, category, subcategory, unit ->
                createFor = null
                reviewViewModel.createAndLink(
                    item = item,
                    name = name,
                    sector = sector,
                    category = category,
                    subcategory = subcategory,
                    unit = unit,
                )
            },
        )
    }
}

@Composable
private fun ReviewItemCard(
    item: HistoryItemRow,
    suggestion: SmartProductSuggestion?,
    saving: Boolean,
    onAcceptSuggestion: (ProductEntity) -> Unit,
    onChoose: () -> Unit,
    onCreate: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.displayDescription,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = listOfNotNull(
                    item.displayMerchantName,
                    item.issuedDate,
                    item.displayUnitPrice?.let {
                        "R$ ${CurrencyInputFormatter.fromStoredDecimal(it)}"
                    },
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (suggestion) {
                is SmartProductSuggestion.ExistingProduct -> {
                    AnimatedInfoCard(
                        visible = true,
                        title = "Possível correspondência",
                        message = "${suggestion.product.normalizedName} • ${suggestion.confidence}% de compatibilidade",
                    )
                    Button(
                        enabled = !saving,
                        onClick = { onAcceptSuggestion(suggestion.product) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Usar ${suggestion.product.normalizedName}")
                    }
                }

                is SmartProductSuggestion.NewProduct -> {
                    Text(
                        text = "Sugestão de novo Produto Mestre: ${suggestion.name}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                null -> Unit
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    enabled = !saving,
                    onClick = onChoose,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Escolher existente")
                }
                OutlinedButton(
                    enabled = !saving,
                    onClick = onCreate,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Criar novo")
                }
            }
        }
    }
}

@Composable
private fun ProductChooserDialog(
    products: List<ProductEntity>,
    item: HistoryItemRow,
    onDismiss: () -> Unit,
    onSelect: (ProductEntity) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(products, query) {
        val clean = query.trim()
        if (clean.isBlank()) products
        else products.filter {
            it.normalizedName.contains(clean, ignoreCase = true) ||
                it.category.contains(clean, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vincular Produto Mestre") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = item.displayDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar Produto Mestre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filtered.take(12), key = { it.id }) { product ->
                        FilterChip(
                            selected = false,
                            onClick = { onSelect(product) },
                            label = {
                                Text(
                                    "${product.normalizedName} • ${product.category}",
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
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
private fun ReviewCreateProductDialog(
    item: HistoryItemRow,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String) -> Unit,
) {
    val suggested = remember(item.itemId, products) {
        ProductSuggestionEngine.suggest(
            description = item.displayDescription,
            unit = item.displayUnit,
            products = products,
            learnedLinks = emptyList(),
        )
    }

    var name by remember {
        mutableStateOf(
            (suggested as? SmartProductSuggestion.NewProduct)?.name.orEmpty(),
        )
    }
    var sector by remember {
        mutableStateOf(
            (suggested as? SmartProductSuggestion.NewProduct)?.sector.orEmpty(),
        )
    }
    var category by remember {
        mutableStateOf(
            (suggested as? SmartProductSuggestion.NewProduct)?.category.orEmpty(),
        )
    }
    var subcategory by remember {
        mutableStateOf(
            (suggested as? SmartProductSuggestion.NewProduct)?.subcategory.orEmpty(),
        )
    }
    var unit by remember { mutableStateOf(item.displayUnit ?: "UN") }

    val sectorSuggestions = products.map { it.sector }
    val categorySuggestions = products
        .filter { sector.isBlank() || it.sector.equals(sector, ignoreCase = true) }
        .map { it.category }
    val subcategorySuggestions = products
        .filter { category.isBlank() || it.category.equals(category, ignoreCase = true) }
        .mapNotNull { it.subcategory }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Criar Produto Mestre raiz") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(
                    text = "Item fiscal: ${item.displayDescription}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SuggestionTextField(
                    value = name,
                    onValueChange = { name = it },
                    suggestions = products.map { it.normalizedName },
                    label = { Text("Produto raiz *") },
                    modifier = Modifier.fillMaxWidth(),
                )
                SuggestionTextField(
                    value = sector,
                    onValueChange = { sector = it },
                    suggestions = sectorSuggestions,
                    label = { Text("Setor *") },
                    modifier = Modifier.fillMaxWidth(),
                )
                SuggestionTextField(
                    value = category,
                    onValueChange = { category = it },
                    suggestions = categorySuggestions,
                    label = { Text("Categoria *") },
                    modifier = Modifier.fillMaxWidth(),
                )
                SuggestionTextField(
                    value = subcategory,
                    onValueChange = { subcategory = it },
                    suggestions = subcategorySuggestions,
                    label = { Text("Subcategoria") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it.uppercase() },
                    label = { Text("Unidade *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && sector.isNotBlank() && category.isNotBlank(),
                onClick = {
                    onCreate(name, sector, category, subcategory, unit)
                },
            ) {
                Text("Criar e vincular")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
