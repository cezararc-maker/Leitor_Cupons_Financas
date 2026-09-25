package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.leitorcuponsfinancas.data.MerchantEntity
import br.com.leitorcuponsfinancas.data.TaxonomyNodeEntity

@Composable
fun MerchantScreen(
    modifier: Modifier = Modifier,
    merchantViewModel: MerchantViewModel = viewModel(),
) {
    val merchants by merchantViewModel.merchants.collectAsStateWithLifecycle()
    val segments by merchantViewModel.segments.collectAsStateWithLifecycle()
    val editState by merchantViewModel.editState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<MerchantEntity?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHero(
                title = "Estabelecimentos",
                subtitle = "${merchants.size} estabelecimento(s) mestre. O segmento limita as opções da taxonomia durante a vinculação.",
                icon = Icons.Default.Storefront,
            )
        }

        item {
            FlowSectionCard(
                title = "Segmento do estabelecimento",
                subtitle = "Mercado, Farmácia, Vestuário e outros segmentos usam árvores diferentes de classificação.",
                icon = Icons.Default.Storefront,
            ) {
                Text(
                    "Depois de classificado, o estabelecimento reaproveita o mesmo segmento nas próximas compras. O CNPJ continua sendo a identidade principal quando disponível.",
                )
            }
        }

        editState.message?.let { message ->
            item {
                AnimatedInfoCard(
                    visible = true,
                    title = "Cadastro atualizado",
                    message = message,
                    accent = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        editState.error?.let { error ->
            item {
                AnimatedInfoCard(
                    visible = true,
                    title = "Não foi possível atualizar",
                    message = error,
                    accent = MaterialTheme.colorScheme.error,
                )
            }
        }

        items(merchants, key = { it.id }) { merchant ->
            val segment = segments.firstOrNull { it.id == merchant.segmentNodeId }

            Card(
                onClick = {
                    merchantViewModel.clearMessage()
                    editing = merchant
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = merchant.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = merchant.cnpjDigits?.let(::formatCnpj)
                            ?: "Sem CNPJ cadastrado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = buildString {
                            append("Segmento: ")
                            append(segment?.name ?: "Não classificado")
                            merchant.segmentSource?.let { source ->
                                append(" • ")
                                append(
                                    when (source) {
                                        "USER" -> "definido pelo usuário"
                                        "TAXONOMY" -> "aprendido pela taxonomia"
                                        "CNAE" -> "sugerido pelo CNAE"
                                        else -> source
                                    },
                                )
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (segment == null) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    merchant.cnaeMain?.let { cnae ->
                        Text(
                            text = "CNAE principal: $cnae",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    editing?.let { merchant ->
        MerchantEditDialog(
            merchant = merchant,
            segments = segments,
            saving = editState.saving,
            onDismiss = { editing = null },
            onSave = { name, segmentId ->
                merchantViewModel.save(
                    merchant = merchant,
                    name = name,
                    segmentNodeId = segmentId,
                )
                editing = null
            },
        )
    }
}

@Composable
private fun MerchantEditDialog(
    merchant: MerchantEntity,
    segments: List<TaxonomyNodeEntity>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Long?) -> Unit,
) {
    var name by remember(merchant.id) { mutableStateOf(merchant.displayName) }
    var segmentId by remember(merchant.id) { mutableStateOf(merchant.segmentNodeId) }
    var choosingSegment by remember(merchant.id) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Editar estabelecimento") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = TextInputRules.capitalizeFirstLetter(it) },
                    label = { Text("Nome exibido") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                merchant.cnpjDigits?.let {
                    Text(
                        text = "CNPJ: ${formatCnpj(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedButton(
                    onClick = { choosingSegment = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Segmento: ${segments.firstOrNull { it.id == segmentId }?.name ?: "Escolher"}",
                    )
                }

                Text(
                    text = "A escolha do segmento controla quais departamentos, categorias e subcategorias aparecerão na vinculação dos produtos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && !saving,
                onClick = { onSave(name, segmentId) },
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !saving,
                onClick = onDismiss,
            ) {
                Text("Cancelar")
            }
        },
    )

    if (choosingSegment) {
        AlertDialog(
            onDismissRequest = { choosingSegment = false },
            title = { Text("Segmento do estabelecimento") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item {
                        OutlinedButton(
                            onClick = {
                                segmentId = null
                                choosingSegment = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Não classificado")
                        }
                    }
                    items(segments, key = { it.id }) { segment ->
                        Button(
                            onClick = {
                                segmentId = segment.id
                                choosingSegment = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(segment.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { choosingSegment = false }) {
                    Text("Fechar")
                }
            },
        )
    }
}

private fun formatCnpj(value: String): String {
    val digits = value.filter(Char::isDigit)
    if (digits.length != 14) return value
    return "${digits.substring(0, 2)}.${digits.substring(2, 5)}.${digits.substring(5, 8)}/${digits.substring(8, 12)}-${digits.substring(12, 14)}"
}
