package com.nexshell.terminal

import com.nexshell.core.Workspace
import com.nexshell.core.WorkspaceProperties
import com.termux.terminal.TerminalSession
import java.util.UUID

class TerminalSessionHolder(
    val id: String = UUID.randomUUID().toString(),
    val workspace: Workspace,
    val label: String,
    nativeLibDir: String,
    properties: WorkspaceProperties,
    rows: Int = 24,
    cols: Int = 80
) {
    var onScreenUpdate: (() -> Unit)? = null

    val client = NexShellSessionClient(onScreenUpdate = { onScreenUpdate?.invoke() })

    val termuxSession: TerminalSession = TerminalSessionFactory.create(
        workspace = workspace,
        properties = properties,
        nativeLibDir = nativeLibDir,
        sessionClient = client,
        rows = rows,
        cols = cols
    )

    val isAlive: Boolean get() = !termuxSession.isRunning.not()

    fun sendInput(text: String) = termuxSession.write(text)

    fun resize(columns: Int, rows: Int, cellWidthPx: Int, cellHeightPx: Int) =
        termuxSession.updateSize(columns, rows, cellWidthPx, cellHeightPx)

    fun destroy() {
        termuxSession.finishIfRunning()
    }
}