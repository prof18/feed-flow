package com.prof18.feedflow.feedsync.nextcloud

import java.io.File

internal actual class NextcloudFileOperationsImpl : NextcloudFileOperations {
    override fun readFile(filePath: String): ByteArray {
        return File(filePath).readBytes()
    }

    override fun writeFile(filePath: String, content: ByteArray) {
        val file = File(filePath)
        file.parentFile?.mkdirs()
        file.writeBytes(content)
    }
}
