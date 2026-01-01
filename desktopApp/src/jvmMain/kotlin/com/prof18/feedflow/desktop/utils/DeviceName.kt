package com.prof18.feedflow.desktop.utils

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

@Suppress("SwallowedException")
fun getUnixDeviceName(): String = try {
    executeCommand(listOf("uname", "-n")) ?: "desktop"
} catch (e: IOException) {
    "desktop"
}

private fun executeCommand(command: List<String>): String? {
    val process = ProcessBuilder(command)
        .redirectErrorStream(true)
        .start()
    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
        val output = reader.readLine()?.trim()
        process.waitFor()
        return output
    }
}
