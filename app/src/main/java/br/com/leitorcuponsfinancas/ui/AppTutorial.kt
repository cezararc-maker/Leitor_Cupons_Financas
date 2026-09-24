package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class TutorialPage(
    val title: String,
    val text: String,
    val icon: ImageVector,
)

@Composable
fun AppTutorialDialog(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
) {
    val pages = remember {
        listOf(
            TutorialPage(
                title = "Bem-vindo ao Leitor Cupons Finanças",
                text = "O aplicativo organiza suas compras a partir dos itens realmente adquiridos e transforma os registros em informações úteis para o seu dia a dia.",
                icon = Icons.Default.ReceiptLong,
            ),
            TutorialPage(
                title = "Adicione compras pelo botão +",
                text = "Use o botão central para escolher QR Code ou chave NFC-e, foto/imagem/PDF ou lançamento manual.",
                icon = Icons.Default.AddCircle,
            ),
            TutorialPage(
                title = "Ensine o aplicativo",
                text = "Produtos mestres, categorias, setores e vínculos aprendidos ajudam o app a reconhecer melhor seus itens e evitam cadastros duplicados.",
                icon = Icons.Default.Inventory2,
            ),
            TutorialPage(
                title = "Acompanhe para onde seu dinheiro vai",
                text = "O dashboard mensal mostra gastos, frequência de compras, estabelecimentos que mais pesaram no bolso e produtos mais comprados ou mais caros.",
                icon = Icons.Default.BarChart,
            ),
        )
    }

    var pageIndex by remember { mutableIntStateOf(0) }
    val page = pages[pageIndex]
    val isLast = pageIndex == pages.lastIndex

    AlertDialog(
        onDismissRequest = {},
        icon = { Icon(page.icon, contentDescription = null) },
        title = { Text(page.title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(page.text)
                Text(
                    text = "\${pageIndex + 1} de \${pages.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isLast) onComplete() else pageIndex += 1
                },
            ) {
                Text(if (isLast) "Começar" else "Próximo")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Pular")
            }
        },
    )
}

@Composable
fun ContextualTipDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onSkipAll: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendi")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkipAll) {
                Text("Pular dicas")
            }
        },
    )
}
