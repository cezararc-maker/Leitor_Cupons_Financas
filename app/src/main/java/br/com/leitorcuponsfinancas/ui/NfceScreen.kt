package br.com.leitorcuponsfinancas.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.leitorcuponsfinancas.domain.NfceQrParseResult
import br.com.leitorcuponsfinancas.domain.NfceQrParser
import br.com.leitorcuponsfinancas.domain.NfceReceipt
import java.math.BigDecimal

@Composable
fun NfceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    nfceViewModel: NfceViewModel = viewModel(),
) {
    var qrText by rememberSaveable { mutableStateOf("") }
    var localResult by rememberSaveable { mutableStateOf<String?>(null) }
    var localError by rememberSaveable { mutableStateOf(false) }
    var consultationUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var accessKey by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraOpen by rememberSaveable { mutableStateOf(false) }
    var cameraPermissionError by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val hasCamera = context.packageManager.hasSystemFeature(
        PackageManager.FEATURE_CAMERA_ANY,
    )

    val lookupState by nfceViewModel.lookupState.collectAsStateWithLifecycle()
    val saveState by nfceViewModel.saveState.collectAsStateWithLifecycle()
    val imageState by nfceViewModel.imageState.collectAsStateWithLifecycle()
    val duplicateState by nfceViewModel.duplicateState.collectAsStateWithLifecycle()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            cameraPermissionError = null
            cameraOpen = true
        } else {
            cameraPermissionError =
                "Permissão de câmera negada. Autorize a câmera para escanear o QR Code."
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            localResult = null
            localError = false
            consultationUrl = null
            accessKey = null
            nfceViewModel.clearLookup()
            nfceViewModel.clearDuplicateState()
            nfceViewModel.readQrImage(uri)
        }
    }

    LaunchedEffect(imageState.qrText) {
        imageState.qrText?.let { text ->
            qrText = text
            localResult = null
            localError = false
            consultationUrl = null
            accessKey = null
            nfceViewModel.clearDuplicateState()
        }
    }

    if (cameraOpen) {
        QrCameraScreen(
            onQrFound = { value ->
                cameraOpen = false
                cameraPermissionError = null
                qrText = value
                localResult = null
                localError = false
                consultationUrl = null
                accessKey = null
                nfceViewModel.clearLookup()
                nfceViewModel.clearImageState()
                nfceViewModel.clearDuplicateState()
            },
            onCancel = {
                cameraOpen = false
            },
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BackArrowButton(onClick = onBack)

        Text(
            text = "Ler NFC-e",
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(
            text = "Escaneie pela câmera, selecione uma imagem com o QR Code ou cole o link da NFC-e.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedButton(
            onClick = {
                cameraPermissionError = null
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) {
                    cameraOpen = true
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            enabled = hasCamera,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (hasCamera) {
                    "Escanear com câmera"
                } else {
                    "Câmera não disponível neste dispositivo"
                },
            )
        }

        cameraPermissionError?.let { error ->
            MessageCard(
                title = "Câmera não autorizada",
                message = error,
            )
        }

        OutlinedButton(
            onClick = { imagePicker.launch(arrayOf("image/*")) },
            enabled = !imageState.reading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (imageState.reading) {
                    "Lendo QR Code da imagem..."
                } else {
                    "Selecionar imagem com QR Code"
                },
            )
        }

        if (imageState.reading) {
            CircularProgressIndicator()
        }

        imageState.qrText?.let {
            Text(
                text = "QR Code encontrado na imagem e carregado no campo abaixo.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        imageState.error?.let { error ->
            MessageCard(
                title = "Não foi possível ler a imagem",
                message = error,
            )
        }

        Text(
            text = "Ou cole o conteúdo do QR Code:",
            style = MaterialTheme.typography.titleMedium,
        )

        OutlinedButton(
            onClick = {
                qrText = "http://www.dfe.ms.gov.br/nfce/qrcode?p=50260912345678000195650010000001231123456783|3|1"
                localResult = null
                localError = false
                consultationUrl = null
                accessKey = null
                nfceViewModel.clearLookup()
                nfceViewModel.clearImageState()
                nfceViewModel.clearDuplicateState()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Usar exemplo de teste")
        }

        Text(
            text = "O exemplo é sintético e serve apenas para validar o parser local.",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(
            value = qrText,
            onValueChange = {
                qrText = it
                localResult = null
                localError = false
                consultationUrl = null
                accessKey = null
                nfceViewModel.clearLookup()
                nfceViewModel.clearImageState()
                nfceViewModel.clearDuplicateState()
            },
            label = { Text("URL ou conteúdo da NFC-e") },
            placeholder = { Text("https://.../nfce/qrcode?p=...") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            enabled = qrText.isNotBlank(),
            onClick = {
                when (val parsed = NfceQrParser.parse(qrText)) {
                    is NfceQrParseResult.Success -> {
                        val data = parsed.data
                        localResult = buildString {
                            appendLine("QR Code válido")
                            appendLine("Versão: ${data.qrVersion}")
                            appendLine("Ambiente: ${data.environment.label}")
                            appendLine("Emissão: ${data.emissionMode.label}")
                            appendLine("UF: Mato Grosso do Sul")
                            append("Chave: ${data.accessKey}")
                            data.issueDay?.let {
                                appendLine()
                                append("Dia da emissão: $it")
                            }
                            data.totalValue?.let {
                                appendLine()
                                append("Valor total: R$ ${formatMoney(it)}")
                            }
                        }
                        localError = false
                        consultationUrl = data.consultationUrl
                        accessKey = data.accessKey
                        nfceViewModel.clearLookup()
                        nfceViewModel.checkDuplicate(data.accessKey)
                    }

                    is NfceQrParseResult.Error -> {
                        localResult = parsed.message
                        localError = true
                        consultationUrl = null
                        accessKey = null
                        nfceViewModel.clearLookup()
                        nfceViewModel.clearDuplicateState()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Processar NFC-e")
        }

        localResult?.let { message ->
            MessageCard(
                title = if (localError) "Não foi possível validar" else "Validação do QR Code",
                message = message,
            )
        }

        if (duplicateState.checking) {
            Text(
                text = "Verificando se esta NFC-e já foi importada...",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (duplicateState.alreadyImported) {
            MessageCard(
                title = "NFC-e já importada",
                message = buildString {
                    append("Esta nota já foi importada")
                    duplicateState.formattedFirstImportedAt?.let { importedAt ->
                        append(" em $importedAt")
                    }
                    append(". Nenhum novo lançamento será criado.")
                },
            )
        }

        consultationUrl?.let { url ->
            Button(
                enabled = !lookupState.loading,
                onClick = { nfceViewModel.lookup(url) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (lookupState.loading) "Consultando SEFAZ-MS..." else "Consultar dados públicos na SEFAZ-MS")
            }
        }

        if (lookupState.loading) {
            CircularProgressIndicator()
            Text(
                text = "Aguardando resposta da consulta pública...",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        lookupState.error?.let { error ->
            MessageCard(
                title = "Consulta pública não concluída",
                message = error,
            )

            consultationUrl?.let { url ->
                OutlinedButton(
                    onClick = {
                        val browserUrl = url
                            .replace("http://", "https://", ignoreCase = true)
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(browserUrl),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Abrir consulta no navegador")
                }
            }
        }

        lookupState.receipt?.let { receipt ->
            ReceiptCard(receipt)

            Button(
                enabled = accessKey != null &&
                    !saveState.saving &&
                    !duplicateState.checking &&
                    !duplicateState.alreadyImported,
                onClick = {
                    accessKey?.let(nfceViewModel::saveReceipt)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        saveState.saving -> "Salvando NFC-e..."
                        duplicateState.alreadyImported -> "NFC-e já importada"
                        else -> "Salvar NFC-e no histórico"
                    },
                )
            }
        }

        saveState.message?.let { message ->
            MessageCard(
                title = "Histórico atualizado",
                message = message,
            )
        }

        saveState.error?.let { error ->
            MessageCard(
                title = "Não foi possível salvar",
                message = error,
            )
        }

        Text(
            text = "A consulta automática usa somente o domínio oficial dfe.ms.gov.br. Se o portal solicitar validação adicional, o app não tenta contorná-la.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun MessageCard(
    title: String,
    message: String,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ReceiptCard(receipt: NfceReceipt) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Dados da NFC-e", style = MaterialTheme.typography.titleLarge)

            receipt.merchantName?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            receipt.merchantCnpj?.let { Text("CNPJ: $it") }
            receipt.merchantAddress?.let { Text(it) }

            val noteInfo = listOfNotNull(
                receipt.number?.let { "NFC-e nº $it" },
                receipt.series?.let { "Série $it" },
                receipt.issuedAt,
            ).joinToString(" • ")
            if (noteInfo.isNotBlank()) Text(noteInfo)

            receipt.totalAmount?.let {
                Text(
                    text = "Valor a pagar: R$ ${formatMoney(it)}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Text(
                text = "Itens encontrados: ${receipt.items.size}",
                style = MaterialTheme.typography.titleMedium,
            )

            receipt.items.forEachIndexed { index, item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text("${index + 1}. ${item.description}", style = MaterialTheme.typography.titleSmall)
                        item.code?.let { Text("Código: $it", style = MaterialTheme.typography.bodySmall) }
                        Text(
                            listOfNotNull(
                                item.quantity?.let { "Qtd.: ${formatNumber(it)}" },
                                item.unit?.let { "UN: $it" },
                                item.unitPrice?.let { "Unit.: R$ ${formatMoney(it)}" },
                                item.total?.let { "Total: R$ ${formatMoney(it)}" },
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun formatMoney(value: BigDecimal): String = value
    .setScale(2, java.math.RoundingMode.HALF_UP)
    .toPlainString()
    .replace(".", ",")

private fun formatNumber(value: BigDecimal): String = value
    .stripTrailingZeros()
    .toPlainString()
    .replace(".", ",")
