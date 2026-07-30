package com.nexshell.rootfs

import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Downloads the official prebuilt proot binary published by termux-packages
 * (the same binary Termux itself ships via `pkg install proot`) instead of
 * compiling proot from source — compiling proot's C source against NDK has
 * proven fragile across CI environments, while the prebuilt binary is the
 * exact artifact millions of Termux installs already run on-device.
 */
object ProotInstaller {

    private fun debArchName(): String = when (Build.SUPPORTED_ABIS.firstOrNull()) {
        "arm64-v8a" -> "aarch64"
        "armeabi-v7a" -> "arm"
        "x86_64" -> "x86_64"
        else -> "aarch64"
    }

    fun isInstalled(filesDir: File): Boolean =
        File(filesDir, "nexshell_proot").exists()

    fun installedPath(filesDir: File): String =
        File(filesDir, "nexshell_proot").absolutePath

    /**
     * termux-packages publishes proot as a .deb; we extract just the proot
     * ELF binary from data.tar.xz inside it and stage it into the app's own
     * files dir (executable, since native lib dirs are read-only at runtime
     * but app-private files dirs allow chmod +x on most Android versions).
     */
    fun install(filesDir: File, onProgress: (String) -> Unit): File {
        val arch = debArchName()
        val url = "https://packages.termux.dev/apt/termux-main/pool/main/p/proot/" // index page — real filename resolved below
        onProgress("Resolving proot package URL…")

        // The exact filename includes a version string that changes over
        // time; instead of hardcoding it, we fetch the Packages index and
        // find the current proot_<version>_<arch>.deb entry.
        val indexUrl = "https://packages.termux.dev/apt/termux-main/dists/stable/main/binary-$arch/Packages"
        val packageLine = fetchText(indexUrl)
            .lineSequence()
            .windowed(size = 20, step = 1, partialWindows = true)
            .firstOrNull { block -> block.any { it.startsWith("Package: proot") } }
            ?: throw IllegalStateException("Could not find proot package entry for $arch")

        val filenameLine = packageLine.firstOrNull { it.startsWith("Filename:") }
            ?: throw IllegalStateException("Could not resolve proot .deb filename")
        val debPath = filenameLine.removePrefix("Filename:").trim()
        val debUrl = "https://packages.termux.dev/apt/termux-main/$debPath"

        onProgress("Downloading proot package…")
        val debFile = File(filesDir, "proot.deb")
        downloadTo(debUrl, debFile)

        onProgress("Extracting proot binary…")
        val extractedBinary = extractProotFromDeb(debFile, filesDir)
        debFile.delete()

        extractedBinary.setExecutable(true, false)
        onProgress("proot installed ✓")
        return extractedBinary
    }

    private fun fetchText(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
        }
        conn.connect()
        return conn.inputStream.bufferedReader().readText()
    }

    private fun downloadTo(url: String, dest: File) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
        }
        conn.connect()
        conn.inputStream.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        }
    }

    /**
     * A .deb is an ar(1) archive containing control.tar.*, data.tar.*, and
     * debian-binary. We only need data.tar.xz's usr/bin/proot entry.
     */
    private fun extractProotFromDeb(debFile: File, outDir: File): File {
        // .deb = ar archive: parse the ar member headers manually (no
        // external ar/dpkg tool available on Android), locate data.tar.xz.
        val bytes = debFile.readBytes()
        var offset = 8 // skip "!<arch>\n" magic
        var dataTarXz: ByteArray? = null

        while (offset < bytes.size) {
            val header = String(bytes, offset, 60, Charsets.US_ASCII)
            val name = header.substring(0, 16).trim()
            val sizeStr = header.substring(48, 58).trim()
            val size = sizeStr.toIntOrNull() ?: break
            val contentStart = offset + 60
            if (name.startsWith("data.tar")) {
                dataTarXz = bytes.copyOfRange(contentStart, contentStart + size)
                break
            }
            offset = contentStart + size + (size % 2) // ar entries are 2-byte aligned
        }

        val dataBytes = dataTarXz ?: throw IllegalStateException("data.tar member not found in .deb")

        val decompressed = if (dataBytes.size > 4 && dataBytes[0] == 0xFD.toByte()) {
            org.apache.commons.compress.compressors.xz.XZCompressorInputStream(dataBytes.inputStream())
        } else {
            java.util.zip.GZIPInputStream(dataBytes.inputStream())
        }

        org.apache.commons.compress.archivers.tar.TarArchiveInputStream(decompressed).use { tarIn ->
            var entry = tarIn.nextTarEntry
            while (entry != null) {
                if (entry.name.endsWith("bin/proot")) {
                    val outFile = File(outDir, "nexshell_proot")
                    FileOutputStream(outFile).use { out -> tarIn.copyTo(out) }
                    return outFile
                }
                entry = tarIn.nextTarEntry
            }
        }
        throw IllegalStateException("proot binary not found inside data.tar")
    }
}