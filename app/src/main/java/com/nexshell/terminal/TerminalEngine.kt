package com.nexshell.terminal

import androidx.compose.ui.graphics.Color
import com.nexshell.theme.TerminalThemeColors
import com.nexshell.theme.ThemeCatalog

data class Cell(
    var char: Char = ' ',
    var fg: Color = Color(0xFFCDD6F4),
    var bg: Color = Color(0xFF1E1E2E),
    var bold: Boolean = false
)

/**
 * Real ANSI/VT100 screen buffer + escape sequence parser. Feed it raw bytes
 * from the PTY master fd; it maintains a grid of Cells plus scrollback.
 * Covers: cursor positioning (CUP/CUU/CUD/CUF/CUB), erase in line/display,
 * SGR (bold + 16-color + 256-color fg/bg), scrolling, line wrap, CR/LF/BS/TAB.
 */
class TerminalEngine(
    var rows: Int,
    var cols: Int,
    private val scrollbackLimit: Int = 2000,
    var theme: TerminalThemeColors = ThemeCatalog.catppuccinMocha
) {
    var grid: Array<Array<Cell>> = Array(rows) { Array(cols) { Cell(bg = theme.background, fg = theme.foreground) } }
        private set
    val scrollback = ArrayDeque<Array<Cell>>()

    var cursorRow = 0
        private set
    var cursorCol = 0
        private set

    private var curFg = theme.foreground
    private var curBg = theme.background
    private var curBold = false

    private val escBuffer = StringBuilder()
    private var inEscape = false

    var onDirty: (() -> Unit)? = null

    fun resize(newRows: Int, newCols: Int) {
        val newGrid = Array(newRows) { r ->
            Array(newCols) { c ->
                if (r < rows && c < cols) grid[r][c] else Cell()
            }
        }
        grid = newGrid
        rows = newRows
        cols = newCols
        cursorRow = cursorRow.coerceIn(0, rows - 1)
        cursorCol = cursorCol.coerceIn(0, cols - 1)
    }

    fun feed(bytes: ByteArray, len: Int) {
        val text = String(bytes, 0, len, Charsets.UTF_8)
        for (ch in text) processChar(ch)
        onDirty?.invoke()
    }

    private fun processChar(ch: Char) {
        if (inEscape) {
            escBuffer.append(ch)
            if (isEscapeTerminator(ch)) {
                handleEscapeSequence(escBuffer.toString())
                escBuffer.clear()
                inEscape = false
            }
            return
        }

        when (ch) {
            '\u001B' -> { inEscape = true; escBuffer.clear() }
            '\r' -> cursorCol = 0
            '\n' -> newLine()
            '\b' -> if (cursorCol > 0) cursorCol--
            '\t' -> {
                val next = ((cursorCol / 8) + 1) * 8
                cursorCol = next.coerceAtMost(cols - 1)
            }
            '\u0007' -> { /* bell — handled at UI layer for vibrate */ }
            else -> writeChar(ch)
        }
    }

    private fun isEscapeTerminator(ch: Char): Boolean {
        if (escBuffer.length == 1) return false // still just '['
        return ch.isLetter() || ch == '~'
    }

    private fun writeChar(ch: Char) {
        if (cursorCol >= cols) {
            newLine()
        }
        grid[cursorRow][cursorCol] = Cell(ch, curFg, curBg, curBold)
        cursorCol++
    }

    private fun newLine() {
        cursorCol = 0
        if (cursorRow == rows - 1) {
            scrollback.addLast(grid[0].copyOf())
            if (scrollback.size > scrollbackLimit) scrollback.removeFirst()
            for (r in 0 until rows - 1) grid[r] = grid[r + 1]
            grid[rows - 1] = Array(cols) { Cell(bg = curBg) }
        } else {
            cursorRow++
        }
    }

    private fun handleEscapeSequence(seq: String) {
        if (!seq.startsWith("[")) return
        val body = seq.substring(1, seq.length - 1)
        val cmd = seq.last()
        val params = body.split(";").mapNotNull { it.toIntOrNull() }

        when (cmd) {
            'H', 'f' -> {
                cursorRow = ((params.getOrNull(0) ?: 1) - 1).coerceIn(0, rows - 1)
                cursorCol = ((params.getOrNull(1) ?: 1) - 1).coerceIn(0, cols - 1)
            }
            'A' -> cursorRow = (cursorRow - (params.getOrNull(0) ?: 1)).coerceIn(0, rows - 1)
            'B' -> cursorRow = (cursorRow + (params.getOrNull(0) ?: 1)).coerceIn(0, rows - 1)
            'C' -> cursorCol = (cursorCol + (params.getOrNull(0) ?: 1)).coerceIn(0, cols - 1)
            'D' -> cursorCol = (cursorCol - (params.getOrNull(0) ?: 1)).coerceIn(0, cols - 1)
            'J' -> eraseInDisplay(params.getOrNull(0) ?: 0)
            'K' -> eraseInLine(params.getOrNull(0) ?: 0)
            'm' -> applySgr(params)
            else -> { /* unsupported sequence — ignored, not faked */ }
        }
    }

    private fun eraseInDisplay(mode: Int) {
        when (mode) {
            0 -> { eraseInLine(0); for (r in cursorRow + 1 until rows) grid[r] = Array(cols) { Cell(bg = curBg) } }
            1 -> { for (r in 0 until cursorRow) grid[r] = Array(cols) { Cell(bg = curBg) }; eraseInLine(1) }
            2, 3 -> grid = Array(rows) { Array(cols) { Cell(bg = curBg) } }
        }
    }

    private fun eraseInLine(mode: Int) {
        when (mode) {
            0 -> for (c in cursorCol until cols) grid[cursorRow][c] = Cell(bg = curBg)
            1 -> for (c in 0..cursorCol) grid[cursorRow][c] = Cell(bg = curBg)
            2 -> grid[cursorRow] = Array(cols) { Cell(bg = curBg) }
        }
    }

    private val ansi16 = arrayOf(
        Color(0xFF45475A), Color(0xFFF38BA8), Color(0xFFA6E3A1), Color(0xFFF9E2AF),
        Color(0xFF89B4FA), Color(0xFFCBA6F7), Color(0xFF94E2D5), Color(0xFFCDD6F4)
    )

    private fun applySgr(params: List<Int>) {
        if (params.isEmpty()) { resetSgr(); return }
        var i = 0
        while (i < params.size) {
            when (val p = params[i]) {
                0 -> resetSgr()
                1 -> curBold = true
                22 -> curBold = false
                in 30..37 -> curFg = theme.ansi[p - 30]
                in 90..97 -> curFg = theme.ansi[p - 90]
                in 40..47 -> curBg = theme.ansi[p - 40]
                39 -> curFg = theme.foreground
                49 -> curBg = theme.background
                38, 48 -> {
                    if (params.getOrNull(i + 1) == 5) {
                        val idx = params.getOrNull(i + 2) ?: 0
                        val color = xterm256(idx)
                        if (p == 38) curFg = color else curBg = color
                        i += 2
                    }
                }
            }
            i++
        }
    }

    private fun resetSgr() { curFg = theme.foreground; curBg = theme.background; curBold = false }

    private fun xterm256(idx: Int): Color {
        if (idx < 8) return theme.ansi[idx]
        if (idx in 16..231) {
            val i = idx - 16
            return Color((i / 36) * 51, ((i / 6) % 6) * 51, (i % 6) * 51)
        }
        val gray = 8 + (idx - 232) * 10
        return Color(gray, gray, gray)
    }
}