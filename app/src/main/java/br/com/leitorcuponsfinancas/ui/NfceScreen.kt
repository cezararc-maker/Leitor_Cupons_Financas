package br.com.leitorcuponsfinancas.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    val lookupState by nfceViewModel.lookupState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Button(onClick = onBack) {
            Text("Voltar")
        }

        Text(
            text = "Ler NFC-e",
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(
            text = "No desktop, cole o link ou conteúdo do QR Code. A câmera e a leitura por imagem serão adicionadas nas próximas etapas.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedButton(
            onClick = { },
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Escanear com câmera — disponível no celular")
        }

        OutlinedButton(
            onClick = { },
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Selecionar imagem com QR Code — próxima etapa")
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
                nfceViewModel.clearLookup()
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
                nfceViewModel.clearLookup()
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
                        nfceViewModel.clearLookup()
                    }

                    is NfceQrParseResult.Error -> {
                        localResult = parsed.message
                        localError = true
                        consultationUrl = null
                        nfceViewModel.clearLookup()
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
        }

        lookupState.receipt?.let { receipt ->
            ReceiptCard(receipt)
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
