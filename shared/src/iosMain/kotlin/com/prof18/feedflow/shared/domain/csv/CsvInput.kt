package com.prof18.feedflow.shared.domain.csv

import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create

actual data class CsvInput(
    val csvData: NSData,
) {
    actual fun readText(): String =
        NSString.create(csvData, NSUTF8StringEncoding)?.toString()
            ?: error("CSV input data is not UTF-8")
}
