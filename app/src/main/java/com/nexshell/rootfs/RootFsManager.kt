package com.nexshell.rootfs

import android.content.ContentResolver
import android.net.Uri
import com.nexshell.core.Workspace
import java.io.File

class RootFsManager {
    private val installer = RootFsInstaller()

    fun install(workspace: Workspace, source: RootFsSource, onProgress: (InstallProgress) -> Unit) {
        installer.install(workspace, source, onProgress)
    }

    fun importCustom(workspace: Workspace, archive: File, onProgress: (InstallProgress) -> Unit) {
        installer.importCustomRootFs(workspace, archive, onProgress)
    }

    fun importCustomFromUri(
        workspace: Workspace,
        resolver: ContentResolver,
        uri: Uri,
        onProgress: (InstallProgress) -> Unit
    ) {
        try {
            // Content Uris (SAF picker results) aren't a real filesystem path,
            // so copy the stream into the workspace's own cache first, then
            // reuse the same tested extraction path as installCustom().
            val cacheDir = File(workspace.rootDir, ".cache").apply { mkdirs() }
            val staged = File(cacheDir, "custom-import.tar")

            resolver.openInputStream(uri)?.use { input ->
                staged.outputStream().use { output -> input.copyTo(output) }
            } ?: run {
                onProgress(InstallProgress.Failed("Cannot open selected file: $uri"))
                return
            }

            installer.importCustomRootFs(workspace, staged, onProgress)
            staged.delete()
        } catch (e: Exception) {
            onProgress(InstallProgress.Failed(e.message ?: e.toString()))
        }
    }

    fun export(workspace: Workspace, destination: File, onProgress: (InstallProgress) -> Unit) {
        installer.exportRootFs(workspace, destination, onProgress)
    }

    fun exportToUri(
        workspace: Workspace,
        resolver: ContentResolver,
        destUri: Uri,
        onProgress: (InstallProgress) -> Unit
    ) {
        try {
            val cacheDir = File(workspace.rootDir, ".cache").apply { mkdirs() }
            val staged = File(cacheDir, "${workspace.id}-export.tar.gz")

            installer.exportRootFs(workspace, staged) { p ->
                if (p !is InstallProgress.Done) onProgress(p)
            }

            resolver.openOutputStream(destUri)?.use { output ->
                staged.inputStream().use { input -> input.copyTo(output) }
            } ?: run {
                onProgress(InstallProgress.Failed("Cannot open destination for writing: $destUri"))
                return
            }
            staged.delete()
            onProgress(InstallProgress.Done)
        } catch (e: Exception) {
            onProgress(InstallProgress.Failed(e.message ?: e.toString()))
        }
    }

    fun remove(workspace: Workspace): Boolean = workspace.filesDir.deleteRecursively()

    fun isInstalled(workspace: Workspace): Boolean =
        workspace.usrDir.exists() && (workspace.usrDir.listFiles()?.isNotEmpty() == true)

    fun rootfsSizeBytes(workspace: Workspace): Long =
        workspace.filesDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}