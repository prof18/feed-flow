package com.prof18.feedflow.shared.domain.csv

import java.io.File

actual data class CsvInput(
    val file: File,
) {
    actual fun readText(): String = file.readText(Charsets.UTF_8)
}
