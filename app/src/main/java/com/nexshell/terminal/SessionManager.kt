package com.nexshell.terminal

import android.content.Context
import com.nexshell.core.Workspace
import com.nexshell.core.WorkspaceProperties
import com.nexshell.service.NexShellForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {

    private val _sessions = MutableStateFlow<List<TerminalSession>>(emptyList())
    val sessions: StateFlow<List<TerminalSession>> = _sessions.asStateFlow()

    fun sessionsFor(workspaceId: String): List<TerminalSession> =
        _sessions.value.filter { it.workspace.id == workspaceId }

    fun createSession(
        context: Context,
        workspace: Workspace,
        properties: WorkspaceProperties,
        label: String = "Session ${sessionsFor(workspace.id).size + 1}",
        rows: Int = 24,
        cols: Int = 80
    ): TerminalSession {
        val session = TerminalSession(
            workspace = workspace,
            label = label,
            nativeLibDir = context.applicationInfo.nativeLibraryDir
        )
        session.start(rows, cols, properties.startupCommand, properties.theme)
        _sessions.value = _sessions.value + session

        // First session of the app run: make sure the foreground service is
        // actually alive before asking it to refresh its notification.
        NexShellForegroundService.start(context.applicationContext)
        NexShellForegroundService.refresh(context.applicationContext)
        return session
    }

    fun closeSession(sessionId: String) {
        val session = _sessions.value.find { it.id == sessionId } ?: return
        session.destroy()
        _sessions.value = _sessions.value.filterNot { it.id == sessionId }
    }

    fun closeAllForWorkspace(workspaceId: String) {
        sessionsFor(workspaceId).forEach { closeSession(it.id) }
    }

    fun totalRunningCount(): Int = _sessions.value.count { it.isAlive }
    fun runningWorkspaceNames(): List<String> =
        _sessions.value.filter { it.isAlive }.map { it.workspace.displayName }.distinct()
}