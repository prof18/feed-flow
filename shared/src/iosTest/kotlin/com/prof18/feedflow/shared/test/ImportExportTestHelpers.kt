package com.prof18.feedflow.shared.test

import com.prof18.feedflow.shared.domain.csv.CsvInput
import com.prof18.feedflow.shared.domain.csv.CsvOutput
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding

actual fun createOpmlInput(content: String): OpmlInput {
    val data = NSString.create(string = content).dataUsingEncoding(NSUTF8StringEncoding)
        ?: error("Failed to encode OPML content")
    return OpmlInput(opmlData = data)
}

actual fun createOpmlOutput(): OpmlOutput {
    val tempPath = NSTemporaryDirectory() + "opml-output-" + NSUUID().UUIDString + ".opml"
    val url = NSURL.fileURLWithPath(tempPath)
    return OpmlOutput(url = url)
}

actual fun createCsvInput(content: String): CsvInput {
    val data = NSString.create(string = content).dataUsingEncoding(NSUTF8StringEncoding)
        ?: error("Failed to encode CSV content")
    return CsvInput(csvData = data)
}

actual fun createCsvOutput(): CsvOutput {
    val tempPath = NSTemporaryDirectory() + "csv-output-" + NSUUID().UUIDString + ".csv"
    val url = NSURL.fileURLWithPath(tempPath)
    return CsvOutput(url = url)
}

actual fun createFailingCsvOutput(): CsvOutput {
    val url = NSURL(string = "invalid://csv-output-" + NSUUID().UUIDString)
    return CsvOutput(url = url)
}
