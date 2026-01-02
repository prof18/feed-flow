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
        (text as NSString).writeToURL(
            url = url,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
    }
}
