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
import br.com.leitorcuponsfinancas.domain.NfceQrParseResult
import br.com.leitorcuponsfinancas.domain.NfceQrParser

@Composable
fun NfceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var qrText by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    var isError by rememberSaveable { mutableStateOf(false) }

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
                result = null
                isError = false
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
                result = null
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
                        result = buildString {
                            appendLine("QR Code válido")
                            appendLine("Versão: ${data.qrVersion}")
                            appendLine("Ambiente: ${data.environment.label}")
                            appendLine("Emissão: ${data.emissionMode.label}")
                            appendLine("UF: Mato Grosso do Sul")
                            append("Chave: ${data.accessKey}")
                            data.issueDay?.let { appendLine(); append("Dia da emissão: $it") }
                            data.totalValue?.let { appendLine(); append("Valor total: R$ ${it.toPlainString()}") }
                        }
                        isError = false
                    }

                    is NfceQrParseResult.Error -> {
                        result = parsed.message
                        isError = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Processar NFC-e")
        }

        result?.let { message ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = if (isError) "Não foi possível validar" else "Resultado",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Text(
            text = "Nesta fase o app valida e interpreta o QR Code; ele ainda não baixa os itens da NFC-e.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
