package com.nexshell.snapshot

import com.nexshell.core.Workspace
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Snapshot(
    val name: String,
    val dir: File,
    val createdAt: Long,
    val sizeBytes: Long
)

sealed class SnapshotProgress {
    data class InProgress(val filesDone: Int) : SnapshotProgress()
    data class Failed(val reason: String) : SnapshotProgress()
    object Done : SnapshotProgress()
}

class SnapshotManager {

    fun listSnapshots(workspace: Workspace): List<Snapshot> {
        if (!workspace.snapshotsDir.exists()) return emptyList()
        return workspace.snapshotsDir.listFiles { f -> f.isDirectory }
            ?.map { dir ->
                Snapshot(
                    name = dir.name,
                    dir = dir,
                    createdAt = dir.lastModified(),
                    sizeBytes = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                )
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    fun createSnapshot(
        workspace: Workspace,
        name: String,
        onProgress: (SnapshotProgress) -> Unit
    ) {
        try {
            val safeName = sanitize(name)
            val target = File(workspace.snapshotsDir, safeName)
            if (target.exists()) {
                onProgress(SnapshotProgress.Failed("Snapshot '$safeName' already exists"))
                return
            }
            target.mkdirs()

            var count = 0
            workspace.filesDir.walkTopDown().forEach { src ->
                // Never snapshot the snapshots directory itself, or we'd recurse forever.
                if (src.absolutePath.startsWith(workspace.snapshotsDir.absolutePath)) return@forEach

                val rel = src.relativeTo(workspace.filesDir)
                val dst = File(target, rel.path)
                when {
                    src.isDirectory -> dst.mkdirs()
                    else -> {
                        dst.parentFile?.mkdirs()
                        src.copyTo(dst, overwrite = true)
                        if (src.canExecute()) dst.setExecutable(true, false)
                    }
                }
                count++
                if (count % 500 == 0) onProgress(SnapshotProgress.InProgress(count))
            }

            onProgress(SnapshotProgress.Done)
        } catch (e: Exception) {
            onProgress(SnapshotProgress.Failed(e.message ?: e.toString()))
        }
    }

    fun restoreSnapshot(
        workspace: Workspace,
        snapshotName: String,
        onProgress: (SnapshotProgress) -> Unit
    ) {
        try {
            val source = File(workspace.snapshotsDir, sanitize(snapshotName))
            if (!source.exists()) {
                onProgress(SnapshotProgress.Failed("Snapshot '$snapshotName' not found"))
                return
            }

            // Restore into a staging dir first, then atomically swap — this way
            // a failure mid-restore never leaves the active workspace half-wiped.
            val staging = File(workspace.rootDir, ".restore-staging")
            staging.deleteRecursively()
            staging.mkdirs()

            var count = 0
            source.walkTopDown().forEach { src ->
                val rel = src.relativeTo(source)
                val dst = File(staging, rel.path)
                when {
                    src.isDirectory -> dst.mkdirs()
                    else -> {
                        dst.parentFile?.mkdirs()
                        src.copyTo(dst, overwrite = true)
                        if (src.canExecute()) dst.setExecutable(true, false)
                    }
                }
                count++
                if (count % 500 == 0) onProgress(SnapshotProgress.InProgress(count))
            }

            val backupOfLive = File(workspace.rootDir, ".pre-restore-backup")
            backupOfLive.deleteRecursively()
            if (workspace.filesDir.exists()) workspace.filesDir.renameTo(backupOfLive)
            staging.renameTo(workspace.filesDir)
            backupOfLive.deleteRecursively()

            onProgress(SnapshotProgress.Done)
        } catch (e: Exception) {
            onProgress(SnapshotProgress.Failed(e.message ?: e.toString()))
        }
    }

    fun deleteSnapshot(workspace: Workspace, snapshotName: String): Boolean =
        File(workspace.snapshotsDir, sanitize(snapshotName)).deleteRecursively()

    fun suggestedName(): String =
        "snapshot-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

    private fun sanitize(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_")
}