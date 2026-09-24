package br.com.leitorcuponsfinancas.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
            total = CurrencyInputFormatter.fromStoredDecimal(draft.totalAmount)
            items.clear()
            items.addAll(
                draft.items.map { item ->
                    item.copy(
                        unitPrice = CurrencyInputFormatter.fromStoredDecimal(item.unitPrice),
                        total = CurrencyInputFormatter.fromStoredDecimal(item.total),
                    )
                },
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHero(
            title = "Cupom por imagem ou PDF",
            subtitle = "Capture, reconheça e revise antes de salvar. Nada é gravado sem sua confirmação.",
            icon = Icons.Default.ReceiptLong,
        )

        FlowSectionCard(
            title = "Escolha a origem",
            subtitle = "Use a câmera ou selecione um arquivo já salvo.",
            icon = Icons.Default.CameraAlt,
        ) {
            Button(
                onClick = { camera.launch(null) },
                enabled = !ocrState.reading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Fotografar cupom")
            }

            OutlinedButton(
                onClick = { picker.launch(arrayOf("image/*", "application/pdf")) },
                enabled = !ocrState.reading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Selecionar imagem, print ou PDF")
            }
        }

        AnimatedContent(
            targetState = when {
                ocrState.reading -> "reading"
                ocrState.error != null -> "error"
                ocrState.draft != null -> "review"
                else -> "idle"
            },
            transitionSpec = {
                (fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 5 })
                    .togetherWith(fadeOut(tween(140)) + slideOutVertically(tween(180)) { -it / 8 })
            },
            label = "ocrState",
        ) { state ->
            when (state) {
                "reading" -> FlowSectionCard(
                    title = "Lendo seu cupom",
                    subtitle = "Reconhecendo texto, produtos e dados fiscais.",
                    icon = Icons.Default.Description,
                    accent = MaterialTheme.colorScheme.secondary,
                ) {
                    CircularProgressIndicator()
                    Text("Aguarde alguns instantes. Em seguida você poderá revisar tudo.")
                }

                "error" -> AnimatedInfoCard(
                    visible = true,
                    title = "Não foi possível reconhecer o documento",
                    message = ocrState.error.orEmpty(),
                    accent = MaterialTheme.colorScheme.error,
                )

                "review" -> Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    FlowSectionCard(
                        title = "Revise a compra",
                        subtitle = "Confira estabelecimento, documento e valor antes de analisar os itens.",
                        icon = Icons.Default.FactCheck,
                        accent = MaterialTheme.colorScheme.secondary,
                    ) {
                        EditField("Estabelecimento *", merchant) {
                            merchant = it
                            confirmed = false
                        }
                        EditField("CNPJ", cnpj) {
                            cnpj = it
                            confirmed = false
                        }
                        EditField("Chave de acesso", accessKey) {
                            accessKey = it
                            confirmed = false
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = number,
                                onValueChange = {
                                    number = it
                                    confirmed = false
                                },
                                label = { Text("NFC-e nº") },
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = series,
                                onValueChange = {
                                    series = it
                                    confirmed = false
                                },
                                label = { Text("Série") },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        EditField("Data/hora *", issuedAt) {
                            issuedAt = it
                            confirmed = false
                        }
                        CurrencyTextField(
                            value = total,
                            onValueChange = {
                                total = it
                                confirmed = false
                            },
                            label = { Text("Total da compra") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    FlowSectionCard(
                        title = "Itens identificados",
                        subtitle = "${items.size} item(ns). Corrija apenas o que estiver diferente do cupom.",
                        icon = Icons.Default.ReceiptLong,
                    ) {
                        items.forEachIndexed { index, item ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "Item ${index + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    EditField("Descrição *", item.description) {
                                        items[index] = item.copy(description = it)
                                        confirmed = false
                                    }
                                    EditField("Código", item.code) {
                                        items[index] = item.copy(code = it)
                                        confirmed = false
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = item.quantity,
                                            onValueChange = {
                                                items[index] = item.copy(quantity = it)
                                                confirmed = false
                                            },
                                            label = { Text("Qtd.") },
                                            modifier = Modifier.weight(1f),
                                        )
                                        OutlinedTextField(
                                            value = item.unit,
                                            onValueChange = {
                                                items[index] = item.copy(unit = it)
                                                confirmed = false
                                            },
                                            label = { Text("Unidade") },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CurrencyTextField(
                                            value = item.unitPrice,
                                            onValueChange = {
                                                items[index] = item.copy(unitPrice = it)
                                                confirmed = false
                                            },
                                            label = { Text("Valor unit.") },
                                            modifier = Modifier.weight(1f),
                                        )
                                        CurrencyTextField(
                                            value = item.total,
                                            onValueChange = {
                                                items[index] = item.copy(total = it)
                                                confirmed = false
                                            },
                                            label = { Text("Total") },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            items.removeAt(index)
                                            confirmed = false
                                        },
                                    ) {
                                        Text("Remover item")
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                items.add(OcrItemDraft(description = ""))
                                confirmed = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Adicionar item não identificado")
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (confirmed) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Checkbox(
                                checked = confirmed,
                                onCheckedChange = { confirmed = it },
                            )
                            Text(
                                text = "Revisei e confirmo que as informações acima estão corretas.",
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }

                    Button(
                        enabled = confirmed &&
                            merchant.isNotBlank() &&
                            issuedAt.isNotBlank() &&
                            items.isNotEmpty() &&
                            !saveState.saving,
                        onClick = {
                            viewModel.saveOcrDraft(
                                OcrReceiptDraft(
                                    merchant,
                                    cnpj,
                                    accessKey,
                                    number,
                                    series,
                                    issuedAt,
                                    total,
                                    items.toList(),
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (saveState.saving) "Salvando..." else "Confirmar e salvar no histórico")
                    }

                    saveState.message?.let {
                        AnimatedInfoCard(
                            visible = true,
                            title = "Histórico atualizado",
                            message = it,
                            accent = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    saveState.error?.let {
                        AnimatedInfoCard(
                            visible = true,
                            title = "Não foi possível salvar",
                            message = it,
                            accent = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
    )
}
