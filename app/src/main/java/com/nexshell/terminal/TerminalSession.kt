package com.nexshell.terminal

import com.nexshell.core.Workspace
import com.nexshell.core.WorkspaceProperties
import com.nexshell.pty.PtyProcess
import kotlinx.coroutines.*
import java.util.UUID

class TerminalSession(
    val id: String = UUID.randomUUID().toString(),
    val workspace: Workspace,
    val label: String,
    nativeLibDir: String
) {
    val engine = TerminalEngine(rows = 24, cols = 80)
    private val pty = PtyProcess(workspace, nativeLibDir)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var readJob: Job? = null

    var isAlive = false
        private set

    fun start(rows: Int, cols: Int, startupCommand: String, themeName: String = "catppuccin-mocha") {
        engine.applyTheme(com.nexshell.theme.ThemeCatalog.byName(themeName))
        engine.resize(rows, cols)
        pty.start(rows, cols, startupCommand)
        isAlive = true

        readJob = scope.launch {
            pty.output().collect { bytes -> engine.feed(bytes, bytes.size) }
        }
        scope.launch {
            pty.awaitExit()
            isAlive = false
        }
    }

    fun runYazi() {
        sendInput("PATH=\"\$HOME/.local/bin:\$PATH\" yazi\r")
    }

    fun sendInput(text: String) = pty.write(text)
    fun sendBytes(bytes: ByteArray) = pty.write(bytes)
    fun resize(rows: Int, cols: Int) { engine.resize(rows, cols); pty.resize(rows, cols) }

    fun destroy() {
        readJob?.cancel()
        pty.destroy()
        scope.cancel()
        isAlive = false
    }
}