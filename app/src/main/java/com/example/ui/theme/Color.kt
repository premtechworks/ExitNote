package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Theme Tokens (Master Specification)
val LightBackground = Color(0xFFF7F7F8)
val LightPrimarySurface = Color(0xFFFFFFFF)
val LightSecondarySurface = Color(0xFFF1F1F4)
val LightPrimaryText = Color(0xFF151519)
val LightSecondaryText = Color(0xFF777782)
val LightPrimaryAccent = Color(0xFF7958F5)
val LightAccentSubtle = Color(0xFFEEE9FF)
val LightBorder = Color(0xFFE8E8ED)
val LightSuccess = Color(0xFF16A36A)
val LightSuccessSubtle = Color(0xFFE8F7EF)
val LightWarning = Color(0xFFD99027)
val LightWarningSubtle = Color(0xFFFDF4E7)
val LightError = Color(0xFFDC5656)
val LightErrorSubtle = Color(0xFFFCEEEE)
val LightDisabled = Color(0xFFB8B8C0)

// Dark Theme Tokens (Master Specification)
val DarkBackground = Color(0xFF101115)
val DarkPrimarySurface = Color(0xFF18191F)
val DarkSecondarySurface = Color(0xFF21222B)
val DarkPrimaryText = Color(0xFFF6F6F8)
val DarkSecondaryText = Color(0xFFA1A1AD)
val DarkPrimaryAccent = Color(0xFFA58BFF)
val DarkAccentSubtle = Color(0xFF30274D)
val DarkBorder = Color(0xFF2A2B33)
val DarkSuccess = Color(0xFF4CCB91)
val DarkSuccessSubtle = Color(0xFF163126)
val DarkWarning = Color(0xFFE8B45F)
val DarkWarningSubtle = Color(0xFF352918)
val DarkError = Color(0xFFFF7373)
val DarkErrorSubtle = Color(0xFF391D21)
val DarkDisabled = Color(0xFF555661)

@Immutable
data class ExitNoteExtendedColors(
    val background: Color,
    val primarySurface: Color,
    val secondarySurface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val primaryAccent: Color,
    val accentSubtle: Color,
    val border: Color,
    val success: Color,
    val successSubtle: Color,
    val warning: Color,
    val warningSubtle: Color,
    val error: Color,
    val errorSubtle: Color,
    val disabled: Color,
    val isDark: Boolean
)

val LightExitNoteColors = ExitNoteExtendedColors(
    background = LightBackground,
    primarySurface = LightPrimarySurface,
    secondarySurface = LightSecondarySurface,
    primaryText = LightPrimaryText,
    secondaryText = LightSecondaryText,
    primaryAccent = LightPrimaryAccent,
    accentSubtle = LightAccentSubtle,
    border = LightBorder,
    success = LightSuccess,
    successSubtle = LightSuccessSubtle,
    warning = LightWarning,
    warningSubtle = LightWarningSubtle,
    error = LightError,
    errorSubtle = LightErrorSubtle,
    disabled = LightDisabled,
    isDark = false
)

val DarkExitNoteColors = ExitNoteExtendedColors(
    background = DarkBackground,
    primarySurface = DarkPrimarySurface,
    secondarySurface = DarkSecondarySurface,
    primaryText = DarkPrimaryText,
    secondaryText = DarkSecondaryText,
    primaryAccent = DarkPrimaryAccent,
    accentSubtle = DarkAccentSubtle,
    border = DarkBorder,
    success = DarkSuccess,
    successSubtle = DarkSuccessSubtle,
    warning = DarkWarning,
    warningSubtle = DarkWarningSubtle,
    error = DarkError,
    errorSubtle = DarkErrorSubtle,
    disabled = DarkDisabled,
    isDark = true
)

val LocalExitNoteColors = staticCompositionLocalOf { LightExitNoteColors }
