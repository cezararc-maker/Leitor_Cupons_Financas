package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import br.com.leitorcuponsfinancas.data.HistoryItemRow
import br.com.leitorcuponsfinancas.data.ProductEntity
import br.com.leitorcuponsfinancas.data.TaxonomyLevel
import br.com.leitorcuponsfinancas.data.TaxonomyNodeEntity
import br.com.leitorcuponsfinancas.data.TaxonomyProductLinkEntity
import br.com.leitorcuponsfinancas.domain.ProductNormalizer
import br.com.leitorcuponsfinancas.domain.SmartProductSuggestion

@Composable
fun TaxonomyProductLinkDialog(
    item: HistoryItemRow,
    products: List<ProductEntity>,
    nodes: List<TaxonomyNodeEntity>,
    productLinks: List<TaxonomyProductLinkEntity>,
    smartSuggestion: SmartProductSuggestion?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSelect: (ProductEntity, Long?) -> Unit,
    onCreate: (String, Long, String) -> Unit,
) {
    val segmentIds = remember(nodes) {
        nodes.filter { it.level == TaxonomyLevel.SEGMENT.code }.map { it.id }.toSet()
    }
    val lockedSegmentId = item.merchantSegmentNodeId
        ?.takeIf { it in segmentIds }

    var selectedNodeId by remember(item.itemId, lockedSegmentId, nodes.size) {
        mutableStateOf(lockedSegmentId)
    }
    var query by remember(item.itemId) { mutableStateOf("") }
    var creating by remember(item.itemId) { mutableStateOf(false) }
    var newName by remember(item.itemId) { mutableStateOf(item.displayDescription) }
    var unit by remember(item.itemId) {
        mutableStateOf(item.displayUnit.orEmpty().ifBlank { "UN" })
    }

    val currentNode = nodes.firstOrNull { it.id == selectedNodeId }
    val path = remember(selectedNodeId, nodes) {
        selectedNodeId?.let { taxonomyPath(nodes, it) }.orEmpty()
    }
    val options = remember(selectedNodeId, lockedSegmentId, nodes) {
        when {
            selectedNodeId == null && lockedSegmentId != null ->
                nodes.filter { it.id == lockedSegmentId && it.active }

            selectedNodeId == null ->
                nodes.filter { it.level == TaxonomyLevel.SEGMENT.code && it.active }

            else ->
                nodes.filter { it.parentId == selectedNodeId && it.active }
        }.sortedBy { it.name.lowercase() }
    }

    val linkedIds = remember(selectedNodeId, productLinks) {
        val id = selectedNodeId
        if (id == null) emptySet()
        else productLinks
            .filter { it.taxonomyNodeId == id }
            .map { it.productId }
            .toSet()
    }

    val classifiedProducts = remember(selectedNodeId, products, linkedIds, currentNode) {
        if (currentNode == null || currentNode.level == TaxonomyLevel.SEGMENT.code) {
            emptyList()
        } else {
            products.filter { product ->
                product.id in linkedIds ||
                    when (currentNode.level) {
                        TaxonomyLevel.DEPARTMENT.code ->
                            ProductNormalizer.searchKey(product.sector) ==
                                ProductNormalizer.searchKey(currentNode.name)
                        TaxonomyLevel.CATEGORY.code ->
                            ProductNormalizer.searchKey(product.category) ==
                                ProductNormalizer.searchKey(currentNode.name)
                        TaxonomyLevel.SUBCATEGORY.code ->
                            ProductNormalizer.searchKey(product.subcategory.orEmpty()) ==
                                ProductNormalizer.searchKey(currentNode.name)
                        else -> false
                    }
            }.distinctBy { it.id }.sortedBy { it.normalizedName.lowercase() }
        }
    }

    val searchedProducts = remember(query, products) {
        val clean = ProductNormalizer.searchKey(query)
        if (clean.isBlank()) emptyList()
        else products
            .filter {
                ProductNormalizer.searchKey(it.normalizedName).contains(clean)
            }
            .take(12)
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (item.productId == null) "Vincular produto" else "Editar vinculação")
                Text(
                    text = item.displayDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 540.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (path.isNotEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "Classificação",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = path.joinToString(" › ") { it.name },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (
                                    currentNode?.parentId != null ||
                                    lockedSegmentId == null
                                ) {
                                    TextButton(
                                        onClick = {
                                            selectedNodeId = currentNode?.parentId
                                            creating = false
                                        },
                                    ) {
                                        Text(
                                            if (currentNode?.parentId == null) {
                                                "Trocar segmento"
                                            } else {
                                                "Voltar um nível"
                                            },
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Segmento fixado pelo estabelecimento. Para trocar, edite o estabelecimento.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "Escolha o segmento do estabelecimento",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "As opções seguintes serão limitadas ao segmento escolhido.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (
                    currentNode != null &&
                    currentNode.level != TaxonomyLevel.SEGMENT.code
                ) {
                    smartSuggestion?.let { suggestion ->
                        item {
                            SmartSuggestionCompact(
                                suggestion = suggestion,
                                onUse = { product ->
                                    onSelect(product, currentNode.id)
                                },
                            )
                        }
                    }
                }

                if (options.isNotEmpty()) {
                    item {
                        val nextLabel = when {
                            selectedNodeId == null -> "Segmentos"
                            currentNode == null -> "Opções"
                            else -> TaxonomyLevel.fromCode(currentNode.level)
                                ?.nextOrNull()
                                ?.label
                                ?.let { "$it disponíveis" }
                                ?: "Opções"
                        }
                        Text(
                            text = nextLabel,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }

                    items(options, key = { it.id }) { node ->
                        OutlinedButton(
                            onClick = {
                                selectedNodeId = node.id
                                creating = false
                                query = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(node.name)
                        }
                    }
                }

                if (currentNode != null && currentNode.level != TaxonomyLevel.SEGMENT.code) {
                    item {
                        Text(
                            text = "Produtos Mestres nesta classificação",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }

                    if (classifiedProducts.isEmpty()) {
                        item {
                            Text(
                                text = "Nenhum Produto Mestre foi classificado aqui ainda. Você pode buscar um existente ou criar um novo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(classifiedProducts, key = { "classified_${it.id}" }) { product ->
                            Button(
                                enabled = !saving,
                                onClick = { onSelect(product, currentNode.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    if (product.id == item.productId) {
                                        "${product.normalizedName} • atual"
                                    } else {
                                        product.normalizedName
                                    },
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Buscar em todos os Produtos Mestres") },
                            placeholder = { Text("Ex.: Banana") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (searchedProducts.isNotEmpty()) {
                        items(searchedProducts, key = { "search_${it.id}" }) { product ->
                            OutlinedButton(
                                enabled = !saving,
                                onClick = { onSelect(product, currentNode.id) },
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

                    item {
                        TextButton(
                            enabled = !saving,
                            onClick = { creating = !creating },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (creating) "Cancelar novo Produto Mestre" else "Criar Produto Mestre nesta classificação")
                        }
                    }

                    if (creating) {
                        item {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Produto raiz *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = unit,
                                onValueChange = { unit = it.uppercase() },
                                label = { Text("Unidade") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        item {
                            Button(
                                enabled = !saving && newName.isNotBlank(),
                                onClick = {
                                    onCreate(
                                        newName,
                                        currentNode.id,
                                        unit,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (saving) "Criando..." else "Criar e vincular")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                enabled = !saving,
                onClick = onDismiss,
            ) {
                Text("Fechar")
            }
        },
    )
}

@Composable
private fun SmartSuggestionCompact(
    suggestion: SmartProductSuggestion,
    onUse: (ProductEntity) -> Unit,
) {
    if (suggestion !is SmartProductSuggestion.ExistingProduct) return

    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Sugestão do app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = suggestion.product.normalizedName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${suggestion.confidence}% de compatibilidade",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = { onUse(suggestion.product) }) {
                Text("Usar")
            }
        }
    }
}

private fun taxonomyPath(
    nodes: List<TaxonomyNodeEntity>,
    nodeId: Long,
): List<TaxonomyNodeEntity> {
    val byId = nodes.associateBy { it.id }
    val result = mutableListOf<TaxonomyNodeEntity>()
    var current = byId[nodeId]

    while (current != null) {
        result += current
        current = current.parentId?.let(byId::get)
    }

    return result.reversed()
}
