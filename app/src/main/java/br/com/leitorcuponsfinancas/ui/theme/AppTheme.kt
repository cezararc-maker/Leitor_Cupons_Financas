package br.com.leitorcuponsfinancas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val FinanceLightColors = lightColorScheme(
    primary = Color(0xFF5A46E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E2FF),
    onPrimaryContainer = Color(0xFF21165F),
    secondary = Color(0xFF008F86),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB9F1EA),
    onSecondaryContainer = Color(0xFF00201D),
    tertiary = Color(0xFFD94B44),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD6),
    onTertiaryContainer = Color(0xFF410002),
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9EAF2),
    outline = Color(0xFF767680),
)

private val FinanceDarkColors = darkColorScheme(
    primary = Color(0xFFC8C0FF),
    onPrimary = Color(0xFF2B1F7A),
    primaryContainer = Color(0xFF43359F),
    onPrimaryContainer = Color(0xFFE7E2FF),
    secondary = Color(0xFF69D9CF),
    onSecondary = Color(0xFF003733),
    secondaryContainer = Color(0xFF00504A),
    onSecondaryContainer = Color(0xFF8FF4EA),
    tertiary = Color(0xFFFFB4AE),
    onTertiary = Color(0xFF690005),
    tertiaryContainer = Color(0xFF93000A),
    onTertiaryContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1117),
    surface = Color(0xFF191B22),
    surfaceVariant = Color(0xFF30323B),
    outline = Color(0xFF90909C),
)

@Composable
fun LeitorCuponsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit,
) {
    val currentDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = fontScale.coerceIn(0.9f, 1.3f),
        ),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) FinanceDarkColors else FinanceLightColors,
            typography = Typography(),
            content = content,
        )
    }
}
