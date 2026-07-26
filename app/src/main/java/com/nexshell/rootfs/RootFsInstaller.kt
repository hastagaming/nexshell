package com.nexshell.rootfs

import com.nexshell.core.Workspace
import io.airlift.compress.zstd.ZstdInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

sealed class InstallProgress {
    data class Downloading(val bytesRead: Long, val totalBytes: Long) : InstallProgress()
    data class Extracting(val entriesDone: Int) : InstallProgress()
    data class Failed(val reason: String) : InstallProgress()
    object VerifyingChecksum : InstallProgress()
    object Done : InstallProgress()
}

class RootFsInstaller {

    fun install(
        workspace: Workspace,
        source: RootFsSource,
        onProgress: (InstallProgress) -> Unit
    ) {
        try {
            val cacheDir = File(workspace.rootDir, ".cache").apply { mkdirs() }
            val archiveFile = File(cacheDir, "rootfs-download")

            downloadTo(source.archiveUrl, archiveFile, onProgress)

            if (source.sha256.isNotBlank()) {
                onProgress(InstallProgress.VerifyingChecksum)
                val actual = sha256Of(archiveFile)
                if (!actual.equals(source.sha256, ignoreCase = true)) {
                    onProgress(InstallProgress.Failed("Checksum mismatch: expected ${source.sha256}, got $actual"))
                    return
                }
            }

            extractTarball(archiveFile, workspace.filesDir, onProgress)

            archiveFile.delete()
            onProgress(InstallProgress.Done)
        } catch (e: Exception) {
            onProgress(InstallProgress.Failed(e.message ?: e.toString()))
        }
    }

    fun importCustomRootFs(workspace: Workspace, archiveFile: File, onProgress: (InstallProgress) -> Unit) {
        try {
            extractTarball(archiveFile, workspace.filesDir, onProgress)
            onProgress(InstallProgress.Done)
        } catch (e: Exception) {
            onProgress(InstallProgress.Failed(e.message ?: e.toString()))
        }
    }

    fun exportRootFs(workspace: Workspace, destination: File, onProgress: (InstallProgress) -> Unit) {
        try {
            org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(
                GZIPOutputStreamCompat(FileOutputStream(destination))
            ).use { tarOut ->
                tarOut.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU)
                var count = 0
                workspace.filesDir.walkTopDown().forEach { file ->
                    val relPath = file.relativeTo(workspace.filesDir).path
                    if (relPath.isEmpty()) return@forEach
                    val entry = TarArchiveEntry(file, relPath)
                    tarOut.putArchiveEntry(entry)
                    if (file.isFile) file.inputStream().use { it.copyTo(tarOut) }
                    tarOut.closeArchiveEntry()
                    count++
                    if (count % 200 == 0) onProgress(InstallProgress.Extracting(count))
                }
            }
            onProgress(InstallProgress.Done)
        } catch (e: Exception) {
            onProgress(InstallProgress.Failed(e.message ?: e.toString()))
        }
    }

    private fun downloadTo(url: String, dest: File, onProgress: (InstallProgress) -> Unit) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
        }
        conn.connect()
        if (conn.responseCode !in 200..299) {
            throw IllegalStateException("Download failed: HTTP ${conn.responseCode} for $url")
        }
        val total = conn.contentLengthLong
        var readTotal = 0L
        BufferedInputStream(conn.inputStream).use { input ->
            FileOutputStream(dest).use { output ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    output.write(buf, 0, n)
                    readTotal += n
                    onProgress(InstallProgress.Downloading(readTotal, total))
                }
            }
        }
    }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun extractTarball(archiveFile: File, targetDir: File, onProgress: (InstallProgress) -> Unit) {
        targetDir.mkdirs()

        val rawStream = BufferedInputStream(archiveFile.inputStream())
        val decompressed = when (detectFormat(archiveFile)) {
            TarFormat.GZIP -> GZIPInputStream(rawStream)
            TarFormat.ZSTD -> ZstdInputStream(rawStream)
            TarFormat.XZ -> XZCompressorInputStream(rawStream)
            TarFormat.PLAIN -> rawStream
        }

        var count = 0
        TarArchiveInputStream(decompressed).use { tarIn ->
            var entry: TarArchiveEntry? = tarIn.nextTarEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)

                // Path traversal guard — refuse entries that escape targetDir.
                if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    throw SecurityException("Rejected unsafe tar entry: ${entry.name}")
                }

                when {
                    entry.isDirectory -> outFile.mkdirs()
                    entry.isSymbolicLink -> {
                        outFile.parentFile?.mkdirs()
                        runCatching {
                            java.nio.file.Files.createSymbolicLink(
                                outFile.toPath(), File(entry.linkName).toPath()
                            )
                        }
                    }
                    else -> {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { out -> tarIn.copyTo(out) }
                        // preserve executable bit — critical for /usr/bin binaries in the rootfs
                        if (entry.mode and 0b001_000_000 != 0) outFile.setExecutable(true, false)
                    }
                }
                count++
                if (count % 300 == 0) onProgress(InstallProgress.Extracting(count))
                entry = tarIn.nextTarEntry
            }
        }
    }

    private enum class TarFormat { PLAIN, GZIP, ZSTD, XZ }

    private fun detectFormat(file: File): TarFormat {
        val header = ByteArray(4)
        file.inputStream().use { it.read(header) }
        return when {
            header[0] == 0x1F.toByte() && header[1] == 0x8B.toByte() -> TarFormat.GZIP
            header[0] == 0x28.toByte() && header[1] == 0xB5.toByte() && header[2] == 0x2F.toByte() -> TarFormat.ZSTD
            header[0] == 0xFD.toByte() && header[1] == '7'.code.toByte() -> TarFormat.XZ
            else -> TarFormat.PLAIN
        }
    }
}

// Thin wrapper so exportRootFs above can use GZIPOutputStream without an
// extra top-level import collision in this file.
private fun GZIPOutputStreamCompat(out: FileOutputStream) = java.util.zip.GZIPOutputStream(out)