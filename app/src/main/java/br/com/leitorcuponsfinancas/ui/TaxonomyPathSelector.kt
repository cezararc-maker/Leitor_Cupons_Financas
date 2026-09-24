package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.leitorcuponsfinancas.data.TaxonomyLevel
import br.com.leitorcuponsfinancas.data.TaxonomyNodeEntity

@Composable
fun TaxonomyPathSelector(
    nodes: List<TaxonomyNodeEntity>,
    selectedNodeId: Long?,
    onSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    segmentLockedTo: Long? = null,
) {
    val activeNodes = remember(nodes) { nodes.filter { it.active } }
    val selected = activeNodes.firstOrNull { it.id == selectedNodeId }

    val path = remember(selectedNodeId, activeNodes) {
        selectedNodeId?.let { taxonomyPathForSelector(activeNodes, it) }.orEmpty()
    }

    val options = remember(selectedNodeId, segmentLockedTo, activeNodes) {
        when {
            selectedNodeId == null && segmentLockedTo != null ->
                activeNodes.filter { it.id == segmentLockedTo }

            selectedNodeId == null ->
                activeNodes.filter { it.level == TaxonomyLevel.SEGMENT.code }

            else ->
                activeNodes.filter { it.parentId == selectedNodeId }
        }.sortedBy { it.name.lowercase() }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (path.isNotEmpty()) {
            Text(
                text = path.joinToString(" › ") { it.name },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            val canGoBack = when {
                selected?.parentId == null && segmentLockedTo != null -> false
                else -> true
            }

            if (canGoBack) {
                TextButton(
                    onClick = { onSelected(selected?.parentId) },
                ) {
                    Text(
                        if (selected?.parentId == null) {
                            "Trocar segmento"
                        } else {
                            "Voltar um nível"
                        },
                    )
                }
            }
        } else {
            Text(
                text = if (segmentLockedTo == null) {
                    "Escolha o segmento"
                } else {
                    "Segmento do estabelecimento"
                },
                style = MaterialTheme.typography.titleSmall,
            )
        }

        if (options.isNotEmpty()) {
            val label = when {
                selectedNodeId == null -> "Segmento"
                selected == null -> "Próximo nível"
                else -> TaxonomyLevel
                    .fromCode(selected.level)
                    ?.nextOrNull()
                    ?.label
                    ?: "Classificação"
            }

            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            options.forEach { node ->
                OutlinedButton(
                    onClick = { onSelected(node.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(node.name)
                }
            }
        } else if (selected != null) {
            Text(
                text = "Classificação final selecionada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun taxonomyPathForSelector(
    nodes: List<TaxonomyNodeEntity>,
    nodeId: Long,
): List<TaxonomyNodeEntity> {
    val byId = nodes.associateBy { it.id }
    val result = mutableListOf<TaxonomyNodeEntity>()
    var current = byId[nodeId]

    while (current != null) {
        result += current
        current = current.parentId?.let(byId::get)
    }

    return result.reversed()
}
