package com.nexshell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.nexshell.core.Workspace
import com.nexshell.core.WorkspaceRepository
import com.nexshell.ui.AppShell
import com.nexshell.ui.DrawerDestination
import com.nexshell.ui.font.FontSettingsScreen
import com.nexshell.ui.rootfs.RootFsInstallerScreen
import com.nexshell.ui.snapshot.SnapshotManagerScreen
import com.nexshell.ui.terminal.SplitTerminalScreen
import com.nexshell.ui.theme.NexShellTheme
import com.nexshell.ui.workspace.WorkspaceSwitcherScreen

sealed class Screen {
    object Switcher : Screen()
    data class RootFsImport(val workspace: Workspace) : Screen()
    data class Terminal(val workspace: Workspace) : Screen()
    data class Snapshots(val workspace: Workspace) : Screen()
    data class FontSettings(val workspace: Workspace) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = WorkspaceRepository.getInstance(applicationContext)

        setContent {
            NexShellTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Switcher) }
                val activeWorkspace = (screen as? Screen.Terminal)?.workspace
                    ?: (screen as? Screen.Snapshots)?.workspace
                    ?: (screen as? Screen.FontSettings)?.workspace

                Surface(modifier = Modifier.fillMaxSize()) {
                    AppShell(
                        activeWorkspace = activeWorkspace,
                        onNavigate = { dest ->
                            val ws = activeWorkspace ?: return@AppShell
                            screen = when (dest) {
                                DrawerDestination.WORKSPACES -> Screen.Switcher
                                DrawerDestination.SNAPSHOTS -> Screen.Snapshots(ws)
                                DrawerDestination.FONT -> Screen.FontSettings(ws)
                                DrawerDestination.ROOTFS -> Screen.RootFsImport(ws)
                            }
                        }
                    ) { openDrawer ->
                        when (val s = screen) {
                            is Screen.Switcher -> WorkspaceSwitcherScreen(
                                repository = repository,
                                onOpenWorkspace = { screen = Screen.Terminal(it) },
                                onNeedsRootFsImport = { screen = Screen.RootFsImport(it) }
                            )
                            is Screen.RootFsImport -> RootFsInstallerScreen(
                                workspace = s.workspace,
                                onInstalled = { screen = Screen.Terminal(s.workspace) }
                            )
                            is Screen.Terminal -> SplitTerminalScreen(
                                workspace = s.workspace,
                                properties = repository.properties(s.workspace.id),
                                onOpenDrawer = openDrawer
                            )
                            is Screen.Snapshots -> SnapshotManagerScreen(workspace = s.workspace)
                            is Screen.FontSettings -> FontSettingsScreen(workspace = s.workspace, repository = repository)
                        }
                    }
                }
            }
        }
    }
}