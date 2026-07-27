package com.nexshell.terminal

import com.nexshell.core.Workspace
import com.nexshell.core.WorkspaceProperties
import com.termux.terminal.TerminalSession
import java.io.File

object TerminalSessionFactory {

    // Initial cell size estimate in pixels — used only to initialize the
    // emulator/pty before TerminalView has been measured with real font
    // metrics. TerminalView corrects this via updateSize() once it knows
    // actual glyph width/height for the chosen font and size.
    private const val INITIAL_CELL_WIDTH_PX = 24
    private const val INITIAL_CELL_HEIGHT_PX = 48

    fun create(
        workspace: Workspace,
        properties: WorkspaceProperties,
        nativeLibDir: String,
        sessionClient: NexShellSessionClient,
        rows: Int,
        cols: Int
    ): TerminalSession {
        val hasRootfs = File(workspace.usrDir, "bin").exists()
        val home = workspace.homeDir.absolutePath

        val (executablePath, args, cwd) = if (hasRootfs) {
            val prootBinary = "$nativeLibDir/libnexshell_proot.so"
            val rootfs = workspace.filesDir.absolutePath
            val bindArgs = mutableListOf("-r", rootfs, "-b", "/dev", "-b", "/proc", "-b", "/sys", "-0")
            val storageMarker = File(workspace.rootDir, ".storage-setup-done")
            if (storageMarker.exists()) {
                bindArgs += listOf("-b", "/storage/emulated/0:/root/storage/shared")
            }
            val innerCmd = properties.startupCommand.ifBlank { "/bin/sh -i" }
            Triple(prootBinary, (bindArgs + listOf("/bin/sh", "-c", innerCmd)).toTypedArray(), home)
        } else {
            val args = if (properties.startupCommand.isNotBlank())
                arrayOf("-c", properties.startupCommand) else arrayOf("-i")
            Triple("/system/bin/sh", args, home)
        }

        val env = arrayOf(
            "HOME=$home",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "TERM=xterm-256color",
            "WORKSPACE=${workspace.id}",
            "LD_LIBRARY_PATH=$nativeLibDir"
        )

        val session = TerminalSession(
            executablePath,
            cwd,
            args,
            env,
            properties.terminalTranscriptRows,
            sessionClient
        )

        // First call after construction — mEmulator is still null at this
        // point, so this path calls initializeEmulator(...) internally,
        // which is what actually creates the pty and starts the process.
        session.updateSize(cols, rows, INITIAL_CELL_WIDTH_PX, INITIAL_CELL_HEIGHT_PX)

        return session
    }
}