package br.com.leitorcuponsfinancas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import br.com.leitorcuponsfinancas.data.AppColorPalette
import br.com.leitorcuponsfinancas.data.AppThemeMode

@Immutable
data class AppVisuals(
    val heroBrush: Brush,
    val accentBrush: Brush,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val tertiaryAccent: Color,
    val onGradient: Color,
)

val LocalAppVisuals = staticCompositionLocalOf {
    AppVisuals(
        heroBrush = Brush.linearGradient(listOf(Color(0xFF5A46E8), Color(0xFF008F86))),
        accentBrush = Brush.linearGradient(listOf(Color(0xFF5A46E8), Color(0xFF7B61FF))),
        primaryAccent = Color(0xFF5A46E8),
        secondaryAccent = Color(0xFF008F86),
        tertiaryAccent = Color(0xFFD94B44),
        onGradient = Color.White,
    )
}

private data class PaletteSpec(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
)

private fun paletteSpec(palette: AppColorPalette): PaletteSpec = when (palette) {
    AppColorPalette.VIOLET -> PaletteSpec(
        primary = Color(0xFF5A46E8),
        secondary = Color(0xFF008F86),
        tertiary = Color(0xFFD94B44),
        gradientStart = Color(0xFF5B3DF5),
        gradientEnd = Color(0xFF1A78F2),
    )

    AppColorPalette.OCEAN -> PaletteSpec(
        primary = Color(0xFF006BD6),
        secondary = Color(0xFF008E9B),
        tertiary = Color(0xFF625BDE),
        gradientStart = Color(0xFF0077E6),
        gradientEnd = Color(0xFF00A8A8),
    )

    AppColorPalette.EMERALD -> PaletteSpec(
        primary = Color(0xFF00875A),
        secondary = Color(0xFF007C91),
        tertiary = Color(0xFF7458D6),
        gradientStart = Color(0xFF00A56A),
        gradientEnd = Color(0xFF00A5A8),
    )

    AppColorPalette.SUNSET -> PaletteSpec(
        primary = Color(0xFFD64A58),
        secondary = Color(0xFF7B55D9),
        tertiary = Color(0xFFF08A32),
        gradientStart = Color(0xFFF05B47),
        gradientEnd = Color(0xFF7A4CE0),
    )

    AppColorPalette.GRAPHITE -> PaletteSpec(
        primary = Color(0xFF4D5B72),
        secondary = Color(0xFF526D82),
        tertiary = Color(0xFF6E5A8A),
        gradientStart = Color(0xFF445269),
        gradientEnd = Color(0xFF65758B),
    )
}

private fun lightScheme(spec: PaletteSpec) = lightColorScheme(
    primary = spec.primary,
    onPrimary = Color.White,
    primaryContainer = spec.primary.copy(alpha = 0.14f).compositeOver(Color.White),
    onPrimaryContainer = Color(0xFF17151F),
    secondary = spec.secondary,
    onSecondary = Color.White,
    secondaryContainer = spec.secondary.copy(alpha = 0.14f).compositeOver(Color.White),
    onSecondaryContainer = Color(0xFF10201F),
    tertiary = spec.tertiary,
    onTertiary = Color.White,
    tertiaryContainer = spec.tertiary.copy(alpha = 0.14f).compositeOver(Color.White),
    onTertiaryContainer = Color(0xFF241313),
    background = Color(0xFFF7F8FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EAF2),
    outline = Color(0xFF747681),
)

private fun darkScheme(spec: PaletteSpec) = darkColorScheme(
    primary = spec.primary.copy(alpha = 0.96f).lighterForDark(),
    onPrimary = Color(0xFF111318),
    primaryContainer = spec.primary.copy(alpha = 0.46f),
    onPrimaryContainer = Color.White,
    secondary = spec.secondary.copy(alpha = 0.96f).lighterForDark(),
    onSecondary = Color(0xFF0D1717),
    secondaryContainer = spec.secondary.copy(alpha = 0.42f),
    onSecondaryContainer = Color.White,
    tertiary = spec.tertiary.copy(alpha = 0.96f).lighterForDark(),
    onTertiary = Color(0xFF201011),
    tertiaryContainer = spec.tertiary.copy(alpha = 0.42f),
    onTertiaryContainer = Color.White,
    background = Color(0xFF0F1117),
    surface = Color(0xFF191B22),
    surfaceVariant = Color(0xFF30323B),
    outline = Color(0xFF90909C),
)

private fun Color.lighterForDark(): Color = Color(
    red = (red + (1f - red) * 0.28f).coerceIn(0f, 1f),
    green = (green + (1f - green) * 0.28f).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * 0.28f).coerceIn(0f, 1f),
    alpha = alpha,
)

private fun Color.compositeOver(background: Color): Color {
    val outA = alpha + background.alpha * (1f - alpha)
    if (outA <= 0f) return Color.Transparent
    return Color(
        red = (red * alpha + background.red * background.alpha * (1f - alpha)) / outA,
        green = (green * alpha + background.green * background.alpha * (1f - alpha)) / outA,
        blue = (blue * alpha + background.blue * background.alpha * (1f - alpha)) / outA,
        alpha = outA,
    )
}

@Composable
fun LeitorCuponsTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    colorPalette: AppColorPalette = AppColorPalette.VIOLET,
    gradientEnabled: Boolean = true,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val spec = paletteSpec(colorPalette)
    val currentDensity = LocalDensity.current

    val heroBrush = if (gradientEnabled) {
        Brush.linearGradient(listOf(spec.gradientStart, spec.gradientEnd))
    } else {
        Brush.linearGradient(listOf(spec.primary, spec.primary))
    }
    val accentBrush = if (gradientEnabled) {
        Brush.linearGradient(listOf(spec.primary, spec.secondary))
    } else {
        Brush.linearGradient(listOf(spec.primary, spec.primary))
    }

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = fontScale.coerceIn(0.9f, 1.3f),
        ),
        LocalAppVisuals provides AppVisuals(
            heroBrush = heroBrush,
            accentBrush = accentBrush,
            primaryAccent = spec.primary,
            secondaryAccent = spec.secondary,
            tertiaryAccent = spec.tertiary,
            onGradient = Color.White,
        ),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkScheme(spec) else lightScheme(spec),
            typography = Typography(),
            content = content,
        )
    }
}
