package com.nexshell.ui.terminal

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.nexshell.extrakeys.ExtraKeysState
import com.nexshell.terminal.TerminalSessionHolder
import com.termux.terminal.TerminalSession
import androidx.core.view.doOnLayout
import com.termux.view.TerminalView as TermuxTerminalView
import com.termux.view.TerminalViewClient

@Composable
fun TerminalView(
    session: TerminalSessionHolder,
    extraKeysState: ExtraKeysState,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            TermuxTerminalView(context, null).apply {
                setTextSize(38)
                setTerminalViewClient(buildViewClient(this, extraKeysState))
                attachSession(session.termuxSession)
                doOnLayout {
                    val paint = android.graphics.Paint().apply {
                        typeface = android.graphics.Typeface.MONOSPACE
                        textSize = 38f
                    }
                    val cellWidth = paint.measureText("X").toInt().coerceAtLeast(1)
                    val cellHeight = paint.fontSpacing.toInt().coerceAtLeast(1)
                    val cols = (width / cellWidth).coerceAtLeast(1)
                    val rows = (height / cellHeight).coerceAtLeast(1)
                    session.resize(cols, rows, cellWidth, cellHeight)
                }
                requestFocus()
            }
        },
        update = { view ->
            if (view.currentSession != session.termuxSession) {
                view.attachSession(session.termuxSession)
            }
        }
    )
}

private fun buildViewClient(view: TermuxTerminalView, extraKeysState: ExtraKeysState): TerminalViewClient {
    return object : TerminalViewClient {
        override fun onScale(scale: Float): Float = scale.coerceIn(0.5f, 2.5f)

        override fun onSingleTapUp(e: android.view.MotionEvent?) {
            view.requestFocus()
            val imm = view.context.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
            imm?.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        override fun shouldBackButtonBeMappedToEscape(): Boolean = false
        override fun shouldEnforceCharBasedInput(): Boolean = true
        override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
        override fun isTerminalViewSelected(): Boolean = true
        override fun copyModeChanged(copyMode: Boolean) { /* selection mode — no extra UI hook needed yet */ }

        override fun onKeyDown(keyCode: Int, e: android.view.KeyEvent?, session: TerminalSession?): Boolean = false
        override fun onKeyUp(keyCode: Int, e: android.view.KeyEvent?): Boolean = false
        override fun onLongPress(event: android.view.MotionEvent?): Boolean = false

        // These three are the real bridge: a physical keyboard combo (or a
        // tap on the ExtraKeysRow's CTRL/ALT/SHIFT button) both funnel
        // through the same latch state, exactly like Termux's own model.
        override fun readControlKey(): Boolean = extraKeysState.ctrlActive
        override fun readAltKey(): Boolean = extraKeysState.altActive
        override fun readShiftKey(): Boolean = extraKeysState.shiftActive
        override fun readFnKey(): Boolean = extraKeysState.fnActive

        override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?): Boolean = false
        override fun onEmulatorSet() { /* fires once TerminalEmulator is attached — no extra setup needed */ }

        override fun logError(tag: String?, message: String?) { android.util.Log.e(tag, message ?: "") }
        override fun logWarn(tag: String?, message: String?) { android.util.Log.w(tag, message ?: "") }
        override fun logInfo(tag: String?, message: String?) { android.util.Log.i(tag, message ?: "") }
        override fun logDebug(tag: String?, message: String?) { android.util.Log.d(tag, message ?: "") }
        override fun logVerbose(tag: String?, message: String?) { android.util.Log.v(tag, message ?: "") }
        override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) { android.util.Log.e(tag, message ?: "", e) }
        override fun logStackTrace(tag: String?, e: Exception?) { android.util.Log.e(tag, "", e) }
    }
}