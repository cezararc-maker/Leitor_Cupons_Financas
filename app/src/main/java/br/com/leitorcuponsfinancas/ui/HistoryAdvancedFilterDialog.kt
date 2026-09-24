package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.leitorcuponsfinancas.data.HistoryItemRow
import br.com.leitorcuponsfinancas.data.ProductEntity

@Composable
fun HistoryAdvancedFilterDialog(
    current: HistoryAdvancedFilter,
    rows: List<HistoryItemRow>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onApply: (HistoryAdvancedFilter) -> Unit,
    onClear: () -> Unit,
) {
    var productId by remember(current) { mutableStateOf(current.productId) }
    var category by remember(current) { mutableStateOf(current.category) }
    var merchant by remember(current) { mutableStateOf(current.merchant) }
    var sourceType by remember(current) { mutableStateOf(current.sourceType) }
    var minimum by remember(current) {
        mutableStateOf(current.minimumAmount?.let { CurrencyInputFormatter.format(it.toBigDecimal()) }.orEmpty())
    }
    var maximum by remember(current) {
        mutableStateOf(current.maximumAmount?.let { CurrencyInputFormatter.format(it.toBigDecimal()) }.orEmpty())
    }
    var onlyUnlinked by remember(current) { mutableStateOf(current.onlyUnlinked) }

    val categories = remember(rows) {
        rows.mapNotNull { it.category?.takeIf(String::isNotBlank) }
            .distinctBy(String::lowercase)
            .sortedBy(String::lowercase)
    }
    val merchants = remember(rows) {
        rows.mapNotNull { it.displayMerchantName?.takeIf(String::isNotBlank) }
            .distinctBy(String::lowercase)
            .sortedBy(String::lowercase)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtros do histórico") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = "Produto Mestre",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                item {
                    FilterChip(
                        selected = productId == null,
                        onClick = { productId = null },
                        label = { Text("Todos os produtos") },
                    )
                }
                items(products.take(30), key = { it.id }) { product ->
                    FilterChip(
                        selected = productId == product.id,
                        onClick = {
                            productId = if (productId == product.id) null else product.id
                        },
                        label = { Text(product.normalizedName) },
                    )
                }

                if (categories.isNotEmpty()) {
                    item {
                        Text(
                            text = "Categoria",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    item {
                        SuggestionTextField(
                            value = category.orEmpty(),
                            onValueChange = { category = it.ifBlank { null } },
                            suggestions = categories,
                            label = { Text("Categoria") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (merchants.isNotEmpty()) {
                    item {
                        SuggestionTextField(
                            value = merchant.orEmpty(),
                            onValueChange = { merchant = it.ifBlank { null } },
                            suggestions = merchants,
                            label = { Text("Estabelecimento") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item {
                    Text(
                        text = "Origem",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(
                            null to "Todas",
                            "NFCE" to "NFC-e",
                            "OCR" to "Foto / OCR",
                            "MANUAL" to "Manual",
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = sourceType == value,
                                onClick = { sourceType = value },
                                label = { Text(label) },
                            )
                        }
                    }
                }

                item {
                    CurrencyTextField(
                        value = minimum,
                        onValueChange = { minimum = it },
                        label = { Text("Valor mínimo por item") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    CurrencyTextField(
                        value = maximum,
                        onValueChange = { maximum = it },
                        label = { Text("Valor máximo por item") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Somente itens sem Produto Mestre")
                            Text(
                                text = "Ajuda a localizar lançamentos que ainda precisam de revisão.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = onlyUnlinked,
                            onCheckedChange = { onlyUnlinked = it },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(
                        HistoryAdvancedFilter(
                            productId = productId,
                            category = category?.trim()?.ifBlank { null },
                            merchant = merchant?.trim()?.ifBlank { null },
                            sourceType = sourceType,
                            minimumAmount = CurrencyInputFormatter.parse(minimum)?.toDouble(),
                            maximumAmount = CurrencyInputFormatter.parse(maximum)?.toDouble(),
                            onlyUnlinked = onlyUnlinked,
                        ),
                    )
                },
            ) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            androidx.compose.foundation.layout.Row {
                TextButton(onClick = onClear) {
                    Text("Limpar")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        },
    )
}
