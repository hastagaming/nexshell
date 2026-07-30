package com.nexshell.terminal

import android.media.AudioManager
import android.media.ToneGenerator
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView

class NexShellSessionClient(private val onScreenUpdate: () -> Unit) : TerminalSessionClient {

    // Set once the real TerminalView attaches this session — required
    // because TerminalView does not auto-refresh; the client must call
    // onScreenUpdated() explicitly, same as upstream Termux does.
    var attachedView: TerminalView? = null

    private var toneGenerator: ToneGenerator? = null

    override fun onTextChanged(changedSession: TerminalSession) {
        attachedView?.onScreenUpdated()
        onScreenUpdate()
    }

    override fun onTitleChanged(changedSession: TerminalSession) { }

    override fun onSessionFinished(finishedSession: TerminalSession) {
        attachedView?.onScreenUpdated()
        onScreenUpdate()
    }

    override fun onCopyTextToClipboard(session: TerminalSession, text: String?) { }
    override fun onPasteTextFromClipboard(session: TerminalSession?) { }

    override fun onBell(session: TerminalSession) {
        runCatching {
            toneGenerator = toneGenerator ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        }
    }

    override fun onColorsChanged(changedSession: TerminalSession) {
        attachedView?.onScreenUpdated()
        onScreenUpdate()
    }

    override fun onTerminalCursorStateChange(state: Boolean) {
        attachedView?.onScreenUpdated()
    }

    override fun setTerminalShellPid(session: TerminalSession, pid: Int) { }
    override fun getTerminalCursorStyle(): Int = TerminalEmulator.DEFAULT_TERMINAL_CURSOR_STYLE

    override fun logError(tag: String?, message: String?) { android.util.Log.e(tag ?: "NexShell", message ?: "") }
    override fun logWarn(tag: String?, message: String?) { android.util.Log.w(tag ?: "NexShell", message ?: "") }
    override fun logInfo(tag: String?, message: String?) { android.util.Log.i(tag ?: "NexShell", message ?: "") }
    override fun logDebug(tag: String?, message: String?) { android.util.Log.d(tag ?: "NexShell", message ?: "") }
    override fun logVerbose(tag: String?, message: String?) { android.util.Log.v(tag ?: "NexShell", message ?: "") }
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) { android.util.Log.e(tag ?: "NexShell", message ?: "", e) }
    override fun logStackTrace(tag: String?, e: Exception?) { android.util.Log.e(tag ?: "NexShell", "", e) }
}