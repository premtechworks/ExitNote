package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ExitNoteLightColorScheme = lightColorScheme(
    primary = LightPrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = LightAccentSubtle,
    onPrimaryContainer = LightPrimaryText,
    secondary = LightSuccess,
    onSecondary = Color.White,
    secondaryContainer = LightSuccessSubtle,
    onSecondaryContainer = LightPrimaryText,
    tertiary = LightWarning,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightPrimaryText,
    surface = LightPrimarySurface,
    onSurface = LightPrimaryText,
    surfaceVariant = LightSecondarySurface,
    onSurfaceVariant = LightSecondaryText,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = LightError,
    onError = Color.White,
    errorContainer = LightErrorSubtle,
    onErrorContainer = LightError
)

private val ExitNoteDarkColorScheme = darkColorScheme(
    primary = DarkPrimaryAccent,
    onPrimary = DarkBackground,
    primaryContainer = DarkAccentSubtle,
    onPrimaryContainer = DarkPrimaryText,
    secondary = DarkSuccess,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSuccessSubtle,
    onSecondaryContainer = DarkPrimaryText,
    tertiary = DarkWarning,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkPrimaryText,
    surface = DarkPrimarySurface,
    onSurface = DarkPrimaryText,
    surfaceVariant = DarkSecondarySurface,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = DarkError,
    onError = DarkBackground,
    errorContainer = DarkErrorSubtle,
    onErrorContainer = DarkError
)

val ExitNoteShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

object ExitNoteTheme {
    val colors: ExitNoteExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExitNoteColors.current
}

@Composable
fun ExitNoteAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val extendedColors = if (darkTheme) DarkExitNoteColors else LightExitNoteColors
    val colorScheme = if (darkTheme) ExitNoteDarkColorScheme else ExitNoteLightColorScheme

    CompositionLocalProvider(LocalExitNoteColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = ExitNoteShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ExitNoteAppTheme(darkTheme = darkTheme, content = content)
}
