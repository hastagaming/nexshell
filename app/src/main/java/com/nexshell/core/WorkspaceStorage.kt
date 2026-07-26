package com.nexshell.core

import android.content.Context
import java.io.File
import java.util.UUID

class WorkspaceStorage(private val context: Context) {

    private val workspacesRoot: File
        get() = context.filesDir.parentFile ?: context.filesDir
        // resolves to /data/data/com.nexshell/ — each workspace becomes a
        // sibling directory: /data/data/com.nexshell/<id>/

    fun listWorkspaceIds(): List<String> {
        val root = workspacesRoot
        if (!root.exists()) return emptyList()
        return root.listFiles { f -> f.isDirectory && File(f, "$${f.name}.properties".removePrefix("$")).exists() || File(f, "${f.name}.properties").exists() }
            ?.map { it.name }
            ?: emptyList()
    }

    fun workspaceDir(id: String): File = File(workspacesRoot, id)

    fun createWorkspace(id: String, distro: Distro, displayName: String): Workspace {
        val root = workspaceDir(id)
        if (root.exists()) {
            throw IllegalStateException("Workspace '$id' already exists at ${root.absolutePath}")
        }

        val workspace = Workspace(
            id = id,
            displayName = displayName,
            distro = distro,
            rootDir = root
        )

        workspace.requiredDirs().forEach { dir ->
            if (!dir.mkdirs() && !dir.exists()) {
                throw IllegalStateException("Failed to create directory: ${dir.absolutePath}")
            }
        }

        // Minimal per-workspace HOME skeleton so tools relying on $HOME
        // don't immediately fail on a completely empty directory.
        File(workspace.homeDir, ".profile").apply {
            if (!exists()) writeText("# NexShell workspace profile for $id\nexport HOME=${workspace.homeDir.absolutePath}\n")
        }

        val props = WorkspaceProperties()
        workspace.propertiesFile.writeText(props.toFileText())

        return workspace
    }

    fun loadWorkspace(id: String): Workspace? {
        val root = workspaceDir(id)
        val propertiesFile = File(root, "$id.properties")
        if (!root.exists() || !propertiesFile.exists()) return null

        // distro is inferred from a marker file written at creation time
        val distroMarker = File(root, ".distro")
        val distro = if (distroMarker.exists()) {
            runCatching { Distro.valueOf(distroMarker.readText().trim()) }.getOrDefault(Distro.CUSTOM)
        } else Distro.CUSTOM

        val nameMarker = File(root, ".displayname")
        val displayName = if (nameMarker.exists()) nameMarker.readText().trim() else id

        return Workspace(id = id, displayName = displayName, distro = distro, rootDir = root)
    }

    fun deleteWorkspace(id: String): Boolean {
        val root = workspaceDir(id)
        return root.deleteRecursively()
    }

    fun cloneWorkspace(sourceId: String, newId: String, newDisplayName: String): Workspace {
        val sourceRoot = workspaceDir(sourceId)
        if (!sourceRoot.exists()) throw IllegalStateException("Source workspace '$sourceId' does not exist")

        val targetRoot = workspaceDir(newId)
        if (targetRoot.exists()) throw IllegalStateException("Workspace '$newId' already exists")

        sourceRoot.copyRecursively(targetRoot, overwrite = false)

        // The clone is an independent copy — rename its properties file and
        // markers so it no longer aliases the source workspace's identity.
        val oldPropsFile = File(targetRoot, "$sourceId.properties")
        val newPropsFile = File(targetRoot, "$newId.properties")
        if (oldPropsFile.exists()) oldPropsFile.copyTo(newPropsFile, overwrite = true)
        oldPropsFile.delete()

        File(targetRoot, ".displayname").writeText(newDisplayName)

        return loadWorkspace(newId) ?: throw IllegalStateException("Clone failed for '$newId'")
    }

    fun newWorkspaceId(prefix: String): String {
        var candidate = prefix
        var suffix = 1
        while (workspaceDir(candidate).exists()) {
            candidate = "$prefix-$suffix"
            suffix++
        }
        return candidate
    }

    fun writeDistroMarker(workspace: Workspace) {
        File(workspace.rootDir, ".distro").writeText(workspace.distro.name)
        File(workspace.rootDir, ".displayname").writeText(workspace.displayName)
    }
}