package br.com.leitorcuponsfinancas.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
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
import br.com.leitorcuponsfinancas.data.TaxonomyLevel
import br.com.leitorcuponsfinancas.data.TaxonomyNodeEntity
import br.com.leitorcuponsfinancas.data.TaxonomyTransferManager

private data class TaxonomyRow(
    val node: TaxonomyNodeEntity,
    val depth: Int,
)

@Composable
fun TaxonomyScreen(
    modifier: Modifier = Modifier,
    taxonomyViewModel: TaxonomyViewModel = viewModel(),
) {
    val nodes by taxonomyViewModel.nodes.collectAsStateWithLifecycle()
    val actionState by taxonomyViewModel.actionState.collectAsStateWithLifecycle()

    var addingParent by remember { mutableStateOf<TaxonomyNodeEntity?>(null) }
    var addingLevel by remember { mutableStateOf<TaxonomyLevel?>(null) }
    var editing by remember { mutableStateOf<TaxonomyNodeEntity?>(null) }
    var showManual by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(taxonomyViewModel::importFrom)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        uri?.let(taxonomyViewModel::exportTo)
    }

    val templateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        uri?.let(taxonomyViewModel::exportTemplateTo)
    }

    val rows = remember(nodes) { flattenTaxonomy(nodes) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHero(
                title = "Taxonomia",
                subtitle = "Segmento → Departamento → Categoria → Subcategoria → Produto Mestre.",
                icon = Icons.Default.AccountTree,
            )
        }

        item {
            FlowSectionCard(
                title = "Importar e compartilhar",
                subtitle = "A importação é sempre incremental: não apaga nem sobrescreve cadastros existentes.",
                icon = Icons.Default.AccountTree,
            ) {
                Button(
                    enabled = !actionState.working,
                    onClick = {
                        importLauncher.launch(arrayOf("text/plain", "text/*", "*/*"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Importar tabela hierárquica")
                }

                OutlinedButton(
                    enabled = !actionState.working,
                    onClick = { exportLauncher.launch("taxonomia_lcf.txt") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Exportar minha taxonomia")
                }

                OutlinedButton(
                    enabled = !actionState.working,
                    onClick = { templateLauncher.launch("modelo_taxonomia_lcf.txt") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Exportar modelo TXT")
                }

                TextButton(
                    onClick = { showManual = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Manual do layout de importação")
                }
            }
        }

        actionState.message?.let { message ->
            item {
                AnimatedInfoCard(
                    visible = true,
                    title = "Taxonomia",
                    message = message,
                    accent = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        actionState.error?.let { error ->
            item {
                AnimatedInfoCard(
                    visible = true,
                    title = "Atenção",
                    message = error,
                    accent = MaterialTheme.colorScheme.error,
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    enabled = !actionState.working,
                    onClick = {
                        addingParent = null
                        addingLevel = TaxonomyLevel.SEGMENT
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Novo segmento")
                }

                OutlinedButton(
                    enabled = !actionState.working,
                    onClick = taxonomyViewModel::restoreBase,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Completar base")
                }
            }
        }

        item {
            Text(
                text = "Estrutura atual",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${nodes.size} classificação(ões). Toque para editar e use + para acrescentar o próximo nível.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        items(rows, key = { it.node.id }) { row ->
            val level = TaxonomyLevel.fromCode(row.node.level)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (row.depth * 14).dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editing = row.node }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = row.node.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (row.depth == 0) FontWeight.Bold else FontWeight.Medium,
                        )
                        Text(
                            text = buildString {
                                append(level?.label ?: row.node.level)
                                if (row.node.builtIn) append(" • base do app")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    level?.nextOrNull()?.let { next ->
                        TextButton(
                            onClick = {
                                addingParent = row.node
                                addingLevel = next
                            },
                        ) {
                            Text("+ ${next.label}")
                        }
                    }
                }
            }
        }
    }

    addingLevel?.let { level ->
        var name by remember(level, addingParent?.id) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                addingLevel = null
                addingParent = null
            },
            title = { Text("Adicionar ${level.label}") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    addingParent?.let { parent ->
                        Text(
                            text = "Dentro de: ${parent.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = TextInputRules.capitalizeFirstLetter(it) },
                        label = { Text("Nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank() && !actionState.working,
                    onClick = {
                        taxonomyViewModel.createNode(
                            parent = addingParent,
                            level = level,
                            name = name,
                        )
                        addingLevel = null
                        addingParent = null
                    },
                ) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        addingLevel = null
                        addingParent = null
                    },
                ) {
                    Text("Cancelar")
                }
            },
        )
    }

    editing?.let { node ->
        var name by remember(node.id, node.name) { mutableStateOf(node.name) }
        val label = TaxonomyLevel.fromCode(node.level)?.label ?: "classificação"

        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Editar $label") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = TextInputRules.capitalizeFirstLetter(it) },
                        label = { Text("Nome exibido") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "A chave interna permanece estável. Renomear não quebra vínculos existentes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank() && !actionState.working,
                    onClick = {
                        taxonomyViewModel.renameNode(node, name)
                        editing = null
                    },
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (showManual) {
        AlertDialog(
            onDismissRequest = { showManual = false },
            title = { Text("Manual da Taxonomia LCF") },
            text = {
                LazyColumn {
                    item {
                        Text(
                            text = TaxonomyTransferManager.MANUAL,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManual = false }) {
                    Text("Fechar")
                }
            },
        )
    }
}

private fun flattenTaxonomy(
    nodes: List<TaxonomyNodeEntity>,
): List<TaxonomyRow> {
    val children = nodes.groupBy { it.parentId }
    val result = mutableListOf<TaxonomyRow>()

    fun visit(parentId: Long?, depth: Int) {
        children[parentId]
            .orEmpty()
            .sortedBy { it.name.lowercase() }
            .forEach { node ->
                result += TaxonomyRow(node, depth)
                visit(node.id, depth + 1)
            }
    }

    visit(null, 0)
    return result
}
