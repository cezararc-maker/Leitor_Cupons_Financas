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
import br.com.leitorcuponsfinancas.data.MerchantProductLinkEntity
import br.com.leitorcuponsfinancas.data.ProductEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProductScreen(
    products: List<ProductEntity>,
    learnedLinks: Map<Long, List<MerchantProductLinkEntity>>,
    onSave: (
        ProductEntity?,
        String,
        String,
        String,
        String,
        String,
        String,
        String,
    ) -> String?,
    onDeactivate: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<ProductEntity?>(null) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var saveError by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    saveError = null
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
                        onClick = {
                            editing = product
                            saveError = null
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
            allProducts = products,
            learnedLinks = editing?.let { learnedLinks[it.id] }.orEmpty(),
            saveError = saveError,
            onDismiss = {
                showForm = false
                editing = null
                saveError = null
            },
            onSave = { name, fiscalDescription, sector, category, subcategory, unit, notes ->
                val error = onSave(
                    editing,
                    name,
                    fiscalDescription,
                    sector,
                    category,
                    subcategory,
                    unit,
                    notes,
                )

                if (error == null) {
                    showForm = false
                    editing = null
                    saveError = null
                } else {
                    saveError = error
                }
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

        }
    }
}

@Composable
private fun ProductFormDialog(
    product: ProductEntity?,
    allProducts: List<ProductEntity>,
    learnedLinks: List<MerchantProductLinkEntity>,
    saveError: String?,
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
    var showLearnedLinks by remember(product?.id) { mutableStateOf(false) }
    var notes by remember(product?.id) { mutableStateOf(product?.notes.orEmpty()) }

    val otherProducts = remember(allProducts, product?.id) {
        allProducts.filter { it.id != product?.id }
    }
    val nameSuggestions = remember(otherProducts) {
        otherProducts.map { it.normalizedName }
    }
    val sectorSuggestions = remember(otherProducts) {
        otherProducts.map { it.sector }
    }
    val categorySuggestions = remember(otherProducts, sector) {
        val sameSector = otherProducts.filter { it.sector.equals(sector, ignoreCase = true) }
        (if (sameSector.isNotEmpty()) sameSector else otherProducts).map { it.category }
    }
    val subcategorySuggestions = remember(otherProducts, category) {
        val sameCategory = otherProducts.filter { it.category.equals(category, ignoreCase = true) }
        (if (sameCategory.isNotEmpty()) sameCategory else otherProducts)
            .mapNotNull { it.subcategory }
    }

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
                    SuggestionTextField(
                        value = name,
                        onValueChange = { name = it },
                        suggestions = nameSuggestions,
                        label = { Text("Nome do produto *") },
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
                    SuggestionTextField(
                        value = sector,
                        onValueChange = { sector = it },
                        suggestions = sectorSuggestions,
                        label = { Text("Setor *") },
                        placeholder = { Text("Ex.: Alimentação") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    SuggestionTextField(
                        value = category,
                        onValueChange = { category = it },
                        suggestions = categorySuggestions,
                        label = { Text("Categoria *") },
                        placeholder = { Text("Ex.: Mercado") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    SuggestionTextField(
                        value = subcategory,
                        onValueChange = { subcategory = it },
                        suggestions = subcategorySuggestions,
                        label = { Text("Subcategoria") },
                        placeholder = { Text("Ex.: Mercearia") },
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

                saveError?.let { error ->
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }

                if (product != null) {
                    item {
                        OutlinedButton(
                            onClick = { showLearnedLinks = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Vínculos e aliases aprendidos (${learnedLinks.size})")
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
    if (showLearnedLinks) {
        LearnedLinksDialog(
            productName = product?.normalizedName.orEmpty(),
            links = learnedLinks,
            onDismiss = { showLearnedLinks = false },
        )
    }
}

@Composable
private fun LearnedLinksDialog(
    productName: String,
    links: List<MerchantProductLinkEntity>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vínculos e aliases aprendidos") },
        text = {
            if (links.isEmpty()) {
                Text(
                    text = "O produto $productName ainda não possui vínculos aprendidos a partir de NFC-e.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(links, key = { it.id }) { link ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    text = link.fiscalDescription,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "CNPJ: ${formatLearnedCnpj(link.merchantCnpj)}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                link.itemCode?.takeIf { it.isNotBlank() }?.let { code ->
                                    Text(
                                        text = "Código no estabelecimento: $code",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Text(
                                    text = "Último reconhecimento: ${formatLearnedDateTime(link.lastUsedAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        },
    )
}

private fun formatLearnedCnpj(value: String): String {
    val digits = value.filter(Char::isDigit)
    if (digits.length != 14) return value
    return "${digits.substring(0, 2)}.${digits.substring(2, 5)}.${digits.substring(5, 8)}/" +
        "${digits.substring(8, 12)}-${digits.substring(12, 14)}"
}

private fun formatLearnedDateTime(timestamp: Long): String =
    DateTimeFormatter
        .ofPattern("dd/MM/yyyy HH:mm")
        .format(
            Instant
                .ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault()),
        )
