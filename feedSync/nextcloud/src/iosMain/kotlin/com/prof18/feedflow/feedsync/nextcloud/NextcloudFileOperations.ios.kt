package com.prof18.feedflow.feedsync.nextcloud

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToURL
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
internal actual class NextcloudFileOperationsImpl : NextcloudFileOperations {
    override fun readFile(filePath: String): ByteArray {
        val url = NSURL.fileURLWithPath(filePath)
        val data = NSData.dataWithContentsOfURL(url)
            ?: throw NextcloudException.FileNotFoundException("Could not read file: $filePath")

        return ByteArray(data.length.toInt()).apply {
            usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
        }
    }

    override fun writeFile(filePath: String, content: ByteArray) {
        val url = NSURL.fileURLWithPath(filePath)
        val parentUrl = url.URLByDeletingLastPathComponent
        if (parentUrl != null) {
            NSFileManager.defaultManager.createDirectoryAtURL(
                parentUrl,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
        }

        val data = content.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = content.size.toULong())
        }

        data.writeToURL(url, atomically = true)
    }
}
