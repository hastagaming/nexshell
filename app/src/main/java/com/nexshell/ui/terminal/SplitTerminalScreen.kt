package com.nexshell.ui.terminal

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexshell.core.Workspace
import com.nexshell.core.WorkspaceProperties
import com.nexshell.font.FontCatalog
import com.nexshell.font.FontFamilyOption
import com.nexshell.service.NexShellForegroundService
import com.nexshell.terminal.SessionManager
import com.nexshell.terminal.TerminalSession

enum class SplitOrientation { NONE, HORIZONTAL, VERTICAL }
data class Pane(val session: TerminalSession)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTerminalScreen(workspace: Workspace, properties: WorkspaceProperties) {
    val context = LocalContext.current

    // Resolved once here, from the workspace's saved .properties font name,
    // then passed down to every pane's TerminalView so all panes in this
    // workspace render with the same family.
    val fontFamily = remember(properties.fontFamily) {
        FontCatalog.allGrouped().values.flatten().find { it.displayName == properties.fontFamily }
            ?: FontCatalog.nerdFonts.first { it.displayName == "JetBrainsMono Nerd Font" }
    }

    var panes by remember {
        mutableStateOf(listOf(Pane(SessionManager.createSession(context, workspace, properties))))
    }
    var orientation by remember { mutableStateOf(SplitOrientation.NONE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workspace.displayName) },
                actions = {
                    IconButton(onClick = {
                        panes = panes + Pane(SessionManager.createSession(context, workspace, properties))
                        orientation = if (orientation == SplitOrientation.NONE) SplitOrientation.HORIZONTAL else orientation
                    }) { Icon(Icons.Filled.Add, contentDescription = "New session") }
                }
            )
        }
    ) { padding ->
        when {
            panes.size == 1 -> {
                TerminalScreenWithKeys(
                    session = panes[0].session,
                    properties = properties,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
            orientation == SplitOrientation.HORIZONTAL -> {
                Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                    panes.forEach { pane ->
                        PaneContainer(
                            pane = pane,
                            fontFamily = fontFamily,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            SessionManager.closeSession(pane.session.id)
                            panes = panes.filterNot { it == pane }
                            NexShellForegroundService.refresh(context.applicationContext)
                        }
                    }
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    panes.forEach { pane ->
                        PaneContainer(
                            pane = pane,
                            fontFamily = fontFamily,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            SessionManager.closeSession(pane.session.id)
                            panes = panes.filterNot { it == pane }
                            NexShellForegroundService.refresh(context.applicationContext)
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(workspace.id) {
        onDispose { /* sessions persist in SessionManager until explicitly closed by user or Exit action */ }
    }
}

@Composable
private fun PaneContainer(
    pane: Pane,
    fontFamily: FontFamilyOption,
    modifier: Modifier,
    onClose: () -> Unit
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(pane.session.label, style = MaterialTheme.typography.labelSmall)
            IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close pane")
            }
        }
        TerminalScreenWithKeys(
            session = pane.session,
            properties = properties,
            modifier = Modifier.weight(1f)
        )
    }
}