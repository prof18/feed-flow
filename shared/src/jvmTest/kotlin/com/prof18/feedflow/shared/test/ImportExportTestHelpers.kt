package com.prof18.feedflow.shared.test

import com.prof18.feedflow.shared.domain.csv.CsvInput
import com.prof18.feedflow.shared.domain.csv.CsvOutput
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import java.io.File
import kotlin.io.path.createTempDirectory

actual fun createOpmlInput(content: String): OpmlInput {
    val file = File.createTempFile("opml-input-", ".opml").apply {
        writeText(content, Charsets.UTF_8)
        deleteOnExit()
    }
    return OpmlInput(file = file)
}

actual fun createOpmlOutput(): OpmlOutput {
    val file = File.createTempFile("opml-output-", ".opml").apply {
        deleteOnExit()
    }
    return OpmlOutput(file = file)
}

actual fun createCsvInput(content: String): CsvInput {
    val file = File.createTempFile("csv-input-", ".csv").apply {
        writeText(content, Charsets.UTF_8)
        deleteOnExit()
    }
    return CsvInput(file = file)
}

actual fun createCsvOutput(): CsvOutput {
    val file = File.createTempFile("csv-output-", ".csv").apply {
        deleteOnExit()
    }
    return CsvOutput(file = file)
}

actual fun createFailingCsvOutput(): CsvOutput {
    val directory = createTempDirectory(prefix = "csv-output-dir-").toFile().apply {
        deleteOnExit()
    }
    return CsvOutput(file = directory)
}
