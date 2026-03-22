package com.prof18.feedflow.shared.domain.csv

import java.io.InputStream

actual class CsvInput(
    val inputStreamProvider: () -> InputStream?,
) {
    actual fun readText(): String {
        val stream = inputStreamProvider() ?: error("CSV input stream is null")
        return stream.bufferedReader().use { it.readText() }
    }
}
