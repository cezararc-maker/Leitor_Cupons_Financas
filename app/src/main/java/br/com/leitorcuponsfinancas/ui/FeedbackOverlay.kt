package br.com.leitorcuponsfinancas.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.leitorcuponsfinancas.data.FeedbackItem
import br.com.leitorcuponsfinancas.data.FeedbackManager
import br.com.leitorcuponsfinancas.data.FeedbackStatus

@Composable
fun FeedbackOverlay(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val manager = remember { FeedbackManager.getInstance(context) }
    val state by manager.state.collectAsStateWithLifecycle()

    var hubOpen by rememberSaveable { mutableStateOf(false) }
    var composerOpen by rememberSaveable { mutableStateOf(false) }
    var listOpen by rememberSaveable { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<FeedbackItem?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {},
    )

    LaunchedEffect(manager) {
        manager.start()
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (!state.available) return

    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = { hubOpen = true },
            modifier = Modifier.align(Alignment.Center),
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "Sugestões e melhorias",
            )
        }

        if (state.unreadCount > 0) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = state.unreadCount.coerceAtMost(99).toString(),
                        color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    if (hubOpen) {
        AlertDialog(
            onDismissRequest = { hubOpen = false },
            title = { Text("Ideias e melhorias") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Use este canal para enviar sugestões e acompanhar o andamento.",
                    )
                    Button(
                        onClick = {
                            hubOpen = false
                            composerOpen = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Enviar nova sugestão")
                    }
                    Button(
                        onClick = {
                            hubOpen = false
                            listOpen = true
                            if (state.isAdmin) {
                                manager.markAdminSeen()
                            } else {
                                manager.markUserSeen()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (state.isAdmin) {
                                "Central de solicitações"
                            } else {
                                "Minhas sugestões"
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { hubOpen = false }) {
                    Text("Fechar")
                }
            },
        )
    }

    if (composerOpen) {
        FeedbackComposerDialog(
            submitting = state.submitting,
            onDismiss = { composerOpen = false },
            onSend = { message ->
                manager.submitSuggestion(message)
                composerOpen = false
            },
        )
    }

    if (listOpen) {
        FeedbackListDialog(
            isAdmin = state.isAdmin,
            items = state.items,
            onDismiss = { listOpen = false },
            onEditStatus = { editingItem = it },
        )
    }

    editingItem?.let { item ->
        FeedbackStatusDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { status, reason ->
                manager.updateStatus(
                    feedbackId = item.id,
                    status = status,
                    discardReason = reason,
                )
                editingItem = null
            },
        )
    }

    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = manager::clearMessage,
            title = { Text("Sugestões") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = manager::clearMessage) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
private fun FeedbackComposerDialog(
    submitting: Boolean,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enviar sugestão") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Conte sua ideia, melhoria ou dificuldade encontrada.")
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        if (it.length <= 1500) text = it
                    },
                    label = { Text("Descreva sua sugestão") },
                    supportingText = {
                        Text("${text.length}/1500")
                    },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(text) },
                enabled = !submitting && text.trim().length >= 10,
            ) {
                Text(if (submitting) "Enviando..." else "Enviar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun FeedbackListDialog(
    isAdmin: Boolean,
    items: List<FeedbackItem>,
    onDismiss: () -> Unit,
    onEditStatus: (FeedbackItem) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (isAdmin) {
                        "Central de solicitações"
                    } else {
                        "Minhas sugestões"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                if (items.isEmpty()) {
                    Text(
                        if (isAdmin) {
                            "Nenhuma solicitação recebida."
                        } else {
                            "Você ainda não enviou sugestões."
                        },
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        items(items, key = { it.id }) { item ->
                            FeedbackItemCard(
                                item = item,
                                isAdmin = isAdmin,
                                onEditStatus = onEditStatus,
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Fechar")
                }
            }
        }
    }
}

@Composable
private fun FeedbackItemCard(
    item: FeedbackItem,
    isAdmin: Boolean,
    onEditStatus: (FeedbackItem) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item.status.label,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                if (isAdmin && !item.authorEmail.isNullOrBlank()) {
                    Text(
                        text = item.authorEmail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = item.message,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (
                item.status == FeedbackStatus.DISCARDED &&
                !item.discardReason.isNullOrBlank()
            ) {
                Text(
                    text = "Motivo: ${item.discardReason}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (isAdmin) {
                val technical = listOfNotNull(
                    item.appVersion?.let { "Versão: $it" },
                    item.deviceModel?.let { "Aparelho: $it" },
                ).joinToString(" • ")

                if (technical.isNotBlank()) {
                    Text(
                        text = technical,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Button(
                    onClick = { onEditStatus(item) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Alterar status")
                }
            }
        }
    }
}

@Composable
private fun FeedbackStatusDialog(
    item: FeedbackItem,
    onDismiss: () -> Unit,
    onSave: (FeedbackStatus, String?) -> Unit,
) {
    var selected by remember(item.id) { mutableStateOf(item.status) }
    var reason by remember(item.id) { mutableStateOf(item.discardReason.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alterar status") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FeedbackStatus.entries.forEach { status ->
                    TextButton(
                        onClick = { selected = status },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (selected == status) {
                                "✓ ${status.label}"
                            } else {
                                status.label
                            },
                        )
                    }
                }

                if (selected == FeedbackStatus.DISCARDED) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it.take(500) },
                        label = { Text("Motivo do descarte") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selected, reason) },
                enabled = selected != FeedbackStatus.DISCARDED || reason.isNotBlank(),
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
