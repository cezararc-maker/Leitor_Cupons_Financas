package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.leitorcuponsfinancas.data.AppPreferences

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    onFontScaleChange: (Float) -> Unit,
    onShowTipsChange: (Boolean) -> Unit,
    onRestartTutorial: () -> Unit,
    onResetTips: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
    ) {
        item {
            Text(
                text = "Configurações",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Personalize a leitura e a experiência do aplicativo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Tamanho dos textos",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    listOf(
                        0.9f to "Compacto",
                        1.0f to "Padrão",
                        1.15f to "Grande",
                        1.3f to "Muito grande",
                    ).forEach { (scale, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = kotlin.math.abs(preferences.fontScale - scale) < 0.01f,
                                onClick = { onFontScaleChange(scale) },
                            )
                            Text(label)
                        }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Dicas durante o uso",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Mostra explicações rápidas na primeira vez que você acessa funções importantes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = preferences.showContextualTips,
                            onCheckedChange = onShowTipsChange,
                        )
                    }

                    Button(
                        onClick = onResetTips,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Mostrar novamente as dicas")
                    }

                    Button(
                        onClick = onRestartTutorial,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Reabrir tutorial inicial")
                    }
                }
            }
        }
    }
}
