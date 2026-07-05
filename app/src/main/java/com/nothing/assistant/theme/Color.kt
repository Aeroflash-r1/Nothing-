package com.nothing.assistant.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

/**
 * Material 3 color tokens for Nothing Assistant.
 * Dark baseline — black AMOLED background, white text, purple accents.
 *
 * True black (#000000) is used for background so AMOLED pixels stay off in idle/ambient.
 */
val WearColorScheme = ColorScheme(
    // Primary — main accent (buttons, active mic, links)
    primary = Color(0xFFB39DDB),
    onPrimary = Color(0xFF000000),

    // Primary container — assistant chat bubbles
    primaryContainer = Color(0xFF3B2C5E),
    onPrimaryContainer = Color(0xFFF3EAFF),

    // Secondary — subtle highlights
    secondary = Color(0xFFE1BEE7),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF4A3A5C),
    onSecondaryContainer = Color(0xFFF3EAF5),

    // Tertiary
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF4F378B),
    onTertiaryContainer = Color(0xFFEADDFF),

    // Background — true black (AMOLED idle pixels off)
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),

    // Surface — cards, message bubbles (barely lifted from black)
    surface = Color(0xFF0E0B14),
    onSurface = Color(0xFFFFFFFF),

    // Surface variant — secondary surfaces, input fields
    surfaceVariant = Color(0xFF1C1626),
    onSurfaceVariant = Color(0xFFB8B3C4),

    // Outline — dividers, input borders
    outline = Color(0xFF3A3345),
    outlineVariant = Color(0xFF2A2438),

    // Error
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)
