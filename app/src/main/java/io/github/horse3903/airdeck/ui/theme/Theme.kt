package io.github.horse3903.airdeck.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.horse3903.airdeck.data.BaseColor
import io.github.horse3903.airdeck.data.ColorScheme
import io.github.horse3903.airdeck.data.getColorFromBaseColor
import androidx.compose.material3.ColorScheme as MaterialColorScheme

/**
 * Creates a dark mode color palette for the given base color.
 */
private fun createDarkColorPalette(@Suppress("UNUSED_PARAMETER") baseColor: BaseColor): MaterialColorScheme {
    return darkColorScheme(
        primary = Color(0xFFF5F5F5),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF1F1F1F),
        onPrimaryContainer = Color(0xFFF5F5F5),
        secondary = Color(0xFFD4D4D4),
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFF262626),
        onSecondaryContainer = Color(0xFFE5E5E5),
        tertiary = Color(0xFFA3A3A3),
        onTertiary = Color(0xFF000000),
        tertiaryContainer = Color(0xFF1A1A1A),
        onTertiaryContainer = Color(0xFFE5E5E5),
        background = Color(0xFF0A0A0C),
        onBackground = Color(0xFFE5E5E5),
        surface = Color(0xFF0A0A0A),
        onSurface = Color(0xFFE5E5E5),
        surfaceVariant = Color(0xFF1A1A1A),
        onSurfaceVariant = Color(0xFFA3A3A3),
        outline = Color(0xFF333333),
        outlineVariant = Color(0xFF262626),
        surfaceTint = Color(0xFFF5F5F5),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF0A0A0A),
        surfaceContainer = Color(0xFF111111),
        surfaceContainerHigh = Color(0xFF161616),
        surfaceContainerHighest = Color(0xFF1A1A1A),
        error = Color(0xFFF87171),
        onError = Color(0xFF000000),
    )
}

/**
 * Creates a light mode color palette for the given base color.
 */
private fun createLightColorPalette(@Suppress("UNUSED_PARAMETER") baseColor: BaseColor): MaterialColorScheme {
    return lightColorScheme(
        primary = Color(0xFF171717),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE5E5E5),
        onPrimaryContainer = Color(0xFF171717),
        secondary = Color(0xFF404040),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFEBEBEB),
        onSecondaryContainer = Color(0xFF262626),
        tertiary = Color(0xFF737373),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF0F0F0),
        onTertiaryContainer = Color(0xFF262626),
        background = Color(0xFFFAFAFA),
        onBackground = Color(0xFF171717),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF171717),
        surfaceVariant = Color(0xFFEFEFEF),
        onSurfaceVariant = Color(0xFF525252),
        outline = Color(0xFFD0D0D0),
        outlineVariant = Color(0xFFE5E5E5),
        surfaceTint = Color(0xFF171717),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F7F7),
        surfaceContainer = Color(0xFFF2F2F2),
        surfaceContainerHigh = Color(0xFFEDEDED),
        surfaceContainerHighest = Color(0xFFE8E8E8),
        error = Color(0xFFDC2626),
        onError = Color(0xFFFFFFFF),
    )
}

@Composable
fun AirDeckTheme(
    darkMode: ColorScheme,
    baseColor: BaseColor,
    content: @Composable () -> Unit
) {

    val darkTheme: Boolean = when (darkMode) {
        ColorScheme.LIGHT -> false
        ColorScheme.DARK -> true
        ColorScheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) {
        createDarkColorPalette(baseColor)
    } else {
        createLightColorPalette(baseColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
