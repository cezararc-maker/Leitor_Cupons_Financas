package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.leitorcuponsfinancas.data.ProductEntity

@Composable
fun ProductScreen(
    products: List<ProductEntity>,
    learnedDescriptions: Map<Long, List<String>>,
    onSave: (
        ProductEntity?,
        String,
        String,
        String,
        String,
        String,
        String,
        String,
    ) -> Unit,
    onDeactivate: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<ProductEntity?>(null) }
    var showForm by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showForm = true
                },
            ) {
                Text("+")
            }
        },
    ) { padding ->
        if (products.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Nenhum produto cadastrado",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Use o botão + para cadastrar manualmente o primeiro produto.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(products, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        learnedDescriptions = learnedDescriptions[product.id].orEmpty(),
                        onClick = {
                            editing = product
                            showForm = true
                        },
                    )
                }
            }
        }
    }

    if (showForm) {
        ProductFormDialog(
            product = editing,
            learnedDescriptions = editing?.let { learnedDescriptions[it.id] }.orEmpty(),
            onDismiss = {
                showForm = false
                editing = null
            },
            onSave = { name, fiscalDescription, sector, category, subcategory, unit, notes ->
                onSave(
                    editing,
                    name,
                    fiscalDescription,
                    sector,
                    category,
                    subcategory,
                    unit,
                    notes,
                )
                showForm = false
                editing = null
            },
            onDeactivate = editing?.let { product ->
                {
                    onDeactivate(product)
                    showForm = false
                    editing = null
                }
            },
        )
    }
}

@Composable
private fun ProductCard(
    product: ProductEntity,
    learnedDescriptions: List<String>,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = product.normalizedName,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = listOfNotNull(
                    product.sector,
                    product.category,
                    product.subcategory,
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Unidade: ${product.unit}",
                style = MaterialTheme.typography.bodySmall,
            )

            product.fiscalDescription?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Descrição principal na NFC-e: $it",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (learnedDescriptions.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Descrições aprendidas:",
                    style = MaterialTheme.typography.bodySmall,
                )
                learnedDescriptions.take(4).forEach { description ->
                    Text(
                        text = "• $description",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (learnedDescriptions.size > 4) {
                    Text(
                        text = "+${learnedDescriptions.size - 4} outra(s)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductFormDialog(
    product: ProductEntity?,
    learnedDescriptions: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        String,
        String,
        String,
        String,
        String,
    ) -> Unit,
    onDeactivate: (() -> Unit)?,
) {
    var name by remember(product?.id) { mutableStateOf(product?.normalizedName.orEmpty()) }
    var fiscalDescription by remember(product?.id) {
        mutableStateOf(product?.fiscalDescription.orEmpty())
    }
    var sector by remember(product?.id) { mutableStateOf(product?.sector.orEmpty()) }
    var category by remember(product?.id) { mutableStateOf(product?.category.orEmpty()) }
    var subcategory by remember(product?.id) { mutableStateOf(product?.subcategory.orEmpty()) }
    var unit by remember(product?.id) { mutableStateOf(product?.unit ?: "UN") }
    var customUnitMode by remember(product?.id) {
        mutableStateOf(
            product?.unit
                ?.let { saved ->
                    ManualUnitType.entries.none {
                        it != ManualUnitType.OTHER && it.code == saved.uppercase()
                    }
                }
                ?: false,
        )
    }
    var showUnitPicker by remember(product?.id) { mutableStateOf(false) }
    var notes by remember(product?.id) { mutableStateOf(product?.notes.orEmpty()) }

    val valid = name.isNotBlank() &&
        sector.isNotBlank() &&
        category.isNotBlank() &&
        unit.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (product == null) "Cadastrar produto" else "Editar produto")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome do produto *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = fiscalDescription,
                        onValueChange = { fiscalDescription = it },
                        label = { Text("Descrição na NFC-e (opcional)") },
                        placeholder = { Text("Ex.: ARROZ TIO LAUTERIOT1") },
                        supportingText = { Text("Informe exatamente como o produto aparece na nota, ou deixe em branco e ensine depois pelo Histórico.") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = sector,
                        onValueChange = { sector = it },
                        label = { Text("Setor *") },
                        placeholder = { Text("Ex.: Alimentação") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Categoria *") },
                        placeholder = { Text("Ex.: Mercado") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = subcategory,
                        onValueChange = { subcategory = it },
                        label = { Text("Subcategoria") },
                        placeholder = { Text("Ex.: Mercearia") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { showUnitPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val selected = ManualUnitType.entries.firstOrNull {
                            it != ManualUnitType.OTHER && it.code == unit.uppercase()
                        }
                        Text(
                            text = selected?.let { "Unidade: ${it.label} (${it.code})" }
                                ?: "Unidade: ${unit.ifBlank { "Escolher" }}",
                        )
                    }
                }

                if (customUnitMode) {
                    item {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it.uppercase() },
                            label = { Text("Unidade personalizada *") },
                            placeholder = { Text("Ex.: CX, DZ, BDJ") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (learnedDescriptions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Descrições aprendidas automaticamente:",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    learnedDescriptions.forEach { learned ->
                        item {
                            Text(
                                text = "• $learned",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Observações") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                }
                if (onDeactivate != null) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onDeactivate,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Desativar produto")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        name,
                        fiscalDescription,
                        sector,
                        category,
                        subcategory,
                        unit,
                        notes,
                    )
                },
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
    if (showUnitPicker) {
        AlertDialog(
            onDismissRequest = { showUnitPicker = false },
            title = { Text("Escolher tipo de unidade") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ManualUnitType.entries) { option ->
                        OutlinedButton(
                            onClick = {
                                if (option == ManualUnitType.OTHER) {
                                    customUnitMode = true
                                    if (
                                        ManualUnitType.entries.any {
                                            it != ManualUnitType.OTHER &&
                                                it.code == unit.uppercase()
                                        }
                                    ) {
                                        unit = ""
                                    }
                                } else {
                                    customUnitMode = false
                                    unit = option.code
                                }
                                showUnitPicker = false
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
                TextButton(onClick = { showUnitPicker = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}
