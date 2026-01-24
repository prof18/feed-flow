package com.prof18.feedflow.shared.domain.csv

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.writeToURL

@OptIn(ExperimentalForeignApi::class)
actual data class CsvOutput(
    val url: NSURL,
) {
    actual fun writeText(text: String) {
        require(url.isFileURL()) { "CSV output url must be a file url" }
        val didWrite = (text as NSString).writeToURL(
            url = url,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
        check(didWrite) { "Failed to write CSV output" }
    }
}
