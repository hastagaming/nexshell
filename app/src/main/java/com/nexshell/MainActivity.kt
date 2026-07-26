package com.nexshell

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.nexshell.core.Workspace
import com.nexshell.core.WorkspaceRepository
import com.nexshell.ui.rootfs.RootFsInstallerScreen
import com.nexshell.ui.terminal.SplitTerminalScreen
import com.nexshell.ui.theme.NexShellTheme
import com.nexshell.ui.workspace.WorkspaceSwitcherScreen

sealed class Screen {
    object Switcher : Screen()
    data class RootFsImport(val workspace: Workspace) : Screen()
    data class Terminal(val workspace: Workspace) : Screen()
}

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way — service still runs, just without a visible notification if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val repository = WorkspaceRepository.getInstance(applicationContext)

        setContent {
            NexShellTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Switcher) }

                Surface(modifier = Modifier.fillMaxSize()) {
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
                            properties = repository.properties(s.workspace.id)
                        )
                    }
                }
            }
        }
    }
}