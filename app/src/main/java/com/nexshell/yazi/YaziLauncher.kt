package com.nexshell.yazi

import com.nexshell.core.Workspace
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

sealed class YaziInstallProgress {
    data class Downloading(val bytesRead: Long, val totalBytes: Long) : YaziInstallProgress()
    object Extracting : YaziInstallProgress()
    data class Failed(val reason: String) : YaziInstallProgress()
    object Done : YaziInstallProgress()
}

class YaziLauncher {

    private fun releaseAssetName(): String {
        // Yazi publishes prebuilt musl-linked Linux binaries per arch, which
        // run correctly inside the proot guest regardless of host libc.
        val arch = when (android.os.Build.SUPPORTED_ABIS.firstOrNull()) {
            "arm64-v8a" -> "aarch64-unknown-linux-musl"
            "armeabi-v7a" -> "armv7-unknown-linux-musleabihf"
            "x86_64" -> "x86_64-unknown-linux-musl"
            else -> "x86_64-unknown-linux-musl"
        }
        return "yazi-$arch.zip"
    }

    fun isInstalled(workspace: Workspace): Boolean =
        File(workspace.homeDir, ".local/bin/yazi").exists()

    fun install(workspace: Workspace, onProgress: (YaziInstallProgress) -> Unit) {
        try {
            val asset = releaseAssetName()
            // Resolves the latest release download URL from GitHub's API redirect.
            val downloadUrl = "https://github.com/sxyazi/yazi/releases/latest/download/$asset"

            val binDir = File(workspace.homeDir, ".local/bin").apply { mkdirs() }
            val zipFile = File(workspace.rootDir, ".cache/yazi-download.zip").apply { parentFile?.mkdirs() }

            val conn = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
            }
            conn.connect()
            if (conn.responseCode !in 200..299) {
                onProgress(YaziInstallProgress.Failed("Download failed: HTTP ${conn.responseCode}"))
                return
            }

            val total = conn.contentLengthLong
            var readTotal = 0L
            conn.inputStream.use { input ->
                FileOutputStream(zipFile).use { output ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        readTotal += n
                        onProgress(YaziInstallProgress.Downloading(readTotal, total))
                    }
                }
            }

            onProgress(YaziInstallProgress.Extracting)
            ZipInputStream(zipFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.substringAfterLast('/') == "yazi") {
                        val outFile = File(binDir, "yazi")
                        FileOutputStream(outFile).use { out -> zip.copyTo(out) }
                        outFile.setExecutable(true, false)
                    }
                    entry = zip.nextEntry
                }
            }
            zipFile.delete()

            if (!File(binDir, "yazi").exists()) {
                onProgress(YaziInstallProgress.Failed("yazi binary not found in downloaded archive"))
                return
            }

            onProgress(YaziInstallProgress.Done)
        } catch (e: Exception) {
            onProgress(YaziInstallProgress.Failed(e.message ?: e.toString()))
        }
    }

    /** Returns the shell command to hand to a TerminalSession so `yazi`
     *  resolves inside the workspace's own $HOME/.local/bin via $PATH. */
    fun launchCommand(): String = "PATH=\"\$HOME/.local/bin:\$PATH\" yazi"
}