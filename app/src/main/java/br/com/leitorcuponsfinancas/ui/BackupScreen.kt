package br.com.leitorcuponsfinancas.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.leitorcuponsfinancas.data.AppBackupManager
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@Composable
fun BackupScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val manager = remember { AppBackupManager(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var restored by remember { mutableStateOf(false) }

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            status = "Criando backup..."
            manager.createBackup(uri)
                .onSuccess { status = "Backup criado com sucesso." }
                .onFailure { status = "Não foi possível criar o backup: ${it.message}" }
            busy = false
        }
    }
    val chooseBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestore = uri
    }

    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BackArrowButton(onClick = onBack)
        Text("Backup e segurança", style = MaterialTheme.typography.headlineSmall)
        Text("O backup recupera toda a base do aplicativo em outro aparelho: produtos, vínculos aprendidos, NFC-e e itens, lançamentos manuais, correções, autoria, perfil local e configurações relevantes.")
        Text(
            "A futura exportação XLSX será apenas para consulta no Excel e não substituirá este backup.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            enabled = !busy,
            onClick = { createBackup.launch(manager.suggestedFileName()) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Criar backup") }
        OutlinedButton(
            enabled = !busy,
            onClick = { chooseBackup.launch(arrayOf("application/zip", "application/octet-stream")) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Restaurar backup") }
        status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (restored) {
            Button(onClick = { restartApplication(context as Activity) }, modifier = Modifier.fillMaxWidth()) {
                Text("Reiniciar aplicativo")
            }
        }
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restaurar este backup?") },
            text = { Text("A base atual deste aparelho será substituída. O arquivo é validado antes da troca e o banco atual é protegido durante a operação.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    scope.launch {
                        busy = true
                        status = "Validando e restaurando backup..."
                        manager.restoreBackup(uri)
                            .onSuccess { restored = true; status = "Backup restaurado. Reinicie o aplicativo para carregar os dados." }
                            .onFailure { status = "Não foi possível restaurar: ${it.message}" }
                        busy = false
                    }
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancelar") } },
        )
    }
}

private fun restartApplication(activity: Activity) {
    val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    if (intent != null) activity.startActivity(intent)
    activity.finishAffinity()
    exitProcess(0)
}
