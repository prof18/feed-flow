package com.prof18.feedflow.shared.domain.csv

import java.io.InputStream

actual data class CsvInput(
    val inputStream: InputStream?,
) {
    actual fun readText(): String {
        val stream = inputStream ?: error("CSV input stream is null")
        return stream.bufferedReader().use { it.readText() }
    }
}
