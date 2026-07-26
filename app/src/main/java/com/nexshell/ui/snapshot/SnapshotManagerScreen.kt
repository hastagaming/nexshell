package com.nexshell.ui.snapshot

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexshell.core.Workspace
import com.nexshell.snapshot.Snapshot
import com.nexshell.snapshot.SnapshotManager
import com.nexshell.snapshot.SnapshotProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SnapshotManagerScreen(workspace: Workspace) {
    val manager = remember { SnapshotManager() }
    val scope = rememberCoroutineScope()
    var snapshots by remember { mutableStateOf(manager.listSnapshots(workspace)) }
    var busyMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    fun refresh() { snapshots = manager.listSnapshots(workspace) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Snapshots — ${workspace.displayName}", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { showCreateDialog = true }) { Icon(Icons.Filled.Add, contentDescription = "Create snapshot") }
        }

        busyMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

        LazyColumnSnapshots(
            snapshots = snapshots,
            onRestore = { snap ->
                scope.launch {
                    busyMessage = "Restoring ${snap.name}…"
                    withContext(Dispatchers.IO) {
                        manager.restoreSnapshot(workspace, snap.name) { p ->
                            when (p) {
                                is SnapshotProgress.InProgress -> busyMessage = "Restoring… ${p.filesDone} files"
                                is SnapshotProgress.Failed -> busyMessage = "Restore failed: ${p.reason}"
                                SnapshotProgress.Done -> busyMessage = "Restored ✓"
                            }
                        }
                    }
                    refresh()
                }
            },
            onDelete = { snap -> manager.deleteSnapshot(workspace, snap.name); refresh() }
        )
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf(manager.suggestedName()) }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Snapshot") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    scope.launch {
                        busyMessage = "Creating snapshot…"
                        withContext(Dispatchers.IO) {
                            manager.createSnapshot(workspace, name) { p ->
                                when (p) {
                                    is SnapshotProgress.InProgress -> busyMessage = "Copying… ${p.filesDone} files"
                                    is SnapshotProgress.Failed -> busyMessage = "Failed: ${p.reason}"
                                    SnapshotProgress.Done -> busyMessage = "Snapshot created ✓"
                                }
                            }
                        }
                        refresh()
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun LazyColumnSnapshots(
    snapshots: List<Snapshot>,
    onRestore: (Snapshot) -> Unit,
    onDelete: (Snapshot) -> Unit
) {
    val fmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US) }
    androidx.compose.foundation.lazy.LazyColumn {
        androidx.compose.foundation.lazy.items(snapshots, key = { it.name }) { snap ->
            ListItem(
                headlineContent = { Text(snap.name) },
                supportingContent = { Text("${fmt.format(Date(snap.createdAt))} · ${snap.sizeBytes / 1024 / 1024} MB") },
                trailingContent = {
                    Row {
                        IconButton(onClick = { onRestore(snap) }) { Icon(Icons.Filled.Restore, contentDescription = "Restore") }
                        IconButton(onClick = { onDelete(snap) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                    }
                }
            )
        }
    }
}