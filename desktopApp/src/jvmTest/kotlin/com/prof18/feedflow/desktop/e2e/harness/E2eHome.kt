package com.prof18.feedflow.desktop.e2e.harness

import java.io.File
import kotlin.random.Random

/** Per-test folder: the app's data folder, the file-dialog response file and the opened-URI log. */
internal class E2eHome private constructor(val dir: File) {
    val dataDir = File(dir, "data")
    val fileDialogResponse = File(dir, "file-dialog-response.txt")
    val openedUriLog = File(dir, "opened-uris.txt")
    val spectreDir = File(dir, "spectre")

    companion object {
        private const val NAME_LENGTH = 8
        private const val MAX_SOCKET_PATH = 100

        fun create(): E2eHome {
            val name = (1..NAME_LENGTH).map { 'a' + Random.nextInt(26) }.joinToString("")
            val home = E2eHome(File(HOME_ROOT, name))
            home.dataDir.mkdirs()
            home.spectreDir.mkdirs()
            val socketPath = File(home.dataDir, "FeedFlow.sock").absolutePath
            check(socketPath.length < MAX_SOCKET_PATH) { "socket path too long for macOS: $socketPath" }
            return home
        }
    }
}
