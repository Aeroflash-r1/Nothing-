package com.nothing.assistant.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

/**
 * Wear Material 3 theme for Nothing Assistant.
 * Uses the dark-baseline ColorScheme defined in Color.kt.
 * Typography uses system default (Roboto Flex on Wear OS 6).
 */
@Composable
fun AssistantTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WearColorScheme,
        content = content
    )
}
