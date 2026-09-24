package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.leitorcuponsfinancas.data.ProductEntity
import br.com.leitorcuponsfinancas.domain.ProductSuggestionEngine
import br.com.leitorcuponsfinancas.domain.SmartProductSuggestion
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ManualEntryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManualEntryViewModel = viewModel(),
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val learnedLinks by viewModel.learnedLinks.collectAsStateWithLifecycle()
    val merchantSuggestions by viewModel.merchantSuggestions.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    var merchantName by remember { mutableStateOf("") }
    var merchantCnpj by remember { mutableStateOf("") }
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
    var productDialogError by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(saveState.message) {
        saveState.message?.let { message ->
            merchantName = ""
            merchantCnpj = ""
            dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            description = ""
            quantity = "1"
            unitType = ManualUnitType.UNIT
            customUnit = ""
            unitPrice = ""
            selectedProduct = null
            showProducts = false
            showUnits = false
            productDialogError = null
            successMessage = message
            viewModel.clearMessage()
        }
    }

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

    val smartSuggestion = remember(description, effectiveUnit, products, learnedLinks) {
        ProductSuggestionEngine.suggest(
            description = description,
            unit = effectiveUnit,
            products = products,
            learnedLinks = learnedLinks,
        )
    }

    val canSave = !saveState.saving &&
        description.isNotBlank() &&
        dateText.isNotBlank() &&
        quantity.isNotBlank() &&
        unitPrice.isNotBlank() &&
        calculatedTotal != null &&
        effectiveUnit.isNotBlank() &&
        selectedProduct != null

    fun applyProduct(product: ProductEntity) {
        selectedProduct = product

        val productUnit = product.unit.trim().uppercase()
        val recognizedUnit = ManualUnitType.entries.firstOrNull {
            it != ManualUnitType.OTHER && it.code == productUnit
        }

        if (recognizedUnit != null) {
            unitType = recognizedUnit
            customUnit = ""
        } else if (productUnit.isNotBlank()) {
            unitType = ManualUnitType.OTHER
            customUnit = productUnit
        }

        showProducts = false
        productDialogError = null
        viewModel.clearMessage()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Lançamento manual",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Use quando não houver NFC-e ou cupom fiscal. Os campos com * são obrigatórios.",
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
                label = { RequiredFieldLabel("Nome do item") },
                placeholder = { Text("Ex.: Pão francês") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item {
            OutlinedButton(
                onClick = {
                    productDialogError = null
                    showProducts = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (selectedProduct == null) {
                    RequiredButtonLabel("Vincular Produto Mestre")
                } else {
                    Text("Produto mestre: ${selectedProduct?.normalizedName}")
                }
            }
            selectedProduct?.let { product ->
                Text(
                    text = listOfNotNull(
                        product.sector,
                        product.category,
                        product.subcategory,
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            OutlinedButton(
                onClick = { showUnits = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row {
                    Text(
                        if (unitType == ManualUnitType.OTHER && customUnit.isNotBlank()) {
                            "Unidade: ${customUnit.trim().uppercase()}"
                        } else {
                            "Unidade: ${unitType.label}"
                        },
                    )
                    RequiredAsterisk()
                }
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
                    label = { RequiredFieldLabel("Tipo de unidade") },
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
                label = { RequiredFieldLabel(unitType.quantityLabel) },
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
                label = { RequiredFieldLabel(priceLabel) },
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
            SuggestionTextField(
                value = merchantName,
                onValueChange = {
                    merchantName = it
                    viewModel.clearMessage()
                },
                suggestions = merchantSuggestions.map { it.name },
                label = { Text("Nome do estabelecimento / empresa (opcional)") },
                placeholder = { Text("Ex.: Padaria Pão de Queijo") },
                modifier = Modifier.fillMaxWidth(),
                onSuggestionSelected = { selected ->
                    merchantName = selected
                    merchantSuggestions
                        .firstOrNull { it.name.equals(selected, ignoreCase = true) }
                        ?.cnpj
                        ?.filter(Char::isDigit)
                        ?.takeIf { it.length == 14 }
                        ?.let { merchantCnpj = it }
                    viewModel.clearMessage()
                },
            )
        }

        item {
            OutlinedTextField(
                value = merchantCnpj,
                onValueChange = { value ->
                    merchantCnpj = value.filter { it.isDigit() }.take(14)
                    viewModel.clearMessage()
                },
                label = { Text("CNPJ do estabelecimento (opcional)") },
                placeholder = { Text("14 dígitos") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = {
                    Text("Se informado, o CNPJ fica salvo no lançamento.")
                },
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
                label = { RequiredFieldLabel("Data") },
                placeholder = { Text("DD/MM/AAAA") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
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
                        merchantCnpj = merchantCnpj,
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

    successMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Lançamento salvo") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { successMessage = null }) {
                    Text("OK")
                }
            },
        )
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
        ManualProductLinkDialog(
            description = description,
            effectiveUnit = effectiveUnit,
            products = products,
            suggestion = smartSuggestion,
            currentProduct = selectedProduct,
            error = productDialogError,
            onDismiss = {
                showProducts = false
                productDialogError = null
            },
            onSelect = ::applyProduct,
            onCreate = { name, sector, category, subcategory, unit ->
                viewModel.createProductMaster(
                    name = name,
                    sector = sector,
                    category = category,
                    subcategory = subcategory,
                    unit = unit,
                    onCreated = ::applyProduct,
                    onError = { productDialogError = it },
                )
            },
        )
    }
}

@Composable
private fun ManualProductLinkDialog(
    description: String,
    effectiveUnit: String,
    products: List<ProductEntity>,
    suggestion: SmartProductSuggestion?,
    currentProduct: ProductEntity?,
    error: String?,
    onDismiss: () -> Unit,
    onSelect: (ProductEntity) -> Unit,
    onCreate: (String, String, String, String, String) -> Unit,
) {
    var creatingNew by remember { mutableStateOf(false) }
    var name by remember(description) { mutableStateOf(description) }
    var sector by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var subcategory by remember { mutableStateOf("") }
    var unit by remember(effectiveUnit) { mutableStateOf(effectiveUnit.ifBlank { "UN" }) }

    val sectorSuggestions = remember(products) {
        products.map { it.sector }
    }
    val categorySuggestions = remember(products, sector) {
        val sameSector = products.filter { it.sector.equals(sector, ignoreCase = true) }
        (if (sameSector.isNotEmpty()) sameSector else products).map { it.category }
    }
    val subcategorySuggestions = remember(products, category) {
        val sameCategory = products.filter { it.category.equals(category, ignoreCase = true) }
        (if (sameCategory.isNotEmpty()) sameCategory else products)
            .mapNotNull { it.subcategory }
    }

    fun useNewSuggestion(newSuggestion: SmartProductSuggestion.NewProduct) {
        name = newSuggestion.name
        sector = newSuggestion.sector
        category = newSuggestion.category
        subcategory = newSuggestion.subcategory.orEmpty()
        unit = newSuggestion.unit
        creatingNew = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (creatingNew) "Criar Produto Mestre" else "Vincular Produto Mestre")
        },
        text = {
            if (creatingNew) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            text = "Crie o Produto Mestre pelo nome raiz, sem marca. Ele será vinculado automaticamente ao lançamento e as variações comerciais poderão ser aprendidas como aliases.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { RequiredFieldLabel("Produto raiz") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    item {
                        SuggestionTextField(
                            value = sector,
                            onValueChange = { sector = it },
                            suggestions = sectorSuggestions,
                            label = { RequiredFieldLabel("Setor") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        SuggestionTextField(
                            value = category,
                            onValueChange = { category = it },
                            suggestions = categorySuggestions,
                            label = { RequiredFieldLabel("Categoria") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        SuggestionTextField(
                            value = subcategory,
                            onValueChange = { subcategory = it },
                            suggestions = subcategorySuggestions,
                            label = { Text("Subcategoria (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it.uppercase() },
                            label = { RequiredFieldLabel("Unidade") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    error?.let { message ->
                        item {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (description.isBlank()) {
                        item {
                            Text(
                                text = "Digite primeiro o nome do item para receber uma sugestão inteligente.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    suggestion?.let { smart ->
                        item {
                            ManualSmartSuggestionCard(
                                suggestion = smart,
                                onUseExisting = onSelect,
                                onCreateNew = ::useNewSuggestion,
                            )
                        }
                    }

                    currentProduct?.let { current ->
                        item {
                            Text(
                                text = "Vínculo atual: ${current.normalizedName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { creatingNew = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Criar novo produto mestre")
                        }
                    }

                    if (products.isNotEmpty()) {
                        item {
                            Text(
                                text = "Produtos mestres existentes:",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        items(products, key = { it.id }) { product ->
                            OutlinedButton(
                                onClick = { onSelect(product) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(
                                        if (product.id == currentProduct?.id) {
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
                    }
                }
            }
        },
        confirmButton = {
            if (creatingNew) {
                Button(
                    enabled = name.isNotBlank() &&
                        sector.isNotBlank() &&
                        category.isNotBlank() &&
                        unit.isNotBlank(),
                    onClick = { onCreate(name, sector, category, subcategory, unit) },
                ) {
                    Text("Criar e vincular")
                }
            }
        },
        dismissButton = {
            TextButton(
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
private fun ManualSmartSuggestionCard(
    suggestion: SmartProductSuggestion,
    onUseExisting: (ProductEntity) -> Unit,
    onCreateNew: (SmartProductSuggestion.NewProduct) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (suggestion.confidence >= 90) {
                    "Sugestão inteligente • Alta confiança"
                } else {
                    "Sugestão inteligente"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )

            when (suggestion) {
                is SmartProductSuggestion.ExistingProduct -> {
                    Text(
                        text = suggestion.product.normalizedName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = listOfNotNull(
                            suggestion.product.sector,
                            suggestion.product.category,
                            suggestion.product.subcategory,
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = { onUseExisting(suggestion.product) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Usar sugestão")
                    }
                }

                is SmartProductSuggestion.NewProduct -> {
                    Text(
                        text = suggestion.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = listOfNotNull(
                            suggestion.sector,
                            suggestion.category,
                            suggestion.subcategory,
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = { onCreateNew(suggestion) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Criar produto sugerido")
                    }
                }
            }
        }
    }
}

@Composable
private fun RequiredFieldLabel(text: String) {
    Row {
        Text(text)
        RequiredAsterisk()
    }
}

@Composable
private fun RequiredButtonLabel(text: String) {
    Row {
        Text(text)
        RequiredAsterisk()
    }
}

@Composable
private fun RequiredAsterisk() {
    Text(
        text = " *",
        color = MaterialTheme.colorScheme.error,
        fontWeight = FontWeight.Bold,
    )
}
