package br.com.leitorcuponsfinancas.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.leitorcuponsfinancas.data.FeedbackItem
import br.com.leitorcuponsfinancas.data.FeedbackManager
import br.com.leitorcuponsfinancas.data.FeedbackStatus
import kotlin.math.roundToInt

@Composable
fun FeedbackOverlay(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val manager = remember { FeedbackManager.getInstance(context) }
    val state by manager.state.collectAsStateWithLifecycle()

    var composerOpen by rememberSaveable { mutableStateOf(false) }
    var listOpen by rememberSaveable { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<FeedbackItem?>(null) }

    val positionPreferences = remember(context) {
        context.getSharedPreferences(
            FEEDBACK_BUTTON_POSITION_PREFERENCES,
            android.content.Context.MODE_PRIVATE,
        )
    }
    val density = LocalDensity.current
    var buttonOffsetX by remember { mutableFloatStateOf(Float.NaN) }
    var buttonOffsetY by remember { mutableFloatStateOf(Float.NaN) }

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

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val buttonContainerSize = 64.dp
        val edgeMargin = 8.dp

        val buttonSizePx = with(density) { buttonContainerSize.toPx() }
        val edgeMarginPx = with(density) { edgeMargin.toPx() }
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val minX = edgeMarginPx
        val minY = edgeMarginPx
        val maxX = (widthPx - buttonSizePx - edgeMarginPx).coerceAtLeast(minX)
        val maxY = (heightPx - buttonSizePx - edgeMarginPx).coerceAtLeast(minY)

        LaunchedEffect(maxX, maxY) {
            val hasSavedPosition =
                positionPreferences.contains(FEEDBACK_BUTTON_POSITION_X) &&
                    positionPreferences.contains(FEEDBACK_BUTTON_POSITION_Y)

            if (hasSavedPosition) {
                val ratioX = positionPreferences
                    .getFloat(FEEDBACK_BUTTON_POSITION_X, 1f)
                    .coerceIn(0f, 1f)
                val ratioY = positionPreferences
                    .getFloat(FEEDBACK_BUTTON_POSITION_Y, 1f)
                    .coerceIn(0f, 1f)

                buttonOffsetX = minX + ((maxX - minX) * ratioX)
                buttonOffsetY = minY + ((maxY - minY) * ratioY)
            } else {
                buttonOffsetX = maxX
                buttonOffsetY = maxY
            }
        }

        val resolvedX = if (buttonOffsetX.isFinite()) {
            buttonOffsetX.coerceIn(minX, maxX)
        } else {
            maxX
        }
        val resolvedY = if (buttonOffsetY.isFinite()) {
            buttonOffsetY.coerceIn(minY, maxY)
        } else {
            maxY
        }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        resolvedX.roundToInt(),
                        resolvedY.roundToInt(),
                    )
                }
                .size(buttonContainerSize)
                .pointerInput(maxX, maxY) {
                    detectDragGestures(
                        onDragStart = {
                            if (!buttonOffsetX.isFinite()) buttonOffsetX = resolvedX
                            if (!buttonOffsetY.isFinite()) buttonOffsetY = resolvedY
                        },
                        onDragEnd = {
                            val xRange = (maxX - minX).coerceAtLeast(1f)
                            val yRange = (maxY - minY).coerceAtLeast(1f)

                            val ratioX = (
                                (buttonOffsetX.coerceIn(minX, maxX) - minX) / xRange
                            ).coerceIn(0f, 1f)
                            val ratioY = (
                                (buttonOffsetY.coerceIn(minY, maxY) - minY) / yRange
                            ).coerceIn(0f, 1f)

                            positionPreferences.edit()
                                .putFloat(FEEDBACK_BUTTON_POSITION_X, ratioX)
                                .putFloat(FEEDBACK_BUTTON_POSITION_Y, ratioY)
                                .apply()
                        },
                    ) { change, dragAmount ->
                        change.consume()

                        val currentX = if (buttonOffsetX.isFinite()) {
                            buttonOffsetX
                        } else {
                            resolvedX
                        }
                        val currentY = if (buttonOffsetY.isFinite()) {
                            buttonOffsetY
                        } else {
                            resolvedY
                        }

                        buttonOffsetX = (currentX + dragAmount.x).coerceIn(minX, maxX)
                        buttonOffsetY = (currentY + dragAmount.y).coerceIn(minY, maxY)
                    }
                },
        ) {
            FloatingActionButton(
                onClick = { composerOpen = true },
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
    }

    if (composerOpen) {
        FeedbackComposerDialog(
            submitting = state.submitting,
            isAdmin = state.isAdmin,
            onDismiss = { composerOpen = false },
            onOpenList = {
                composerOpen = false
                listOpen = true
                if (state.isAdmin) {
                    manager.markAdminSeen()
                } else {
                    manager.markUserSeen()
                }
            },
            onSend = { message ->
                manager.submitSuggestion(message)
                composerOpen = false
            },
        )
    }

    if (listOpen) {
        FeedbackListDialog(
            isAdmin = state.isAdmin,
            feedbackItems = state.items,
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
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onOpenList: () -> Unit,
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
                TextButton(
                    onClick = onOpenList,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        if (isAdmin) {
                            "Central de solicitações"
                        } else {
                            "Acompanhar minhas sugestões"
                        },
                    )
                }
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
    feedbackItems: List<FeedbackItem>,
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

                if (feedbackItems.isEmpty()) {
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
                        items(feedbackItems, key = { it.id }) { item ->
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

private const val FEEDBACK_BUTTON_POSITION_PREFERENCES = "feedback_button_position"
private const val FEEDBACK_BUTTON_POSITION_X = "normalized_x"
private const val FEEDBACK_BUTTON_POSITION_Y = "normalized_y"
