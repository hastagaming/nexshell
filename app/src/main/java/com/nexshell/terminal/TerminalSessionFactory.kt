package com.nexshell.terminal

import com.nexshell.core.Workspace
import com.nexshell.core.WorkspaceProperties
import com.termux.terminal.TerminalSession
import java.io.File

object TerminalSessionFactory {

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
            val bindArgs = mutableListOf(
                "-r", rootfs,
                "-b", "/dev",
                "-b", "/proc",
                "-b", "/sys",
                "-0"
            )
            val storageMarker = File(workspace.rootDir, ".storage-setup-done")
            if (storageMarker.exists()) {
                bindArgs += listOf("-b", "/storage/emulated/0:/root/storage/shared")
            }
            val innerCmd = properties.startupCommand.ifBlank { "/bin/sh -i" }
            Triple(prootBinary, (bindArgs + listOf("/bin/sh", "-c", innerCmd)).toTypedArray(), home)
        } else {
            // No rootfs installed for this workspace yet — fall back to
            // Android's own shell so the session is still usable
            // (e.g. to run nexshell-setup) until RootFS Manager installs one.
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

        // TerminalSession.create() is Termux's real constructor: it forks
        // the process, attaches a genuine PTY via their JNI bridge, and
        // wires stdin/stdout to the TerminalEmulator screen buffer.
        return TerminalSession(
            executablePath,
            cwd,
            args,
            env,
            TerminalSession.getDefaultTermType(), // "xterm-256color" default from Termux
            sessionClient
        ).apply {
            updateSize(cols, rows)
        }
    }
}