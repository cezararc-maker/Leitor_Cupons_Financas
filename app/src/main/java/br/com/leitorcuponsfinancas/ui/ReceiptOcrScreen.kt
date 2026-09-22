package br.com.leitorcuponsfinancas.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.leitorcuponsfinancas.domain.OcrItemDraft
import br.com.leitorcuponsfinancas.domain.OcrReceiptDraft

@Composable
fun ReceiptOcrScreen(
    modifier: Modifier = Modifier,
    viewModel: NfceViewModel = viewModel(),
) {
    val ocrState by viewModel.ocrState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    var merchant by remember { mutableStateOf("") }
    var cnpj by remember { mutableStateOf("") }
    var accessKey by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var series by remember { mutableStateOf("") }
    var issuedAt by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(false) }
    val items = remember { mutableStateListOf<OcrItemDraft>() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            confirmed = false
            viewModel.clearOcrState()
            viewModel.readReceiptDocument(uri)
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            confirmed = false
            viewModel.clearOcrState()
            viewModel.readReceiptPhoto(bitmap)
        }
    }

    LaunchedEffect(ocrState.draft) {
        ocrState.draft?.let { draft ->
            merchant = draft.merchantName
            cnpj = draft.merchantCnpj
            accessKey = draft.accessKey
            number = draft.number
            series = draft.series
            issuedAt = draft.issuedAt
            total = draft.totalAmount
            items.clear()
            items.addAll(draft.items)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ler cupom por imagem ou PDF", style = MaterialTheme.typography.headlineSmall)
        Text("Fotografe o cupom ou selecione uma imagem, print ou PDF. O reconhecimento é uma sugestão: revise tudo antes de confirmar.")

        Button(onClick = { camera.launch(null) }, enabled = !ocrState.reading, modifier = Modifier.fillMaxWidth()) {
            Text("Tirar foto do cupom")
        }
        OutlinedButton(
            onClick = { picker.launch(arrayOf("image/*", "application/pdf")) },
            enabled = !ocrState.reading,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Selecionar imagem, print ou PDF") }

        if (ocrState.reading) {
            CircularProgressIndicator()
            Text("Reconhecendo texto e dados da compra...")
        }
        ocrState.error?.let { Text(it) }

        if (ocrState.draft != null) {
            Text("Revise as informações identificadas", style = MaterialTheme.typography.titleLarge)
            EditField("Estabelecimento *", merchant) { merchant = it; confirmed = false }
            EditField("CNPJ", cnpj) { cnpj = it; confirmed = false }
            EditField("Chave de acesso", accessKey) { accessKey = it; confirmed = false }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(number, { number = it; confirmed = false }, label = { Text("NFC-e nº") }, modifier = Modifier.weight(1f))
                OutlinedTextField(series, { series = it; confirmed = false }, label = { Text("Série") }, modifier = Modifier.weight(1f))
            }
            EditField("Data/hora *", issuedAt) { issuedAt = it; confirmed = false }
            EditField("Total da compra", total) { total = it; confirmed = false }

            Text("Itens", style = MaterialTheme.typography.titleMedium)
            items.forEachIndexed { index, item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EditField("Descrição *", item.description) { items[index] = item.copy(description = it); confirmed = false }
                        EditField("Código", item.code) { items[index] = item.copy(code = it); confirmed = false }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(item.quantity, { items[index] = item.copy(quantity = it); confirmed = false }, label = { Text("Qtd.") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(item.unit, { items[index] = item.copy(unit = it); confirmed = false }, label = { Text("Unidade") }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(item.unitPrice, { items[index] = item.copy(unitPrice = it); confirmed = false }, label = { Text("Valor unit.") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(item.total, { items[index] = item.copy(total = it); confirmed = false }, label = { Text("Total") }, modifier = Modifier.weight(1f))
                        }
                        OutlinedButton(onClick = { items.removeAt(index); confirmed = false }) { Text("Remover item") }
                    }
                }
            }
            OutlinedButton(
                onClick = { items.add(OcrItemDraft(description = "")); confirmed = false },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Adicionar item não identificado") }

            Row(Modifier.fillMaxWidth()) {
                Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                Text(
                    "Revisei e confirmo que as informações acima estão corretas.",
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Button(
                enabled = confirmed && merchant.isNotBlank() && issuedAt.isNotBlank() && items.isNotEmpty() && !saveState.saving,
                onClick = {
                    viewModel.saveOcrDraft(
                        OcrReceiptDraft(merchant, cnpj, accessKey, number, series, issuedAt, total, items.toList())
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (saveState.saving) "Salvando..." else "Salvar no histórico") }

            saveState.message?.let { Text(it) }
            saveState.error?.let { Text(it) }
        }
    }
}

@Composable
private fun EditField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}
