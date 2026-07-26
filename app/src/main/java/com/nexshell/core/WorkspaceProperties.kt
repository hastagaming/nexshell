package com.nexshell.core

import java.io.File

data class WorkspaceProperties(
    val extraKeys: List<List<String>> = listOf(
        listOf("ESC", "TAB", "CTRL", "ALT", "UP", "DOWN"),
        listOf("HOME", "PGUP", "LEFT", "RIGHT", "PGDN", "END")
    ),
    val fontFamily: String = "JetBrainsMono Nerd Font",
    val fontSize: Int = 14,
    val cursorStyle: String = "block",
    val cursorBlink: Boolean = true,
    val startupCommand: String = "",
    val terminalTranscriptRows: Int = 2000,
    val bellCharacter: String = "vibrate",
    val theme: String = "catppuccin-mocha"
) {
    fun toFileText(): String {
        val extraKeysStr = extraKeys.joinToString(",") { row ->
            "[" + row.joinToString(",") { "'$it'" } + "]"
        }
        return buildString {
            appendLine("extra-keys = [$extraKeysStr]")
            appendLine()
            appendLine("font-family = $fontFamily")
            appendLine("font-size = $fontSize")
            appendLine()
            appendLine("cursor-style = $cursorStyle")
            appendLine("cursor-blink = $cursorBlink")
            appendLine()
            appendLine("startup-command = $startupCommand")
            appendLine()
            appendLine("terminal-transcript-rows = $terminalTranscriptRows")
            appendLine("bell-character = $bellCharacter")
            appendLine()
            appendLine("theme = $theme")
        }
    }

    companion object {
        fun parse(file: File): WorkspaceProperties {
            if (!file.exists()) return WorkspaceProperties()

            val raw = file.readText()
            val map = mutableMapOf<String, String>()

            // Handle the multi-line extra-keys = [[...],[...]] block first,
            // since it contains commas/brackets that break simple line parsing.
            val extraKeysRegex = Regex("""extra-keys\s*=\s*(\[.*?\](?:\s*,\s*\[.*?\])*)""", RegexOption.DOT_MATCHES_ALL)
            val extraKeysMatch = extraKeysRegex.find(raw)
            val extraKeys: List<List<String>> = extraKeysMatch?.let {
                parseExtraKeys(it.groupValues[1])
            } ?: WorkspaceProperties().extraKeys

            val withoutExtraKeys = if (extraKeysMatch != null) {
                raw.removeRange(extraKeysMatch.range)
            } else raw

            withoutExtraKeys.lineSequence().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
                val idx = trimmed.indexOf('=')
                if (idx <= 0) return@forEach
                val key = trimmed.substring(0, idx).trim()
                val value = trimmed.substring(idx + 1).trim()
                map[key] = value
            }

            val defaults = WorkspaceProperties()
            return WorkspaceProperties(
                extraKeys = extraKeys,
                fontFamily = map["font-family"] ?: defaults.fontFamily,
                fontSize = map["font-size"]?.toIntOrNull() ?: defaults.fontSize,
                cursorStyle = map["cursor-style"] ?: defaults.cursorStyle,
                cursorBlink = map["cursor-blink"]?.toBooleanStrictOrNull() ?: defaults.cursorBlink,
                startupCommand = map["startup-command"] ?: defaults.startupCommand,
                terminalTranscriptRows = map["terminal-transcript-rows"]?.toIntOrNull() ?: defaults.terminalTranscriptRows,
                bellCharacter = map["bell-character"] ?: defaults.bellCharacter,
                theme = map["theme"] ?: defaults.theme
            )
        }

        private fun parseExtraKeys(block: String): List<List<String>> {
            // block looks like: [['ESC','TAB',...],['HOME','PGUP',...]]
            val rowRegex = Regex("""\[([^\[\]]*)]""")
            return rowRegex.findAll(block).map { rowMatch ->
                rowMatch.groupValues[1]
                    .split(",")
                    .map { it.trim().trim('\'', '"') }
                    .filter { it.isNotEmpty() }
            }.toList()
        }
    }
}