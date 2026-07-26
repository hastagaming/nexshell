package com.nexshell.setup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.nexshell.core.Workspace
import java.io.File

sealed class SetupResult {
    object AlreadyGranted : SetupResult()
    object NeedsPermissionRequest : SetupResult()
    object Granted : SetupResult()
}

/**
 * Real Android storage-access flow — equivalent in spirit to Termux's
 * termux-setup-storage, but targets MANAGE_EXTERNAL_STORAGE (required on
 * Android 11+ for a general-purpose file-manager-capable environment).
 * Only after this succeeds does PtyProcess bind /storage/emulated/0 into
 * the workspace's proot session.
 */
class NexShellSetup(private val activity: Activity) {

    fun checkStatus(): SetupResult {
        return if (hasFullStorageAccess()) SetupResult.AlreadyGranted
        else SetupResult.NeedsPermissionRequest
    }

    private fun hasFullStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val perm = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            activity.checkSelfPermission(perm) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestAccess(requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivityForResult(intent, requestCode)
        } else {
            activity.requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                requestCode
            )
        }
    }

    /** Called after permission is confirmed granted. Marks the workspace
     *  and creates the ~/storage/{shared,downloads,pictures,movies} symlink
     *  layout, mirroring what the guest will see once proot bind-mounts it. */
    fun finalizeForWorkspace(workspace: Workspace): Boolean {
        if (!hasFullStorageAccess()) return false

        File(workspace.rootDir, ".storage-setup-done").writeText(System.currentTimeMillis().toString())

        val storageLinkDir = File(workspace.homeDir, "storage").apply { mkdirs() }
        val realShared = Environment.getExternalStorageDirectory()

        val links = mapOf(
            "shared" to realShared,
            "downloads" to File(realShared, Environment.DIRECTORY_DOWNLOADS),
            "pictures" to File(realShared, Environment.DIRECTORY_PICTURES),
            "movies" to File(realShared, Environment.DIRECTORY_MOVIES)
        )

        links.forEach { (name, target) ->
            val link = File(storageLinkDir, name)
            if (!link.exists()) {
                runCatching {
                    java.nio.file.Files.createSymbolicLink(link.toPath(), target.toPath())
                }
            }
        }
        return true
    }
}