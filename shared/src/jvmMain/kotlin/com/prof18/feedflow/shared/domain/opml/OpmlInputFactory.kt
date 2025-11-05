package com.prof18.feedflow.shared.domain.opml

import java.io.File
import java.nio.file.Files

actual fun createOpmlInputFromByteArray(data: ByteArray): OpmlInput {
    val tempFile = Files.createTempFile("feedflow-opml-import-", ".opml").toFile()
    tempFile.writeBytes(data)
    tempFile.deleteOnExit()
    return OpmlInput(file = tempFile)
}
