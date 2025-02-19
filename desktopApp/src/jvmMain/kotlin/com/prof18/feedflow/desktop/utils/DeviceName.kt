package com.prof18.feedflow.desktop.utils

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

@Suppress("SwallowedException")
fun getUnixDeviceName(): String = try {
    executeCommand("uname -n") ?: "desktop"
} catch (e: IOException) {
    "desktop"
}

private fun executeCommand(command: String): String? {
    val process = Runtime.getRuntime().exec(command)
    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
        return reader.readLine()?.trim()
    }
}
