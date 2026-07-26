package com.nexshell.service

import com.nexshell.core.Workspace
import com.nexshell.pty.PtyNative
import kotlinx.coroutines.*

enum class ServiceState { STOPPED, RUNNING, FAILED }

data class ManagedService(
    val name: String,
    val startCommand: String,
    val stopCommand: String?,
    val workspace: Workspace,
    var pid: Int = -1,
    var state: ServiceState = ServiceState.STOPPED
)

/**
 * Runs and tracks background service processes per workspace. Each service
 * is a real forked process inside the workspace's rootfs (via the same
 * proot path used for interactive sessions), not a UI-only toggle.
 */
class ServiceManager(private val nativeLibDir: String) {

    private val services = mutableMapOf<String, MutableList<ManagedService>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun servicesFor(workspaceId: String): List<ManagedService> = services[workspaceId] ?: emptyList()

    fun registerService(workspace: Workspace, name: String, startCommand: String, stopCommand: String?) {
        val list = services.getOrPut(workspace.id) { mutableListOf() }
        if (list.none { it.name == name }) {
            list += ManagedService(name, startCommand, stopCommand, workspace)
        }
    }

    fun start(workspaceId: String, name: String) {
        val svc = services[workspaceId]?.find { it.name == name } ?: return
        if (svc.state == ServiceState.RUNNING) return

        val prootBinary = "$nativeLibDir/libnexshell_proot.so"
        val rootfs = svc.workspace.filesDir.absolutePath
        val env = arrayOf(
            "HOME=${svc.workspace.homeDir.absolutePath}",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "LD_LIBRARY_PATH=$nativeLibDir"
        )
        val args = arrayOf("-r", rootfs, "-b", "/dev", "-b", "/proc", "-0", "/bin/sh", "-c", svc.startCommand)

        val result = PtyNative.forkPty(prootBinary, args, env, svc.workspace.homeDir.absolutePath, 24, 80)
        if (result == null) {
            svc.state = ServiceState.FAILED
            return
        }

        svc.pid = result[1].toInt()
        svc.state = ServiceState.RUNNING

        scope.launch {
            val exitCode = PtyNative.waitForExit(svc.pid)
            svc.state = if (exitCode == 0) ServiceState.STOPPED else ServiceState.FAILED
        }
    }

    fun stop(workspaceId: String, name: String) {
        val svc = services[workspaceId]?.find { it.name == name } ?: return
        if (svc.state != ServiceState.RUNNING || svc.pid <= 0) return

        if (svc.stopCommand != null) {
            // Run the configured graceful-stop command instead of SIGKILL.
            val prootBinary = "$nativeLibDir/libnexshell_proot.so"
            val rootfs = svc.workspace.filesDir.absolutePath
            val env = arrayOf("HOME=${svc.workspace.homeDir.absolutePath}", "LD_LIBRARY_PATH=$nativeLibDir")
            val args = arrayOf("-r", rootfs, "-b", "/dev", "-b", "/proc", "-0", "/bin/sh", "-c", svc.stopCommand)
            PtyNative.forkPty(prootBinary, args, env, svc.workspace.homeDir.absolutePath, 24, 80)
        } else {
            PtyNative.sendSignal(svc.pid, 15)
        }
        svc.state = ServiceState.STOPPED
        svc.pid = -1
    }

    fun stopAllForWorkspace(workspaceId: String) {
        services[workspaceId]?.forEach { if (it.state == ServiceState.RUNNING) stop(workspaceId, it.name) }
    }

    fun runningCountFor(workspaceId: String): Int =
        services[workspaceId]?.count { it.state == ServiceState.RUNNING } ?: 0
}