package com.prof18.feedflow.shared.test

import com.prof18.feedflow.shared.domain.csv.CsvInput
import com.prof18.feedflow.shared.domain.csv.CsvOutput
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

actual fun createOpmlInput(content: String): OpmlInput =
    OpmlInput { ByteArrayInputStream(content.toByteArray()) }

actual fun createOpmlOutput(): OpmlOutput =
    OpmlOutput { ByteArrayOutputStream() }

actual fun createCsvInput(content: String): CsvInput =
    CsvInput { ByteArrayInputStream(content.toByteArray()) }

actual fun createCsvOutput(): CsvOutput =
    CsvOutput { ByteArrayOutputStream() }

actual fun createFailingCsvOutput(): CsvOutput =
    CsvOutput {
        object : OutputStream() {
            override fun write(b: Int) {
                throw IOException("Failed to write CSV")
            }
        }
    }
