package com.nexshell.pty

object PtyNative {
    init {
        System.loadLibrary("nexshell_pty")
    }

    external fun forkPty(
        cmd: String,
        args: Array<String>,
        env: Array<String>,
        cwd: String?,
        rows: Int,
        cols: Int
    ): LongArray?

    external fun readFd(fd: Int, buf: ByteArray, maxLen: Int): Int
    external fun writeFd(fd: Int, buf: ByteArray, len: Int): Int
    external fun resize(fd: Int, rows: Int, cols: Int)
    external fun closeFd(fd: Int)
    external fun waitForExit(pid: Int): Int
    external fun sendSignal(pid: Int, sig: Int)
}