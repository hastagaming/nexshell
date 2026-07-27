package com.nexshell.service

import com.nexshell.core.Workspace
import java.io.File

enum class ServiceState { STOPPED, RUNNING, FAILED }

data class ManagedService(
    val name: String,
    val startCommand: String,
    val stopCommand: String?,
    val workspace: Workspace,
    var process: Process? = null,
    var state: ServiceState = ServiceState.STOPPED
)

/**
 * Runs and tracks background service processes per workspace using
 * ProcessBuilder — a real forked OS process inside the workspace's rootfs
 * via proot, not a UI-only toggle.
 */
class ServiceManager(private val nativeLibDir: String) {

    private val services = mutableMapOf<String, MutableList<ManagedService>>()

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
        val home = svc.workspace.homeDir.absolutePath

        val command = listOf(
            prootBinary, "-r", rootfs, "-b", "/dev", "-b", "/proc", "-0",
            "/bin/sh", "-c", svc.startCommand
        )

        try {
            val process = ProcessBuilder(command)
                .directory(File(home))
                .redirectErrorStream(true)
                .apply {
                    environment()["HOME"] = home
                    environment()["LD_LIBRARY_PATH"] = nativeLibDir
                }
                .start()

            svc.process = process
            svc.state = ServiceState.RUNNING

            Thread {
                val exitCode = process.waitFor()
                svc.state = if (exitCode == 0) ServiceState.STOPPED else ServiceState.FAILED
                svc.process = null
            }.apply { isDaemon = true }.start()
        } catch (e: Exception) {
            svc.state = ServiceState.FAILED
        }
    }

    fun stop(workspaceId: String, name: String) {
        val svc = services[workspaceId]?.find { it.name == name } ?: return
        val process = svc.process ?: return

        if (svc.stopCommand != null) {
            val prootBinary = "$nativeLibDir/libnexshell_proot.so"
            val rootfs = svc.workspace.filesDir.absolutePath
            val home = svc.workspace.homeDir.absolutePath
            val command = listOf(
                prootBinary, "-r", rootfs, "-b", "/dev", "-b", "/proc", "-0",
                "/bin/sh", "-c", svc.stopCommand
            )
            runCatching {
                ProcessBuilder(command).apply {
                    environment()["HOME"] = home
                    environment()["LD_LIBRARY_PATH"] = nativeLibDir
                }.start().waitFor()
            }
        } else {
            process.destroy()
        }
        svc.state = ServiceState.STOPPED
        svc.process = null
    }

    fun stopAllForWorkspace(workspaceId: String) {
        services[workspaceId]?.forEach { if (it.state == ServiceState.RUNNING) stop(workspaceId, it.name) }
    }

    fun runningCountFor(workspaceId: String): Int =
        services[workspaceId]?.count { it.state == ServiceState.RUNNING } ?: 0
}