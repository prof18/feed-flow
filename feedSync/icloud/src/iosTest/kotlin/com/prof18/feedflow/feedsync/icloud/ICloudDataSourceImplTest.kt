package com.prof18.feedflow.feedsync.icloud

import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.getBytes
import platform.Foundation.writeToURL
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertIs

class ICloudDataSourceImplTest {

    @Test
    fun `local folder resolver supports upload and download success path`() = runBlocking {
        val temporaryDirectory = platform.Foundation.NSTemporaryDirectory().trimEnd('/')
        val root = "$temporaryDirectory/feedflow-icloud-${NSUUID.UUID().UUIDString}"
        val cloudRoot = "$root/cloud"
        val temporaryRoot = "$root/temporary"
        createDirectory(cloudRoot)
        createDirectory(temporaryRoot)

        val databaseName = "FeedFlowFeedSyncDB-debug.db"
        val source = NSURL.fileURLWithPath("$root/source.db")
        val bytes = byteArrayOf(1, 2, 3, 5, 8)
        bytes.toNSData().writeToURL(source, atomically = true)
        val dataSource = ICloudDataSourceImpl(
            logger = Logger(StaticConfig(logWriterList = emptyList())),
            localBaseFolderURL = NSURL.fileURLWithPath(cloudRoot),
            localTemporaryFolderURL = NSURL.fileURLWithPath(temporaryRoot),
        )

        assertIs<ICloudUploadResult.Success>(dataSource.performUpload(source, databaseName))
        val cloudFile = requireNotNull(NSURL.fileURLWithPath(cloudRoot).URLByAppendingPathComponent(databaseName))
        assertContentEquals(bytes, requireNotNull(NSData.create(contentsOfURL = cloudFile)).toByteArray())

        assertIs<ICloudDownloadResult.Success>(dataSource.performDownload(databaseName))
        val temporaryFile = requireNotNull(
            NSURL.fileURLWithPath(temporaryRoot).URLByAppendingPathComponent(databaseName),
        )
        assertContentEquals(bytes, requireNotNull(NSData.create(contentsOfURL = temporaryFile)).toByteArray())

        NSFileManager.defaultManager.removeItemAtPath(root, null)
        Unit
    }

    private fun createDirectory(path: String) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }
}

private fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isNotEmpty()) {
        result.usePinned { pinned -> getBytes(pinned.addressOf(0), length) }
    }
    return result
}

private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData()
    } else {
        usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }
