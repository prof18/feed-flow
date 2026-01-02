package com.prof18.feedflow.shared.domain.csv

import java.io.File

actual data class CsvOutput(
    val file: File,
) {
    actual fun writeText(text: String) {
        file.writeText(text, Charsets.UTF_8)
    }
}
