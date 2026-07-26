package com.nexshell.theme

import androidx.compose.ui.graphics.Color

data class TerminalThemeColors(
    val name: String,
    val background: Color,
    val foreground: Color,
    val ansi: Array<Color> // 8 base ANSI colors, index 0..7
)

object ThemeCatalog {
    val catppuccinMocha = TerminalThemeColors(
        "catppuccin-mocha",
        background = Color(0xFF1E1E2E), foreground = Color(0xFFCDD6F4),
        ansi = arrayOf(
            Color(0xFF45475A), Color(0xFFF38BA8), Color(0xFFA6E3A1), Color(0xFFF9E2AF),
            Color(0xFF89B4FA), Color(0xFFCBA6F7), Color(0xFF94E2D5), Color(0xFFBAC2DE)
        )
    )

    val dracula = TerminalThemeColors(
        "dracula",
        background = Color(0xFF282A36), foreground = Color(0xFFF8F8F2),
        ansi = arrayOf(
            Color(0xFF21222C), Color(0xFFFF5555), Color(0xFF50FA7B), Color(0xFFF1FA8C),
            Color(0xFFBD93F9), Color(0xFFFF79C6), Color(0xFF8BE9FD), Color(0xFFF8F8F2)
        )
    )

    val nord = TerminalThemeColors(
        "nord",
        background = Color(0xFF2E3440), foreground = Color(0xFFD8DEE9),
        ansi = arrayOf(
            Color(0xFF3B4252), Color(0xFFBF616A), Color(0xFFA3BE8C), Color(0xFFEBCB8B),
            Color(0xFF81A1C1), Color(0xFFB48EAD), Color(0xFF88C0D0), Color(0xFFE5E9F0)
        )
    )

    val gruvboxDark = TerminalThemeColors(
        "gruvbox-dark",
        background = Color(0xFF282828), foreground = Color(0xFFEBDBB2),
        ansi = arrayOf(
            Color(0xFF282828), Color(0xFFCC241D), Color(0xFF98971A), Color(0xFFD79921),
            Color(0xFF458588), Color(0xFFB16286), Color(0xFF689D6A), Color(0xFFA89984)
        )
    )

    val all = listOf(catppuccinMocha, dracula, nord, gruvboxDark)

    fun byName(name: String): TerminalThemeColors = all.find { it.name == name } ?: catppuccinMocha
}