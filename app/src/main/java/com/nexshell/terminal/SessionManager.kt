package com.nexshell.terminal

import android.content.Context
import com.nexshell.core.Workspace
import com.nexshell.core.WorkspaceProperties
import com.nexshell.service.NexShellForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {

    private val _sessions = MutableStateFlow<List<TerminalSessionHolder>>(emptyList())
    val sessions: StateFlow<List<TerminalSessionHolder>> = _sessions.asStateFlow()

    fun sessionsFor(workspaceId: String): List<TerminalSessionHolder> =
        _sessions.value.filter { it.workspace.id == workspaceId }

    fun createSession(
        context: Context,
        workspace: Workspace,
        properties: WorkspaceProperties,
        label: String = "Session ${sessionsFor(workspace.id).size + 1}",
        rows: Int = 24,
        cols: Int = 80
    ): TerminalSessionHolder {
        val holder = TerminalSessionHolder(
            workspace = workspace,
            label = label,
            nativeLibDir = context.applicationInfo.nativeLibraryDir,
            properties = properties,
            rows = rows,
            cols = cols
        )
        _sessions.value = _sessions.value + holder

        NexShellForegroundService.start(context.applicationContext)
        NexShellForegroundService.refresh(context.applicationContext)
        return holder
    }

    fun closeSession(sessionId: String) {
        val holder = _sessions.value.find { it.id == sessionId } ?: return
        holder.destroy()
        _sessions.value = _sessions.value.filterNot { it.id == sessionId }
    }

    fun closeAllForWorkspace(workspaceId: String) {
        sessionsFor(workspaceId).forEach { closeSession(it.id) }
    }

    fun totalRunningCount(): Int = _sessions.value.count { it.isAlive }
    fun runningWorkspaceNames(): List<String> =
        _sessions.value.filter { it.isAlive }.map { it.workspace.displayName }.distinct()
}