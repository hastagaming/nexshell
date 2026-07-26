package com.nexshell.pty

import com.nexshell.core.Workspace
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import java.io.File

class PtyProcess(private val workspace: Workspace, private val nativeLibDir: String) {

    private var masterFd: Int = -1
    private var pid: Int = -1
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val isRunning: Boolean get() = pid > 0 && masterFd >= 0

    fun start(rows: Int, cols: Int, startupCommand: String) {
        val prootBinary = File(nativeLibDir, "libnexshell_proot.so").absolutePath
        val home = workspace.homeDir.absolutePath
        val rootfs = workspace.filesDir.absolutePath
        val hasRootfs = File(workspace.usrDir, "bin").exists()

        val env = arrayOf(
            "HOME=$home",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "TERM=xterm-256color",
            "WORKSPACE=${workspace.id}",
            "PROOT_TMP_DIR=${File(workspace.rootDir, ".proot-tmp").apply { mkdirs() }.absolutePath",
            "LD_LIBRARY_PATH=$nativeLibDir"
        )

        val shellCmd: String
        val args: Array<String>

        if (hasRootfs) {
            // Real chroot-style isolation into the workspace's own rootfs via proot.
            // -r: new root. -b: bind mounts so the guest sees Android's /dev, /proc, /sys
            // and — after nexshell-setup — the user's shared storage.
            shellCmd = prootBinary
            val bindArgs = mutableListOf(
                "-r", rootfs,
                "-b", "/dev",
                "-b", "/proc",
                "-b", "/sys",
                "-w", "/root".let { if (File(home).exists()) home.removePrefix(rootfs).ifBlank { "/" } else "/" },
                "-0" // fake root uid inside the guest, required by apt/apk package managers
            )
            val androidStorageMarker = File(workspace.rootDir, ".storage-setup-done")
            if (androidStorageMarker.exists()) {
                bindArgs += listOf("-b", "/storage/emulated/0:/root/storage/shared")
            }
            val innerCmd = if (startupCommand.isNotBlank()) startupCommand else "/bin/sh -i"
            args = (bindArgs + listOf("/bin/sh", "-c", innerCmd)).toTypedArray()
        } else {
            // No rootfs installed yet for this workspace — fall back to Android's
            // own shell so the terminal is still usable (e.g. to run nexshell-setup
            // or inspect the empty workspace) until RootFS Manager installs one.
            shellCmd = "/system/bin/sh"
            args = if (startupCommand.isNotBlank()) arrayOf("-c", startupCommand) else arrayOf("-i")
        }

        val result = PtyNative.forkPty(shellCmd, args, env, home, rows, cols)
            ?: throw IllegalStateException("forkPty failed for workspace ${workspace.id}")

        masterFd = result[0].toInt()
        pid = result[1].toInt()
    }

    fun output() = callbackFlow<ByteArray> {
        val buf = ByteArray(4096)
        val job = scope.launch {
            while (isActive && isRunning) {
                val n = PtyNative.readFd(masterFd, buf, buf.size)
                if (n > 0) trySend(buf.copyOf(n)) else break
            }
            close()
        }
        awaitClose { job.cancel() }
    }

    fun write(data: ByteArray) { if (isRunning) PtyNative.writeFd(masterFd, data, data.size) }
    fun write(text: String) = write(text.toByteArray(Charsets.UTF_8))
    fun resize(rows: Int, cols: Int) { if (isRunning) PtyNative.resize(masterFd, rows, cols) }

    fun destroy() {
        if (pid > 0) PtyNative.sendSignal(pid, 15)
        if (masterFd >= 0) PtyNative.closeFd(masterFd)
        scope.cancel()
        pid = -1
        masterFd = -1
    }

    suspend fun awaitExit(): Int = withContext(Dispatchers.IO) {
        if (pid <= 0) -1 else PtyNative.waitForExit(pid)
    }

    private fun callbackFlow(block: suspend kotlinx.coroutines.channels.ProducerScope<ByteArray>.() -> Unit) =
        kotlinx.coroutines.flow.callbackFlow(block)
}