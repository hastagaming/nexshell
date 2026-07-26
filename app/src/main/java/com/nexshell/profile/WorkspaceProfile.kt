package com.nexshell.profile

data class WorkspaceProfile(
    val name: String,
    val shell: String = "/bin/sh",
    val environmentOverrides: Map<String, String> = emptyMap(),
    val startupCommand: String = "",
    val extraKeysOverride: List<List<String>>? = null,
    val fontFamilyOverride: String? = null,
    val themeOverride: String? = null
) {
    fun toIniBlock(): String = buildString {
        appendLine("[profile.$name]")
        appendLine("shell = $shell")
        appendLine("startup-command = $startupCommand")
        if (fontFamilyOverride != null) appendLine("font-family = $fontFamilyOverride")
        if (themeOverride != null) appendLine("theme = $themeOverride")
        environmentOverrides.forEach { (k, v) -> appendLine("env.$k = $v") }
    }

    companion object {
        fun parseAll(text: String): List<WorkspaceProfile> {
            val profiles = mutableListOf<WorkspaceProfile>()
            var currentName: String? = null
            var shell = "/bin/sh"
            var startup = ""
            var font: String? = null
            var theme: String? = null
            val env = mutableMapOf<String, String>()

            fun flush() {
                val name = currentName ?: return
                profiles += WorkspaceProfile(name, shell, env.toMap(), startup, null, font, theme)
            }

            text.lineSequence().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEach

                val sectionMatch = Regex("""^\[profile\.(.+)]$""").find(line)
                if (sectionMatch != null) {
                    flush()
                    currentName = sectionMatch.groupValues[1]
                    shell = "/bin/sh"; startup = ""; font = null; theme = null; env.clear()
                    return@forEach
                }

                val idx = line.indexOf('=')
                if (idx <= 0) return@forEach
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                when {
                    key == "shell" -> shell = value
                    key == "startup-command" -> startup = value
                    key == "font-family" -> font = value
                    key == "theme" -> theme = value
                    key.startsWith("env.") -> env[key.removePrefix("env.")] = value
                }
            }
            flush()
            return profiles
        }
    }
}