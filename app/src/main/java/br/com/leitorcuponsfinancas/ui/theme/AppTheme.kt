package br.com.leitorcuponsfinancas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FinanceLightColors = lightColorScheme(
    primary = Color(0xFF5B4BC4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E1FF),
    onPrimaryContainer = Color(0xFF21165D),
    secondary = Color(0xFF53646F),
    secondaryContainer = Color(0xFFD7E4EC),
    background = Color(0xFFF8F9FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8EAF0),
    outline = Color(0xFF777780),
)

private val FinanceDarkColors = darkColorScheme(
    primary = Color(0xFFC7BFFF),
    onPrimary = Color(0xFF2C2174),
    primaryContainer = Color(0xFF44389A),
    onPrimaryContainer = Color(0xFFE7E1FF),
    secondary = Color(0xFFBBC9D3),
    secondaryContainer = Color(0xFF3B4B55),
    background = Color(0xFF111318),
    surface = Color(0xFF191B20),
    surfaceVariant = Color(0xFF303239),
    outline = Color(0xFF909099),
)

@Composable
fun LeitorCuponsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) FinanceDarkColors else FinanceLightColors,
        typography = Typography(),
        content = content,
    )
}
