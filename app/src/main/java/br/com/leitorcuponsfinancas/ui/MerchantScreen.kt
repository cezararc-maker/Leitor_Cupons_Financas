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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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

@Composable
fun MerchantScreen(
    modifier: Modifier = Modifier,
    merchantViewModel: MerchantViewModel = viewModel(),
) {
    val merchants by merchantViewModel.merchants.collectAsStateWithLifecycle()
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
                subtitle = "${merchants.size} estabelecimento(s) mestre. O CNPJ é usado como identidade quando disponível.",
                icon = Icons.Default.Storefront,
            )
        }

        item {
            FlowSectionCard(
                title = "Como funciona",
                subtitle = "Variações de razão social e nomes de notas podem apontar para um único estabelecimento.",
                icon = Icons.Default.Storefront,
            ) {
                Text(
                    "Você pode ajustar o nome exibido sem alterar o CNPJ das compras já registradas.",
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
                }
            }
        }
    }

    editing?.let { merchant ->
        var name by remember(merchant.id) { mutableStateOf(merchant.displayName) }

        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Editar estabelecimento") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
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
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank() && !editState.saving,
                    onClick = {
                        merchantViewModel.rename(merchant, name)
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
}

private fun formatCnpj(value: String): String {
    val digits = value.filter(Char::isDigit)
    if (digits.length != 14) return value
    return "${digits.substring(0, 2)}.${digits.substring(2, 5)}.${digits.substring(5, 8)}/${digits.substring(8, 12)}-${digits.substring(12, 14)}"
}
