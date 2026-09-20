package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.leitorcuponsfinancas.data.ProductEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ManualEntryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManualEntryViewModel = viewModel(),
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    var merchantName by remember { mutableStateOf("") }
    var dateText by remember {
        mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
    }
    var description by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitType by remember { mutableStateOf(ManualUnitType.UNIT) }
    var customUnit by remember { mutableStateOf("") }
    var unitPrice by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showProducts by remember { mutableStateOf(false) }
    var showUnits by remember { mutableStateOf(false) }

    val calculatedTotal = remember(quantity, unitPrice) {
        ManualEntryCalculator.calculateTotal(
            quantity = quantity,
            unitPrice = unitPrice,
        )
    }

    val effectiveUnit = if (unitType == ManualUnitType.OTHER) {
        customUnit.trim().uppercase()
    } else {
        unitType.code
    }

    val priceLabel = if (unitType == ManualUnitType.OTHER && customUnit.isNotBlank()) {
        "Valor por ${customUnit.trim().lowercase()}"
    } else {
        unitType.priceLabel
    }

    val canSave = !saveState.saving &&
        description.isNotBlank() &&
        dateText.isNotBlank() &&
        quantity.isNotBlank() &&
        unitPrice.isNotBlank() &&
        calculatedTotal != null &&
        effectiveUnit.isNotBlank()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Button(onClick = onBack) {
                Text("Voltar")
            }
        }

        item {
            Text(
                text = "Lançamento manual",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Use quando não houver NFC-e ou cupom fiscal. Nome do item, unidade, quantidade e preço são obrigatórios. O total é calculado automaticamente.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    viewModel.clearMessage()
                },
                label = { Text("Nome do item *") },
                placeholder = { Text("Ex.: Pão francês") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item {
            OutlinedButton(
                onClick = { showProducts = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    selectedProduct?.let {
                        "Produto mestre: ${it.normalizedName}"
                    } ?: "Vincular a produto mestre (opcional)",
                )
            }

            if (selectedProduct != null) {
                TextButton(
                    onClick = {
                        selectedProduct = null
                        viewModel.clearMessage()
                    },
                ) {
                    Text("Remover vínculo")
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { showUnits = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (unitType == ManualUnitType.OTHER && customUnit.isNotBlank()) {
                        "Unidade: ${customUnit.trim().uppercase()}"
                    } else {
                        "Unidade: ${unitType.label}"
                    },
                )
            }
        }

        if (unitType == ManualUnitType.OTHER) {
            item {
                OutlinedTextField(
                    value = customUnit,
                    onValueChange = {
                        customUnit = it
                        viewModel.clearMessage()
                    },
                    label = { Text("Tipo de unidade *") },
                    placeholder = { Text("Ex.: Caixa, dúzia, bandeja...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }

        item {
            OutlinedTextField(
                value = quantity,
                onValueChange = {
                    quantity = it
                    viewModel.clearMessage()
                },
                label = { Text("${unitType.quantityLabel} *") },
                placeholder = {
                    Text(
                        when (unitType) {
                            ManualUnitType.KILOGRAM -> "Ex.: 0,750"
                            ManualUnitType.LITER -> "Ex.: 1,5"
                            else -> "Ex.: 10"
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item {
            OutlinedTextField(
                value = unitPrice,
                onValueChange = {
                    unitPrice = it
                    viewModel.clearMessage()
                },
                label = { Text("$priceLabel *") },
                placeholder = { Text("Ex.: 1,50") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Valor total",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = calculatedTotal?.let {
                            "R$ ${ManualEntryCalculator.formatMoney(it)}"
                        } ?: "Preencha quantidade e valor para calcular.",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "Calculado automaticamente: quantidade × preço.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = merchantName,
                onValueChange = {
                    merchantName = it
                    viewModel.clearMessage()
                },
                label = { Text("Estabelecimento (opcional)") },
                placeholder = { Text("Ex.: Padaria do bairro") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item {
            OutlinedTextField(
                value = dateText,
                onValueChange = {
                    dateText = it
                    viewModel.clearMessage()
                },
                label = { Text("Data *") },
                placeholder = { Text("DD/MM/AAAA") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        saveState.message?.let { message ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = "Lançamento salvo",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(message)
                    }
                }
            }
        }

        saveState.error?.let { error ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = "Não foi possível salvar",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(error)
                    }
                }
            }
        }

        item {
            Button(
                enabled = canSave,
                onClick = {
                    viewModel.save(
                        merchantName = merchantName,
                        dateText = dateText,
                        description = description,
                        quantity = quantity,
                        unit = effectiveUnit,
                        unitPrice = unitPrice,
                        productId = selectedProduct?.id,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (saveState.saving) {
                        "Salvando..."
                    } else {
                        "Salvar lançamento manual"
                    },
                )
            }
        }
    }

    if (showUnits) {
        AlertDialog(
            onDismissRequest = { showUnits = false },
            title = { Text("Escolher unidade") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ManualUnitType.entries) { option ->
                        OutlinedButton(
                            onClick = {
                                unitType = option
                                if (option != ManualUnitType.OTHER) {
                                    customUnit = ""
                                }
                                showUnits = false
                                viewModel.clearMessage()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${option.label} (${option.code})")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showUnits = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (showProducts) {
        AlertDialog(
            onDismissRequest = { showProducts = false },
            title = { Text("Escolher produto mestre") },
            text = {
                if (products.isEmpty()) {
                    Text("Nenhum produto mestre cadastrado.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(products, key = { it.id }) { product ->
                            OutlinedButton(
                                onClick = {
                                    selectedProduct = product

                                    if (description.isBlank()) {
                                        description = product.normalizedName
                                    }

                                    val productUnit = product.unit.trim().uppercase()
                                    val recognizedUnit = ManualUnitType.entries.firstOrNull {
                                        it.code == productUnit
                                    }

                                    if (recognizedUnit != null) {
                                        unitType = recognizedUnit
                                        customUnit = ""
                                    } else if (productUnit.isNotBlank()) {
                                        unitType = ManualUnitType.OTHER
                                        customUnit = productUnit
                                    }

                                    showProducts = false
                                    viewModel.clearMessage()
                                },
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
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showProducts = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}
