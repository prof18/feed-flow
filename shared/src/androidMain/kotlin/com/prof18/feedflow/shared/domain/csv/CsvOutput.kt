package com.prof18.feedflow.shared.domain.csv

import java.io.OutputStream

actual class CsvOutput(
    val outputStreamProvider: () -> OutputStream?,
) {
    actual fun writeText(text: String) {
        val stream = outputStreamProvider() ?: error("CSV output stream is null")
        stream.bufferedWriter().use { it.write(text) }
    }
}
