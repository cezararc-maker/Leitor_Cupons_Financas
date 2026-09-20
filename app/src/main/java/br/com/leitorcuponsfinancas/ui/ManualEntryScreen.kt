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
    var unit by remember { mutableStateOf("UN") }
    var unitPrice by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showProducts by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Button(onClick = onBack) { Text("Voltar") } }
        item {
            Text("Lançamento manual", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Use quando não houver NFC-e ou cupom fiscal. O lançamento aparecerá normalmente em Histórico e Gastos.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item { OutlinedTextField(value = merchantName, onValueChange = { merchantName = it; viewModel.clearMessage() }, label = { Text("Estabelecimento *") }, placeholder = { Text("Ex.: Padaria do bairro") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = dateText, onValueChange = { dateText = it; viewModel.clearMessage() }, label = { Text("Data *") }, placeholder = { Text("DD/MM/AAAA") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = description, onValueChange = { description = it; viewModel.clearMessage() }, label = { Text("Descrição do item *") }, placeholder = { Text("Ex.: Pão francês") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item {
            OutlinedButton(onClick = { showProducts = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedProduct?.let { "Produto mestre: ${it.normalizedName}" } ?: "Vincular a produto mestre (opcional)")
            }
            if (selectedProduct != null) {
                TextButton(onClick = { selectedProduct = null }) { Text("Remover vínculo") }
            }
        }
        item { OutlinedTextField(value = quantity, onValueChange = { quantity = it; viewModel.clearMessage() }, label = { Text("Quantidade") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = unit, onValueChange = { unit = it; viewModel.clearMessage() }, label = { Text("Unidade") }, placeholder = { Text("UN, KG, L...") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = unitPrice, onValueChange = { unitPrice = it; viewModel.clearMessage() }, label = { Text("Valor unitário") }, placeholder = { Text("Ex.: 1,50") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = totalAmount, onValueChange = { totalAmount = it; viewModel.clearMessage() }, label = { Text("Valor total *") }, placeholder = { Text("Ex.: 15,00") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }

        saveState.message?.let { message ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Lançamento salvo", style = MaterialTheme.typography.titleMedium)
                        Text(message)
                    }
                }
            }
        }
        saveState.error?.let { error ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Não foi possível salvar", style = MaterialTheme.typography.titleMedium)
                        Text(error)
                    }
                }
            }
        }
        item {
            Button(
                enabled = !saveState.saving && merchantName.isNotBlank() && description.isNotBlank() && totalAmount.isNotBlank(),
                onClick = {
                    viewModel.save(
                        merchantName = merchantName,
                        dateText = dateText,
                        description = description,
                        quantity = quantity,
                        unit = unit,
                        unitPrice = unitPrice,
                        totalAmount = totalAmount,
                        productId = selectedProduct?.id,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (saveState.saving) "Salvando..." else "Salvar lançamento manual") }
        }
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
                                onClick = { selectedProduct = product; showProducts = false },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(product.normalizedName)
                                    Text(
                                        listOfNotNull(product.sector, product.category, product.subcategory).joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showProducts = false }) { Text("Cancelar") } },
        )
    }
}
