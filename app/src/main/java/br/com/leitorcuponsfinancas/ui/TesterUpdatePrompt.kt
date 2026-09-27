package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.leitorcuponsfinancas.data.TesterUpdateManager

@Composable
fun TesterUpdatePrompt(
    onRemindLater: () -> Unit,
) {
    val state by TesterUpdateManager.state.collectAsStateWithLifecycle()
    val release = state.availableRelease

    if (release != null) {
        AlertDialog(
            onDismissRequest = onRemindLater,
            title = {
                Text("Nova atualização disponível")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Versão ${release.displayVersion} (${release.versionCode})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Você pode instalar agora ou pedir para o app lembrar novamente amanhã, na primeira abertura do dia.",
                    )
                    release.releaseNotes
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { notes ->
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                }
            },
            confirmButton = {
                Button(
                    onClick = TesterUpdateManager::installAvailableUpdate,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Instalar agora")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onRemindLater,
                ) {
                    Text("Lembrar mais tarde")
                }
            },
        )
    }

    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = TesterUpdateManager::clearMessage,
            title = { Text("Atualizações") },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = TesterUpdateManager::clearMessage,
                ) {
                    Text("OK")
                }
            },
        )
    }
}
