package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.leitorcuponsfinancas.data.MerchantProductLinkEntity
import br.com.leitorcuponsfinancas.data.ProductEntity
import br.com.leitorcuponsfinancas.domain.ProductDuplicateDetector
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

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }

    val categories = remember(products) {
        products
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { product ->
            val matchesCategory = selectedCategory == null ||
                product.category.equals(selectedCategory, ignoreCase = true)
            val query = searchQuery.trim()
            val matchesQuery = query.isBlank() ||
                product.normalizedName.contains(query, ignoreCase = true) ||
                product.category.contains(query, ignoreCase = true) ||
                product.sector.contains(query, ignoreCase = true) ||
                product.subcategory.orEmpty().contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

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
                Icon(Icons.Default.Add, contentDescription = "Adicionar Produto Mestre")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ScreenHero(
                    title = "Produtos Mestres",
                    subtitle = "${products.size} produto(s) raiz. Marcas e descrições comerciais ficam nos aliases aprendidos.",
                    icon = Icons.Default.Inventory2,
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar Produto Mestre") },
                    placeholder = { Text("Ex.: Macarrão") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (categories.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            AssistChip(
                                onClick = { selectedCategory = null },
                                label = { Text("Todos") },
                            )
                        }
                        items(categories) { category ->
                            AssistChip(
                                onClick = {
                                    selectedCategory = if (
                                        selectedCategory.equals(category, ignoreCase = true)
                                    ) {
                                        null
                                    } else {
                                        category
                                    }
                                },
                                label = { Text(category) },
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Produto Mestre = produto raiz",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "Macarrão Renata, Liane ou Galo apontam para Macarrão. Macarrão instantâneo permanece separado por representar outro tipo de produto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (filteredProducts.isEmpty()) {
                item {
                    FlowSectionCard(
                        title = if (products.isEmpty()) {
                            "Nenhum Produto Mestre cadastrado"
                        } else {
                            "Nenhum resultado"
                        },
                        subtitle = if (products.isEmpty()) {
                            "Use o botão + para cadastrar o primeiro produto raiz."
                        } else {
                            "Ajuste a busca ou selecione outra categoria."
                        },
                        icon = Icons.Default.Inventory2,
                    ) {
                        Text(
                            text = "Evite criar variações por marca. O objetivo é consolidar seus gastos pelo produto que realmente importa para a análise.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
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
            onUseExisting = { existing ->
                editing = existing
                saveError = null
                showForm = true
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
    onUseExisting: (ProductEntity) -> Unit,
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

    val duplicateCandidates = remember(name, otherProducts) {
        ProductDuplicateDetector.findCandidates(
            input = name,
            products = otherProducts,
        )
    }

    val valid = name.isNotBlank() &&
        sector.isNotBlank() &&
        category.isNotBlank() &&
        unit.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                ScreenHero(
                    title = if (product == null) {
                        "Novo Produto Mestre"
                    } else {
                        "Editar Produto Mestre"
                    },
                    subtitle = "Produto raiz, sem marca. Classifique uma vez e reutilize em todas as compras.",
                    icon = Icons.Default.Inventory2,
                    modifier = Modifier.padding(14.dp),
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 18.dp,
                        end = 18.dp,
                        bottom = 18.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Text(
                                text = "Exemplo: Renata, Liane e Galo podem ser reconhecidos como Macarrão. Macarrão instantâneo continua separado porque representa outro tipo de produto.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }

                    item {
                        SuggestionTextField(
                            value = name,
                            onValueChange = { name = it },
                            suggestions = nameSuggestions,
                            label = { Text("Produto raiz *") },
                            placeholder = { Text("Ex.: Macarrão") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (duplicateCandidates.isNotEmpty()) {
                        item {
                            FlowSectionCard(
                                title = "Verifique antes de criar",
                                subtitle = "Encontramos Produtos Mestres parecidos. Reutilizar um cadastro evita dividir suas análises.",
                                icon = Icons.Default.Search,
                                accent = MaterialTheme.colorScheme.tertiary,
                            ) {
                                duplicateCandidates.forEach { candidate ->
                                    OutlinedButton(
                                        onClick = {
                                            onUseExisting(candidate.product)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            "${candidate.product.normalizedName} • ${candidate.similarity}% semelhante",
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = fiscalDescription,
                            onValueChange = { fiscalDescription = it },
                            label = { Text("Descrição fiscal conhecida (opcional)") },
                            placeholder = { Text("Ex.: MAC RENATA ESPAGUETE 500G") },
                            supportingText = {
                                Text("Pode conter marca. Serve apenas para ajudar no reconhecimento fiscal.")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    item {
                        FlowSectionCard(
                            title = "Classificação",
                            subtitle = "Reutilize setores, categorias e subcategorias já cadastrados.",
                            icon = Icons.Default.Inventory2,
                        ) {
                            SuggestionTextField(
                                value = sector,
                                onValueChange = { sector = it },
                                suggestions = sectorSuggestions,
                                label = { Text("Setor *") },
                                placeholder = { Text("Ex.: Alimentação") },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            SuggestionTextField(
                                value = category,
                                onValueChange = { category = it },
                                suggestions = categorySuggestions,
                                label = { Text("Categoria *") },
                                placeholder = { Text("Ex.: Mercado") },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            SuggestionTextField(
                                value = subcategory,
                                onValueChange = { subcategory = it },
                                suggestions = subcategorySuggestions,
                                label = { Text("Subcategoria") },
                                placeholder = { Text("Ex.: Massas") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
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
                            AnimatedInfoCard(
                                visible = true,
                                title = "Não foi possível salvar",
                                message = error,
                                accent = MaterialTheme.colorScheme.error,
                            )
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
                            OutlinedButton(
                                onClick = onDeactivate,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Desativar Produto Mestre")
                            }
                        }
                    }
                }

                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Cancelar")
                        }
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
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
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
