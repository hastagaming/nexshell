package com.nexshell.terminal

import android.media.AudioManager
import android.media.ToneGenerator
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

/**
 * Bridges a real com.termux.terminal.TerminalSession (genuine PTY +
 * VT100/xterm emulation) to NexShell's own UI state. This is the actual
 * Termux terminal engine — not a reimplementation.
 */
class NexShellSessionClient(private val onScreenUpdate: () -> Unit) : TerminalSessionClient {

    private var toneGenerator: ToneGenerator? = null

    override fun onTextChanged(changedSession: TerminalSession) = onScreenUpdate()

    override fun onTitleChanged(changedSession: TerminalSession) { /* workspace label stays user-defined */ }

    override fun onSessionFinished(finishedSession: TerminalSession) = onScreenUpdate()

    override fun onCopyTextToClipboard(session: TerminalSession, text: String?) { /* wired to Android clipboard in TerminalView */ }

    override fun onPasteTextFromClipboard(session: TerminalSession?) { /* wired to Android clipboard in TerminalView */ }

    override fun onBell(session: TerminalSession) {
        runCatching {
            toneGenerator = toneGenerator ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        }
    }

    override fun onColorsChanged(changedSession: TerminalSession) = onScreenUpdate()

    override fun onTerminalCursorStateChange(state: Boolean) = onScreenUpdate()

    override fun setTerminalShellPid(session: TerminalSession, pid: Int) { /* pid tracked by ServiceManager separately if registered as a service */ }

    override fun getTerminalCursorStyle(): Int = TerminalEmulator.DEFAULT_TERMINAL_CURSOR_STYLE

    override fun logError(tag: String?, message: String?) { android.util.Log.e(tag ?: "NexShell", message ?: "") }
    override fun logWarn(tag: String?, message: String?) { android.util.Log.w(tag ?: "NexShell", message ?: "") }
    override fun logInfo(tag: String?, message: String?) { android.util.Log.i(tag ?: "NexShell", message ?: "") }
    override fun logDebug(tag: String?, message: String?) { android.util.Log.d(tag ?: "NexShell", message ?: "") }
    override fun logVerbose(tag: String?, message: String?) { android.util.Log.v(tag ?: "NexShell", message ?: "") }
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) { android.util.Log.e(tag ?: "NexShell", message ?: "", e) }
    override fun logStackTrace(tag: String?, e: Exception?) { android.util.Log.e(tag ?: "NexShell", "", e) }
}