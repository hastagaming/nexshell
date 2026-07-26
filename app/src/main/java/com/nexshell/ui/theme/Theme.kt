package com.nexshell.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NexShellDark = darkColorScheme(
    primary = Color(0xFF89B4FA),
    secondary = Color(0xFFCBA6F7),
    background = Color(0xFF1E1E2E),
    surface = Color(0xFF181825),
    onPrimary = Color(0xFF11111B),
    onBackground = Color(0xFFCDD6F4),
    onSurface = Color(0xFFCDD6F4)
)

private val NexShellLight = lightColorScheme(
    primary = Color(0xFF1E66F5),
    secondary = Color(0xFF8839EF),
    background = Color(0xFFEFF1F5),
    surface = Color(0xFFE6E9EF)
)

@Composable
fun NexShellTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) NexShellDark else NexShellLight,
        typography = MaterialTheme.typography,
        content = content
    )
}