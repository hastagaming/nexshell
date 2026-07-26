package com.nexshell.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexshell.core.Distro
import com.nexshell.core.Workspace
import com.nexshell.core.WorkspaceRepository
import com.nexshell.rootfs.RootFsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceSwitcherScreen(
    repository: WorkspaceRepository,
    onOpenWorkspace: (Workspace) -> Unit,
    onNeedsRootFsImport: (Workspace) -> Unit
) {
    val workspaces by repository.workspaces.collectAsState()
    val activeId by repository.activeWorkspaceId.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    val rootFsManager = remember { RootFsManager() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Switch Workspace") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(workspaces, key = { it.id }) { ws ->
                    WorkspaceRow(
                        workspace = ws,
                        isActive = ws.id == activeId,
                        hasRootFs = rootFsManager.isInstalled(ws),
                        onClick = {
                            repository.setActiveWorkspace(ws.id)
                            if (rootFsManager.isInstalled(ws)) {
                                onOpenWorkspace(ws)
                            } else {
                                // No rootfs yet — the shell would only fall back to
                                // Android's own sh, which isn't the point of NexShell.
                                onNeedsRootFsImport(ws)
                            }
                        }
                    )
                }
            }

            Divider()

            ListItem(
                headlineContent = { Text("Create Workspace") },
                leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
                modifier = Modifier.clickable { showCreateDialog = true }
            )
        }
    }

    if (showCreateDialog) {
        CreateWorkspaceDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { distro, name ->
                val ws = repository.createWorkspace(distro, name)
                showCreateDialog = false
                onNeedsRootFsImport(ws)
            }
        )
    }
}

@Composable
private fun WorkspaceRow(workspace: Workspace, isActive: Boolean, hasRootFs: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isActive, onClick = onClick)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(workspace.displayName, fontWeight = FontWeight.SemiBold)
                Text(workspace.distro.label, style = MaterialTheme.typography.bodySmall)
            }
            if (!hasRootFs) {
                AssistChip(onClick = onClick, label = { Text("Needs RootFS") })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateWorkspaceDialog(onDismiss: () -> Unit, onCreate: (Distro, String) -> Unit) {
    var selectedDistro by remember { mutableStateOf(Distro.UBUNTU) }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Workspace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display name") }, singleLine = true)
                Text("Distro", style = MaterialTheme.typography.labelLarge)
                Distro.entries.forEach { distro ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedDistro = distro }) {
                        RadioButton(selected = selectedDistro == distro, onClick = { selectedDistro = distro })
                        Text(distro.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onCreate(selectedDistro, name.trim()) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}