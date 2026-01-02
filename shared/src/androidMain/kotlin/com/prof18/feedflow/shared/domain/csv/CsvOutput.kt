package com.prof18.feedflow.shared.domain.csv

import java.io.OutputStream

actual data class CsvOutput(
    val outputStream: OutputStream?,
) {
    actual fun writeText(text: String) {
        val stream = outputStream ?: error("CSV output stream is null")
        stream.bufferedWriter().use { it.write(text) }
    }
}
