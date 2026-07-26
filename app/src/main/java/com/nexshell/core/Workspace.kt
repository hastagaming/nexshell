package com.nexshell.core

import java.io.File

/**
 * Represents one isolated Linux workspace. Each workspace owns its own
 * rootfs directory tree under the app's private data dir — nothing is
 * shared between workspaces at the filesystem level.
 */
data class Workspace(
    val id: String,
    val displayName: String,
    val distro: Distro,
    val rootDir: File,
    val isActive: Boolean = false
) {
    val filesDir: File get() = File(rootDir, "files")
    val homeDir: File get() = File(filesDir, "home")
    val usrDir: File get() = File(filesDir, "usr")
    val etcDir: File get() = File(filesDir, "etc")
    val varDir: File get() = File(filesDir, "var")
    val propertiesFile: File get() = File(rootDir, "$id.properties")
    val snapshotsDir: File get() = File(rootDir, "snapshots")

    fun requiredDirs(): List<File> = listOf(filesDir, homeDir, usrDir, etcDir, varDir, snapshotsDir)
}

enum class Distro(val label: String) {
    UBUNTU("Ubuntu 24.04"),
    DEBIAN("Debian 13"),
    ALPINE("Alpine"),
    CUSTOM("Custom RootFS")
}