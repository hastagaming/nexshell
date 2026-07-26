package com.nexshell.profile

import com.nexshell.core.Workspace
import java.io.File

class ProfileManager {

    private fun profilesFile(workspace: Workspace) = File(workspace.rootDir, "profiles.ini")

    fun listProfiles(workspace: Workspace): List<WorkspaceProfile> {
        val file = profilesFile(workspace)
        if (!file.exists()) return listOf(defaultProfile())
        val parsed = WorkspaceProfile.parseAll(file.readText())
        return if (parsed.isEmpty()) listOf(defaultProfile()) else parsed
    }

    fun saveProfile(workspace: Workspace, profile: WorkspaceProfile) {
        val existing = listProfiles(workspace).filterNot { it.name == profile.name }
        val all = existing + profile
        profilesFile(workspace).writeText(all.joinToString("\n") { it.toIniBlock() })
    }

    fun deleteProfile(workspace: Workspace, name: String) {
        if (name == "Default") return // Default is not deletable
        val remaining = listProfiles(workspace).filterNot { it.name == name }
        profilesFile(workspace).writeText(remaining.joinToString("\n") { it.toIniBlock() })
    }

    private fun defaultProfile() = WorkspaceProfile(
        name = "Default", shell = "/bin/sh", startupCommand = ""
    )
}