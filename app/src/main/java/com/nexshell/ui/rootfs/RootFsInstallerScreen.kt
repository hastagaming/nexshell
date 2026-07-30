package com.nexshell.ui.rootfs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexshell.core.Workspace
import com.nexshell.rootfs.InstallProgress
import com.nexshell.rootfs.RootFsCatalog
import com.nexshell.rootfs.ProotInstaller
import com.nexshell.rootfs.RootFsManager
import com.nexshell.rootfs.RootFsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private object CustomRootFsMarker

@Composable
fun RootFsInstallerScreen(workspace: Workspace, onInstalled: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { RootFsManager() }
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf<InstallProgress?>(null) }
    var selected by remember { mutableStateOf<Any?>(null) } // RootFsSource or CustomRootFsMarker
    var customUri by remember { mutableStateOf<Uri?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            customUri = uri
            selected = CustomRootFsMarker
        }
    }

    val busy = progress is InstallProgress.Downloading || progress is InstallProgress.Extracting

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Install RootFS for ${workspace.displayName}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        listOf(RootFsCatalog.UBUNTU, RootFsCatalog.DEBIAN, RootFsCatalog.ALPINE).forEach { src ->
            ListItem(
                headlineContent = { Text(src.displayName) },
                supportingContent = { Text(src.archiveUrl, maxLines = 1) },
                trailingContent = { RadioButton(selected = selected === src, onClick = { selected = src }) }
            )
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        ListItem(
            headlineContent = { Text("Custom RootFS") },
            supportingContent = {
                Text(
                    customUri?.lastPathSegment ?: "Import your own .tar / .tar.gz / .tar.zst / .tar.xz",
                    maxLines = 1
                )
            },
            leadingContent = { Icon(Icons.Filled.FileOpen, contentDescription = null) },
            trailingContent = { RadioButton(selected = selected === CustomRootFsMarker, onClick = {
                filePicker.launch(arrayOf(
                    "application/gzip",
                    "application/x-gtar",
                    "application/x-tar",
                    "application/x-xz",
                    "application/octet-stream" // many file managers report .tar.zst as this
                ))
            }) }
        )

        Spacer(Modifier.height(16.dp))

        Button(
            enabled = !busy && selected != null && (selected !== CustomRootFsMarker || customUri != null),
            onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        when (val sel = selected) {
                            is RootFsSource -> manager.install(workspace, sel) { p ->
                                progress = p
                                if (p is InstallProgress.Done) {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            val nativeLibDir = context.applicationInfo.nativeLibraryDir
                                            if (!com.nexshell.rootfs.ProotInstaller.isInstalled(context.filesDir)) {
                                                runCatching {
                                                    com.nexshell.rootfs.ProotInstaller.install(context.filesDir) { }
                                                }
                                            }
                                        }
                                        onInstalled()
                                    }
                                }
                            }
                            CustomRootFsMarker -> {
                                val uri = customUri ?: return@withContext
                                manager.importCustomFromUri(workspace, context.contentResolver, uri) { p ->
                                    progress = p
                                    if (p is InstallProgress.Done) scope.launch { onInstalled() }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        ) { Text(if (selected === CustomRootFsMarker) "Import" else "Install") }

        Spacer(Modifier.height(16.dp))

        when (val p = progress) {
            is InstallProgress.Downloading -> {
                val pct = if (p.totalBytes > 0) (p.bytesRead * 100 / p.totalBytes).toInt() else 0
                Text(if (selected === CustomRootFsMarker) "Copying file… ${p.bytesRead / 1024 / 1024} MB"
                     else "Downloading… $pct% (${p.bytesRead / 1024 / 1024} MB)")
                if (selected !== CustomRootFsMarker) {
                    LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            InstallProgress.VerifyingChecksum -> Text("Verifying checksum…")
            is InstallProgress.Extracting -> {
                Text("Extracting… ${p.entriesDone} files")
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            is InstallProgress.Failed -> Text("Failed: ${p.reason}", color = MaterialTheme.colorScheme.error)
            InstallProgress.Done -> Text("Installed ✓")
            null -> {}
        }
    }
}