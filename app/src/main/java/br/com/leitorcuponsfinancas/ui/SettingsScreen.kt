package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.leitorcuponsfinancas.data.AppColorPalette
import br.com.leitorcuponsfinancas.data.AppPreferences
import br.com.leitorcuponsfinancas.data.AppThemeMode

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    onFontScaleChange: (Float) -> Unit,
    onShowTipsChange: (Boolean) -> Unit,
    onRestartTutorial: () -> Unit,
    onResetTips: () -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onColorPaletteChange: (AppColorPalette) -> Unit,
    onGradientEnabledChange: (Boolean) -> Unit,
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
                text = "Personalize a aparência, a leitura e a experiência do aplicativo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Aparência",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Modo do tema",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    listOf(
                        AppThemeMode.SYSTEM to "Seguir o sistema",
                        AppThemeMode.LIGHT to "Claro",
                        AppThemeMode.DARK to "Escuro",
                    ).forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThemeModeChange(mode) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = preferences.themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                            )
                            Text(label)
                        }
                    }

                    Text(
                        text = "Paleta",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    PaletteOption(
                        label = "Violeta",
                        selected = preferences.colorPalette == AppColorPalette.VIOLET,
                        colors = listOf(Color(0xFF5B3DF5), Color(0xFF1A78F2)),
                        onClick = { onColorPaletteChange(AppColorPalette.VIOLET) },
                    )
                    PaletteOption(
                        label = "Oceano",
                        selected = preferences.colorPalette == AppColorPalette.OCEAN,
                        colors = listOf(Color(0xFF0077E6), Color(0xFF00A8A8)),
                        onClick = { onColorPaletteChange(AppColorPalette.OCEAN) },
                    )
                    PaletteOption(
                        label = "Esmeralda",
                        selected = preferences.colorPalette == AppColorPalette.EMERALD,
                        colors = listOf(Color(0xFF00A56A), Color(0xFF00A5A8)),
                        onClick = { onColorPaletteChange(AppColorPalette.EMERALD) },
                    )
                    PaletteOption(
                        label = "Pôr do sol",
                        selected = preferences.colorPalette == AppColorPalette.SUNSET,
                        colors = listOf(Color(0xFFF05B47), Color(0xFF7A4CE0)),
                        onClick = { onColorPaletteChange(AppColorPalette.SUNSET) },
                    )
                    PaletteOption(
                        label = "Grafite",
                        selected = preferences.colorPalette == AppColorPalette.GRAPHITE,
                        colors = listOf(Color(0xFF445269), Color(0xFF65758B)),
                        onClick = { onColorPaletteChange(AppColorPalette.GRAPHITE) },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Usar degradê",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Aplica degradê nos cabeçalhos, botão + e destaques principais.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = preferences.gradientEnabled,
                            onCheckedChange = onGradientEnabledChange,
                        )
                    }
                }
            }
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

@Composable
private fun PaletteOption(
    label: String,
    selected: Boolean,
    colors: List<Color>,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(colors)),
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
    }
}
