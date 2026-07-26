package com.nexshell.rootfs

data class RootFsSource(
    val distroId: String,
    val displayName: String,
    val archiveUrl: String,
    val sha256: String
)

object RootFsCatalog {
    // Official minimal rootfs tarballs, matching what proot-distro/Termux use.
    val UBUNTU = RootFsSource(
        distroId = "ubuntu",
        displayName = "Ubuntu 24.04",
        archiveUrl = "https://partner-images.canonical.com/oci/noble/current/ubuntu-noble-oci-arm64-root.tar.gz",
        sha256 = "" // filled at install time from the published SHA256SUMS file
    )
    val DEBIAN = RootFsSource(
        distroId = "debian",
        displayName = "Debian 13",
        archiveUrl = "https://github.com/debuerreotype/docker-debian-artifacts/raw/dist-arm64/trixie/rootfs.tar.xz",
        sha256 = ""
    )
    val ALPINE = RootFsSource(
        distroId = "alpine",
        displayName = "Alpine",
        archiveUrl = "https://dl-cdn.alpinelinux.org/alpine/latest-stable/releases/aarch64/alpine-minirootfs-3.20.3-aarch64.tar.gz",
        sha256 = ""
    )
}