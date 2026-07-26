package com.nexshell.ui.terminal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.nexshell.font.FontCatalog
import com.nexshell.font.FontFamilyOption
import com.nexshell.font.FontStyle
import com.nexshell.terminal.TerminalSession
import android.graphics.Paint

@Composable
fun TerminalView(
    session: TerminalSession,
    fontFamily: FontFamilyOption,
    fontSizeSp: Int = 14,
    modifier: Modifier = Modifier
) {
    var tick by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val density = LocalDensity.current

    DisposableEffect(session.id) {
        session.engine.onDirty = { tick++ }
        onDispose { session.engine.onDirty = null }
    }

    val paints = remember(fontFamily, fontSizeSp) {
        val sizePx = with(density) { fontSizeSp.sp.toPx() }
        mapOf(
            FontStyle.REGULAR to Paint().apply {
                typeface = FontCatalog.resolveTypeface(context, fontFamily, FontStyle.REGULAR)
                textSize = sizePx; isAntiAlias = true
            },
            FontStyle.BOLD to Paint().apply {
                typeface = FontCatalog.resolveTypeface(context, fontFamily, FontStyle.BOLD)
                textSize = sizePx; isAntiAlias = true
            },
            FontStyle.ITALIC to Paint().apply {
                typeface = FontCatalog.resolveTypeface(context, fontFamily, FontStyle.ITALIC)
                textSize = sizePx; isAntiAlias = true
            },
            FontStyle.BOLD_ITALIC to Paint().apply {
                typeface = FontCatalog.resolveTypeface(context, fontFamily, FontStyle.BOLD_ITALIC)
                textSize = sizePx; isAntiAlias = true
            }
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E2E))
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) handleKeyEvent(event, session) else false
            }
    ) {
        tick
        drawTerminal(session, paints)
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

private fun handleKeyEvent(event: KeyEvent, session: TerminalSession): Boolean {
    val ch = event.utf16CodePoint
    when (event.key) {
        Key.Enter -> { session.sendInput("\r"); return true }
        Key.Backspace -> { session.sendInput("\u007F"); return true }
        Key.Tab -> { session.sendInput("\t"); return true }
        Key.DirectionUp -> { session.sendInput("\u001B[A"); return true }
        Key.DirectionDown -> { session.sendInput("\u001B[B"); return true }
        Key.DirectionRight -> { session.sendInput("\u001B[C"); return true }
        Key.DirectionLeft -> { session.sendInput("\u001B[D"); return true }
        Key.Escape -> { session.sendInput("\u001B"); return true }
        else -> {}
    }
    if (ch > 0) { session.sendInput(String(Character.toChars(ch))); return true }
    return false
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTerminal(
    session: TerminalSession,
    paints: Map<FontStyle, Paint>
) {
    val engine = session.engine
    val regular = paints.getValue(FontStyle.REGULAR)
    val charWidth = regular.measureText("W")
    val lineHeight = regular.fontSpacing

    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        for (row in 0 until engine.rows) {
            for (col in 0 until engine.cols) {
                val cell = engine.grid[row][col]
                if (cell.bg != Color(0xFF1E1E2E)) {
                    val bgPaint = Paint().apply { color = cell.bg.toArgb() }
                    nativeCanvas.drawRect(
                        col * charWidth, row * lineHeight,
                        (col + 1) * charWidth, (row + 1) * lineHeight,
                        bgPaint
                    )
                }
                if (cell.char != ' ') {
                    val style = when {
                        cell.bold && cell.italic -> FontStyle.BOLD_ITALIC
                        cell.bold -> FontStyle.BOLD
                        cell.italic -> FontStyle.ITALIC
                        else -> FontStyle.REGULAR
                    }
                    val paint = paints.getValue(style)
                    paint.color = cell.fg.toArgb()
                    nativeCanvas.drawText(
                        cell.char.toString(),
                        col * charWidth,
                        (row + 1) * lineHeight - paint.descent(),
                        paint
                    )
                }
            }
        }

        val cursorPaint = Paint().apply { color = Color(0x8089B4FA).toArgb() }
        nativeCanvas.drawRect(
            engine.cursorCol * charWidth, engine.cursorRow * lineHeight,
            (engine.cursorCol + 1) * charWidth, (engine.cursorRow + 1) * lineHeight,
            cursorPaint
        )
    }
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)