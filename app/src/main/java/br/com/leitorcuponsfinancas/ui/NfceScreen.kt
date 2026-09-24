package br.com.leitorcuponsfinancas.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
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
        ScreenHero(
            title = "Ler NFC-e",
            subtitle = "Aponte a câmera para o QR Code ou use uma imagem/link já disponível.",
            icon = Icons.Default.QrCodeScanner,
        )

        FlowSectionCard(
            title = "Escanear",
            subtitle = "A forma mais rápida para registrar a compra.",
            icon = Icons.Default.QrCodeScanner,
        ) {
            Button(
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
                        "Abrir câmera e escanear"
                    } else {
                        "Câmera não disponível"
                    },
                )
            }

            OutlinedButton(
                onClick = { imagePicker.launch(arrayOf("image/*")) },
                enabled = !imageState.reading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (imageState.reading) {
                        "Lendo imagem..."
                    } else {
                        "Selecionar imagem com QR Code"
                    },
                )
            }

            AnimatedContent(
                targetState = when {
                    imageState.reading -> "reading"
                    imageState.error != null -> "error"
                    imageState.qrText != null -> "success"
                    else -> "idle"
                },
                transitionSpec = {
                    (fadeIn(tween(160)) + slideInVertically(tween(190)) { it / 4 })
                        .togetherWith(fadeOut(tween(120)) + slideOutVertically(tween(150)) { -it / 8 })
                },
                label = "qrImageState",
            ) { state ->
                when (state) {
                    "reading" -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator()
                            Text("Procurando QR Code na imagem...")
                        }
                    }

                    "error" -> AnimatedInfoCard(
                        visible = true,
                        title = "Não foi possível ler a imagem",
                        message = imageState.error.orEmpty(),
                        accent = MaterialTheme.colorScheme.error,
                    )

                    "success" -> AnimatedInfoCard(
                        visible = true,
                        title = "QR Code encontrado",
                        message = "O conteúdo foi carregado e está pronto para validação.",
                        accent = MaterialTheme.colorScheme.secondary,
                    )

                    else -> Unit
                }
            }
        }

        FlowSectionCard(
            title = "Link ou conteúdo da NFC-e",
            subtitle = "Use esta opção quando você já tiver copiado o conteúdo do QR Code.",
            icon = Icons.Default.Key,
            accent = MaterialTheme.colorScheme.secondary,
        ) {
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
                minLines = 3,
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
                Text("Validar NFC-e")
            }

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
                Text("Carregar exemplo de teste")
            }
        }

        localResult?.let { message ->
            AnimatedInfoCard(
                visible = true,
                title = if (localError) "Não foi possível validar" else "NFC-e validada",
                message = message,
                accent = if (localError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.secondary
                },
            )
        }

        cameraPermissionError?.let { error ->
            AnimatedInfoCard(
                visible = true,
                title = "Câmera não autorizada",
                message = error,
                accent = MaterialTheme.colorScheme.error,
            )
        }

        if (duplicateState.checking) {
            FlowSectionCard(
                title = "Verificando histórico",
                subtitle = "Confirmando se esta NFC-e já foi registrada.",
                icon = Icons.Default.ReceiptLong,
            ) {
                CircularProgressIndicator()
            }
        }

        if (duplicateState.alreadyImported) {
            AnimatedInfoCard(
                visible = true,
                title = "NFC-e já importada",
                message = buildString {
                    append("Esta nota já foi importada")
                    duplicateState.formattedFirstImportedAt?.let { importedAt ->
                        append(" em $importedAt")
                    }
                    append(". Nenhum novo lançamento será criado.")
                },
                accent = MaterialTheme.colorScheme.tertiary,
            )
        }

        consultationUrl?.let { url ->
            FlowSectionCard(
                title = "Consultar dados da compra",
                subtitle = "A consulta usa somente o domínio oficial da SEFAZ-MS.",
                icon = Icons.Default.ReceiptLong,
                accent = MaterialTheme.colorScheme.secondary,
            ) {
                Button(
                    enabled = !lookupState.loading,
                    onClick = { nfceViewModel.lookup(url) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (lookupState.loading) {
                            "Consultando SEFAZ-MS..."
                        } else {
                            "Consultar dados públicos"
                        },
                    )
                }

                if (lookupState.loading) {
                    CircularProgressIndicator()
                    Text("Aguardando resposta da consulta pública...")
                }
            }
        }

        lookupState.error?.let { error ->
            FlowSectionCard(
                title = "Consulta automática não concluída",
                subtitle = error,
                icon = Icons.Default.Key,
                accent = MaterialTheme.colorScheme.tertiary,
            ) {
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

                accessKey?.let { key ->
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("Chave de acesso NFC-e", key),
                            )
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.dfe.ms.gov.br/nfce/consulta/"),
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Copiar chave e consultar")
                    }
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
                        else -> "Salvar compra no histórico"
                    },
                )
            }
        }

        saveState.message?.let { message ->
            AnimatedInfoCard(
                visible = true,
                title = "Histórico atualizado",
                message = message,
                accent = MaterialTheme.colorScheme.secondary,
            )
        }

        saveState.error?.let { error ->
            AnimatedInfoCard(
                visible = true,
                title = "Não foi possível salvar",
                message = error,
                accent = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ReceiptCard(receipt: NfceReceipt) {
    FlowSectionCard(
        title = receipt.merchantName ?: "Dados da NFC-e",
        subtitle = listOfNotNull(
            receipt.number?.let { "NFC-e nº $it" },
            receipt.series?.let { "Série $it" },
            receipt.issuedAt,
        ).joinToString(" • ").ifBlank { "Compra identificada" },
        icon = Icons.Default.ReceiptLong,
        accent = MaterialTheme.colorScheme.secondary,
    ) {
        receipt.merchantCnpj?.let {
            Text("CNPJ: $it", style = MaterialTheme.typography.bodySmall)
        }
        receipt.merchantAddress?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }

        receipt.totalAmount?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "TOTAL",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = "R$ ${formatMoney(it)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Text(
            text = "${receipt.items.size} item(ns) encontrados",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        receipt.items.forEachIndexed { index, item ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        "${index + 1}. ${item.description}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    item.code?.let {
                        Text("Código: $it", style = MaterialTheme.typography.bodySmall)
                    }
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

private fun formatMoney(value: BigDecimal): String = value
    .setScale(2, java.math.RoundingMode.HALF_UP)
    .toPlainString()
    .replace(".", ",")

private fun formatNumber(value: BigDecimal): String = value
    .stripTrailingZeros()
    .toPlainString()
    .replace(".", ",")
