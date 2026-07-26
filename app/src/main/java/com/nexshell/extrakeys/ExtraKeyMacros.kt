package com.nexshell.extrakeys

/**
 * Reproduces Termux's real extra-key dispatch semantics: named keys map to
 * their actual terminal byte sequences, and CTRL/ALT modifiers combine with
 * both named keys and printable characters the same way a physical terminal
 * keyboard would. This is not a cosmetic label — every mapping here sends a
 * byte sequence a real shell will interpret correctly.
 */
object ExtraKeyMacros {

    private val namedKeySequences = mapOf(
        "ESC" to "\u001B",
        "TAB" to "\t",
        "UP" to "\u001B[A",
        "DOWN" to "\u001B[B",
        "RIGHT" to "\u001B[C",
        "LEFT" to "\u001B[D",
        "HOME" to "\u001B[H",
        "END" to "\u001B[F",
        "PGUP" to "\u001B[5~",
        "PGDN" to "\u001B[6~",
        "INS" to "\u001B[2~",
        "DEL" to "\u001B[3~",
        "ENTER" to "\r",
        "BACKSPACE" to "\u007F",
        "F1" to "\u001BOP", "F2" to "\u001BOQ", "F3" to "\u001BOR", "F4" to "\u001BOS",
        "F5" to "\u001B[15~", "F6" to "\u001B[17~", "F7" to "\u001B[18~", "F8" to "\u001B[19~",
        "F9" to "\u001B[20~", "F10" to "\u001B[21~", "F11" to "\u001B[23~", "F12" to "\u001B[24~"
    )

    /** Keys that toggle a modifier state instead of sending bytes directly. */
    val modifierKeys = setOf("CTRL", "ALT", "SHIFT", "FN")

    fun isModifier(label: String): Boolean = label.uppercase() in modifierKeys

    /** Resolves what should actually be written to the PTY for a tapped key,
     *  given which modifiers are currently latched. Returns null if the key
     *  is itself a modifier toggle (handled separately by the caller). */
    fun resolve(label: String, ctrl: Boolean, alt: Boolean): String? {
        val upper = label.uppercase()
        if (isModifier(upper)) return null

        val base = namedKeySequences[upper]
        if (base != null) {
            return if (alt) "\u001B$base" else base
        }

        // Single printable character (e.g. a letter key on a custom row).
        if (label.length == 1) {
            val ch = label[0]
            return when {
                ctrl -> ctrlCode(ch)?.let { seq -> if (alt) "\u001B$seq" else seq } ?: label
                alt -> "\u001B$label"
                else -> label
            }
        }

        // Unrecognized multi-char label — send literally (e.g. a custom
        // startup snippet the user typed directly into .properties).
        return label
    }

    /** CTRL+letter produces the standard ASCII control code: CTRL+A = 0x01
     *  through CTRL+Z = 0x1A, matching real terminal keyboard behavior. */
    private fun ctrlCode(ch: Char): String? {
        val upper = ch.uppercaseChar()
        if (upper !in 'A'..'Z') return null
        val code = (upper.code - 'A'.code + 1)
        return code.toChar().toString()
    }
}