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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
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
import br.com.leitorcuponsfinancas.data.MerchantEntity
import br.com.leitorcuponsfinancas.data.ProductEntity
import br.com.leitorcuponsfinancas.data.TaxonomyLevel
import br.com.leitorcuponsfinancas.data.TaxonomyNodeEntity
import br.com.leitorcuponsfinancas.data.TaxonomyProductLinkEntity
import br.com.leitorcuponsfinancas.domain.ProductNormalizer
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
    val taxonomyNodes by viewModel.taxonomyNodes.collectAsStateWithLifecycle()
    val taxonomyProductLinks by viewModel.taxonomyProductLinks.collectAsStateWithLifecycle()
    val merchants by viewModel.merchants.collectAsStateWithLifecycle()
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
    var selectedTaxonomyNodeId by remember { mutableStateOf<Long?>(null) }
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
            selectedTaxonomyNodeId = null
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
        selectedTaxonomyNodeId = inferManualTaxonomyNodeId(
            product = product,
            nodes = taxonomyNodes,
            links = taxonomyProductLinks,
        )

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

    val matchedMerchant = remember(merchantName, merchantCnpj, merchants) {
        findManualMerchant(
            merchants = merchants,
            merchantName = merchantName,
            merchantCnpj = merchantCnpj,
        )
    }
    val merchantSegmentNodeId = matchedMerchant?.segmentNodeId

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ScreenHero(
                    title = "Nova compra",
                    subtitle = "Preencha apenas o essencial. O app reaproveita cadastros e calcula o total para você.",
                    icon = Icons.Default.ShoppingCart,
                )
            }

            item {
                FlowSectionCard(
                    title = "O que você comprou?",
                    subtitle = "Informe o item e vincule ao Produto Mestre raiz.",
                    icon = Icons.Default.ShoppingCart,
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = TextInputRules.capitalizeFirstLetter(it)
                            viewModel.clearMessage()
                        },
                        label = { RequiredFieldLabel("Nome do item") },
                        placeholder = { Text("Ex.: Macarrão Renata espaguete 500 g") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

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
                            Text("Produto Mestre: ${selectedProduct?.normalizedName}")
                        }
                    }

                    selectedProduct?.let { product ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    text = product.normalizedName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
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

                    when (val suggestion = smartSuggestion) {
                        is SmartProductSuggestion.ExistingProduct -> {
                            if (selectedProduct?.id != suggestion.product.id) {
                                AnimatedInfoCard(
                                    visible = true,
                                    title = "Sugestão inteligente",
                                    message = "${suggestion.product.normalizedName} • ${suggestion.confidence}% de compatibilidade",
                                )
                                OutlinedButton(
                                    onClick = { applyProduct(suggestion.product) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Usar ${suggestion.product.normalizedName}")
                                }
                            }
                        }

                        is SmartProductSuggestion.NewProduct -> {
                            AnimatedInfoCard(
                                visible = true,
                                title = "Possível Produto Mestre",
                                message = "${suggestion.name} • ${suggestion.confidence}% de compatibilidade. Abra Vincular Produto Mestre para revisar.",
                            )
                        }

                        null -> Unit
                    }
                }
            }

            item {
                FlowSectionCard(
                    title = "Quanto?",
                    subtitle = "Quantidade, unidade e valor. O total é calculado automaticamente.",
                    icon = Icons.Default.Payments,
                    accent = MaterialTheme.colorScheme.secondary,
                ) {
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

                    if (unitType == ManualUnitType.OTHER) {
                        OutlinedTextField(
                            value = customUnit,
                            onValueChange = {
                                customUnit = TextInputRules.capitalizeFirstLetter(it)
                                viewModel.clearMessage()
                            },
                            label = { RequiredFieldLabel("Tipo de unidade") },
                            placeholder = { Text("Ex.: Caixa, dúzia, bandeja...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
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
                                        ManualUnitType.KILOGRAM -> "0,750"
                                        ManualUnitType.LITER -> "1,5"
                                        else -> "2"
                                    },
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )

                        CurrencyTextField(
                            value = unitPrice,
                            onValueChange = {
                                unitPrice = it
                                viewModel.clearMessage()
                            },
                            label = { RequiredFieldLabel(priceLabel) },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = "TOTAL",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                text = calculatedTotal?.let {
                                    "R$ ${ManualEntryCalculator.formatMoney(it)}"
                                } ?: "—",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                text = "Quantidade × valor unitário",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }

            item {
                FlowSectionCard(
                    title = "Onde e quando?",
                    subtitle = "O estabelecimento é sugerido a partir do seu histórico.",
                    icon = Icons.Default.Storefront,
                    accent = MaterialTheme.colorScheme.tertiary,
                ) {
                    SuggestionTextField(
                        value = merchantName,
                        onValueChange = {
                            merchantName = it
                            viewModel.clearMessage()
                        },
                        suggestions = merchantSuggestions.map { it.name },
                        label = { Text("Estabelecimento (opcional)") },
                        placeholder = { Text("Ex.: Atacadão") },
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

                    OutlinedTextField(
                        value = merchantCnpj,
                        onValueChange = { value ->
                            merchantCnpj = value.filter { it.isDigit() }.take(14)
                            viewModel.clearMessage()
                        },
                        label = { Text("CNPJ (opcional)") },
                        placeholder = { Text("14 dígitos") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    PurchaseDateField(
                        value = dateText,
                        onValueChange = {
                            dateText = it
                            viewModel.clearMessage()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        monthsBack = 6,
                    )
                }
            }

            saveState.error?.let { error ->
                item {
                    AnimatedInfoCard(
                        visible = true,
                        title = "Não foi possível salvar",
                        message = error,
                        accent = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Surface(
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
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
                        taxonomyNodeId = selectedTaxonomyNodeId,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(
                    if (saveState.saving) {
                        "Salvando..."
                    } else {
                        "Salvar compra"
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
            taxonomyNodes = taxonomyNodes,
            taxonomyProductLinks = taxonomyProductLinks,
            segmentLockedTo = merchantSegmentNodeId,
            suggestion = smartSuggestion,
            currentProduct = selectedProduct,
            currentTaxonomyNodeId = selectedTaxonomyNodeId,
            error = productDialogError,
            onDismiss = {
                showProducts = false
                productDialogError = null
            },
            onSelect = { product, taxonomyNodeId ->
                selectedTaxonomyNodeId = taxonomyNodeId
                viewModel.linkProductTaxonomy(product.id, taxonomyNodeId)
                applyProduct(product)
                selectedTaxonomyNodeId = taxonomyNodeId
            },
            onCreate = { name, taxonomyNodeId, unit ->
                viewModel.createProductMasterTaxonomy(
                    name = name,
                    taxonomyNodeId = taxonomyNodeId,
                    unit = unit,
                    onCreated = { product ->
                        applyProduct(product)
                        selectedTaxonomyNodeId = taxonomyNodeId
                    },
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
    taxonomyNodes: List<TaxonomyNodeEntity>,
    taxonomyProductLinks: List<TaxonomyProductLinkEntity>,
    segmentLockedTo: Long?,
    suggestion: SmartProductSuggestion?,
    currentProduct: ProductEntity?,
    currentTaxonomyNodeId: Long?,
    error: String?,
    onDismiss: () -> Unit,
    onSelect: (ProductEntity, Long) -> Unit,
    onCreate: (String, Long, String) -> Unit,
) {
    var selectedNodeId by remember(
        currentTaxonomyNodeId,
        segmentLockedTo,
        taxonomyNodes.size,
    ) {
        mutableStateOf(
            currentTaxonomyNodeId
                ?: segmentLockedTo,
        )
    }
    var creatingNew by remember { mutableStateOf(false) }
    var name by remember(description) {
        mutableStateOf(
            (suggestion as? SmartProductSuggestion.NewProduct)?.name
                ?.takeIf { it.isNotBlank() }
                ?: description,
        )
    }
    var unit by remember(effectiveUnit) {
        mutableStateOf(effectiveUnit.ifBlank { "UN" })
    }
    var query by remember { mutableStateOf("") }

    val selectedNode = taxonomyNodes.firstOrNull { it.id == selectedNodeId }
    val selectedPath = remember(selectedNodeId, taxonomyNodes) {
        selectedNodeId
            ?.let { manualTaxonomyPath(taxonomyNodes, it) }
            .orEmpty()
    }
    val hasDepartment = selectedPath.any {
        it.level == TaxonomyLevel.DEPARTMENT.code
    }
    val hasCategory = selectedPath.any {
        it.level == TaxonomyLevel.CATEGORY.code
    }
    val classificationReady = hasDepartment && hasCategory

    val linkedProductIds = remember(selectedNodeId, taxonomyProductLinks) {
        selectedNodeId
            ?.let { nodeId ->
                taxonomyProductLinks
                    .filter { it.taxonomyNodeId == nodeId }
                    .map { it.productId }
                    .toSet()
            }
            .orEmpty()
    }

    val scopedProducts = remember(
        selectedNodeId,
        selectedNode,
        products,
        linkedProductIds,
    ) {
        if (selectedNode == null || !classificationReady) {
            emptyList()
        } else {
            products
                .filter { product ->
                    product.id in linkedProductIds ||
                        when (selectedNode.level) {
                            TaxonomyLevel.DEPARTMENT.code ->
                                ProductNormalizer.searchKey(product.sector) ==
                                    ProductNormalizer.searchKey(selectedNode.name)

                            TaxonomyLevel.CATEGORY.code ->
                                ProductNormalizer.searchKey(product.category) ==
                                    ProductNormalizer.searchKey(selectedNode.name)

                            TaxonomyLevel.SUBCATEGORY.code ->
                                ProductNormalizer.searchKey(product.subcategory.orEmpty()) ==
                                    ProductNormalizer.searchKey(selectedNode.name)

                            else -> false
                        }
                }
                .distinctBy { it.id }
                .sortedBy { it.normalizedName.lowercase() }
        }
    }

    val filteredScopedProducts = remember(query, scopedProducts) {
        val clean = ProductNormalizer.searchKey(query)
        if (clean.isBlank()) {
            scopedProducts
        } else {
            scopedProducts.filter {
                ProductNormalizer.searchKey(it.normalizedName).contains(clean)
            }
        }
    }

    val suggestedExisting = suggestion as? SmartProductSuggestion.ExistingProduct
    val suggestedAllowed = suggestedExisting?.product?.let { suggestedProduct ->
        scopedProducts.any { it.id == suggestedProduct.id }
    } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Vincular Produto Mestre")
                Text(
                    text = description.ifBlank { "Lançamento manual" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    TaxonomyPathSelector(
                        nodes = taxonomyNodes,
                        selectedNodeId = selectedNodeId,
                        onSelected = {
                            selectedNodeId = it
                            creatingNew = false
                            query = ""
                        },
                        segmentLockedTo = segmentLockedTo,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (
                    segmentLockedTo != null &&
                    selectedPath.firstOrNull()?.id == segmentLockedTo
                ) {
                    item {
                        Text(
                            text = "O segmento foi reconhecido pelo estabelecimento e permanece fixo nesta vinculação.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (classificationReady) {
                    if (suggestedAllowed && suggestedExisting != null) {
                        item {
                            AnimatedInfoCard(
                                visible = true,
                                title = "Sugestão dentro desta classificação",
                                message = "${suggestedExisting.product.normalizedName} • ${suggestedExisting.confidence}% de compatibilidade",
                            )
                        }
                        item {
                            Button(
                                onClick = {
                                    selectedNodeId?.let { nodeId ->
                                        onSelect(suggestedExisting.product, nodeId)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Usar ${suggestedExisting.product.normalizedName}")
                            }
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
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = TextInputRules.capitalizeFirstLetter(it) },
                            label = { Text("Buscar nesta classificação") },
                            placeholder = { Text("Ex.: Banana") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (filteredScopedProducts.isEmpty()) {
                        item {
                            Text(
                                text = "Nenhum Produto Mestre encontrado neste ramo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(filteredScopedProducts, key = { it.id }) { product ->
                            OutlinedButton(
                                onClick = {
                                    selectedNodeId?.let { nodeId ->
                                        onSelect(product, nodeId)
                                    }
                                },
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

                    item {
                        TextButton(
                            onClick = { creatingNew = !creatingNew },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (creatingNew) {
                                    "Cancelar novo Produto Mestre"
                                } else {
                                    "Criar Produto Mestre neste ramo"
                                },
                            )
                        }
                    }

                    if (creatingNew) {
                        item {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = TextInputRules.capitalizeFirstLetter(it) },
                                label = { RequiredFieldLabel("Produto raiz") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
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
                        item {
                            Button(
                                enabled = name.isNotBlank() &&
                                    unit.isNotBlank() &&
                                    selectedNodeId != null,
                                onClick = {
                                    selectedNodeId?.let { nodeId ->
                                        onCreate(name, nodeId, unit)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Criar e vincular")
                            }
                        }
                    }
                } else if (selectedNode != null) {
                    item {
                        Text(
                            text = "Continue afunilando a classificação até chegar pelo menos à Categoria.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
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
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

private fun inferManualTaxonomyNodeId(
    product: ProductEntity,
    nodes: List<TaxonomyNodeEntity>,
    links: List<TaxonomyProductLinkEntity>,
): Long? {
    links.firstOrNull { it.productId == product.id }?.let {
        return it.taxonomyNodeId
    }

    fun key(value: String?): String = ProductNormalizer.searchKey(value.orEmpty())

    product.subcategory?.let { subcategory ->
        nodes.firstOrNull { node ->
            node.level == TaxonomyLevel.SUBCATEGORY.code &&
                key(node.name) == key(subcategory) &&
                manualTaxonomyPath(nodes, node.id).let { path ->
                    path.any {
                        it.level == TaxonomyLevel.DEPARTMENT.code &&
                            key(it.name) == key(product.sector)
                    } &&
                        path.any {
                            it.level == TaxonomyLevel.CATEGORY.code &&
                                key(it.name) == key(product.category)
                        }
                }
        }?.let { return it.id }
    }

    nodes.firstOrNull { node ->
        node.level == TaxonomyLevel.CATEGORY.code &&
            key(node.name) == key(product.category) &&
            manualTaxonomyPath(nodes, node.id).any {
                it.level == TaxonomyLevel.DEPARTMENT.code &&
                    key(it.name) == key(product.sector)
            }
    }?.let { return it.id }

    return null
}

private fun findManualMerchant(
    merchants: List<MerchantEntity>,
    merchantName: String,
    merchantCnpj: String,
): MerchantEntity? {
    val cnpjDigits = merchantCnpj.filter(Char::isDigit)
    if (cnpjDigits.length == 14) {
        merchants.firstOrNull { it.cnpjDigits == cnpjDigits }?.let { return it }
    }

    val nameKey = ProductNormalizer.searchKey(merchantName)
    if (nameKey.isBlank()) return null

    return merchants.firstOrNull {
        ProductNormalizer.searchKey(it.displayName) == nameKey
    }
}

private fun manualTaxonomyPath(
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
