package com.prof18.ikloud

import com.prof18.feedflow.feedsync.icloud.apple.ICloudFileDiscovery
import com.prof18.feedflow.feedsync.icloud.apple.ICloudFileDiscoveryResult
import com.prof18.feedflow.feedsync.icloud.apple.LocalICloudFileDiscovery
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSUUID
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.getBytes
import platform.Foundation.writeToURL
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ICloudHelperTest {

    @Test
    fun failedUploadPreservesTheExistingContainerFile() {
        withFixture { databaseUrl, containerUrl, _ ->
            val previous = "last good backup".encodeToByteArray()
            writeBytes(containerUrl, previous)
            assertEquals(2, uploadToICloud(databaseUrl, containerUrl))
            assertContentEquals(previous, readBytes(containerUrl))
        }
    }

    @Test
    fun uploadReplacesAnExistingContainerFile() {
        withFixture { databaseUrl, containerUrl, _ ->
            writeBytes(containerUrl, "previous".encodeToByteArray())
            val updated = "updated snapshot".encodeToByteArray()
            writeBytes(databaseUrl, updated)
            assertEquals(0, uploadToICloud(databaseUrl, containerUrl))
            assertContentEquals(updated, readBytes(containerUrl))
        }
    }

    @Test
    fun uploadCreatesAnInitiallyAbsentDocumentsDirectory() {
        withFixture { databaseUrl, containerUrl, _ ->
            val firstSnapshot = "first cloud snapshot".encodeToByteArray()
            writeBytes(databaseUrl, firstSnapshot)
            val nestedCloudUrl = assertNotNull(
                containerUrl.URLByDeletingLastPathComponent
                    ?.URLByAppendingPathComponent("Documents")
                    ?.URLByAppendingPathComponent("container.db"),
            )

            assertEquals(0, uploadToICloud(databaseUrl, nestedCloudUrl))
            assertContentEquals(firstSnapshot, readBytes(nestedCloudUrl))
        }
    }

    @Test
    fun missingDownloadPreservesTheLocalDatabase() {
        withFixture { databaseUrl, containerUrl, tempUrl ->
            val previous = "last good local snapshot".encodeToByteArray()
            writeBytes(databaseUrl, previous)
            assertEquals(5, downloadToFile(containerUrl, tempUrl, fileDiscovery = LocalICloudFileDiscovery()))
            assertContentEquals(previous, readBytes(databaseUrl))
        }
    }

    @Test
    fun firstDownloadCreatesTheStagedFile() {
        withFixture { _, containerUrl, tempUrl ->
            val snapshot = "cloud snapshot".encodeToByteArray()
            writeBytes(containerUrl, snapshot)
            assertEquals(0, downloadToFile(containerUrl, tempUrl, fileDiscovery = LocalICloudFileDiscovery()))
            assertContentEquals(snapshot, readBytes(tempUrl))
        }
    }

    @Test
    fun uploadCopiesDatabaseBytesToTheLocalContainer() {
        withFixture { databaseUrl, containerUrl, _ ->
            val databaseBytes = byteArrayOf(0, 1, 127, -128, -1, 0, 42)
            writeBytes(databaseUrl, databaseBytes)

            assertEquals(
                expected = 0,
                actual = uploadToICloud(databaseUrl, containerUrl),
            )
            assertContentEquals(databaseBytes, readBytes(containerUrl))
        }
    }

    @Test
    fun downloadStagesContainerBytesWithoutReplacingTheDatabase() {
        withFixture { databaseUrl, containerUrl, tempUrl ->
            val originalDatabaseBytes = "original database bytes".encodeToByteArray()
            val containerBytes = "container bytes • download ✅".encodeToByteArray()
            writeBytes(databaseUrl, originalDatabaseBytes)
            writeBytes(containerUrl, containerBytes)

            assertEquals(
                expected = 0,
                actual = downloadToFile(
                    iCloudUrl = containerUrl,
                    destinationUrl = tempUrl,
                    fileDiscovery = LocalICloudFileDiscovery(),
                ),
            )
            assertContentEquals(containerBytes, readBytes(tempUrl))
            assertContentEquals(originalDatabaseBytes, readBytes(databaseUrl))
        }
    }

    @Test
    fun completedDiscoveryWithoutBackupUsesConfirmedRemoteMissingCode() {
        withFixture { _, containerUrl, tempUrl ->
            assertEquals(
                6,
                downloadToFile(
                    iCloudUrl = containerUrl,
                    destinationUrl = tempUrl,
                    fileDiscovery = FakeICloudFileDiscovery(ICloudFileDiscoveryResult.ConfirmedMissing),
                ),
            )
        }
    }

    @Test
    fun discoveryFailureDoesNotUseRemoteMissingCode() {
        withFixture { _, containerUrl, tempUrl ->
            assertEquals(
                3,
                downloadToFile(
                    iCloudUrl = containerUrl,
                    destinationUrl = tempUrl,
                    fileDiscovery = FakeICloudFileDiscovery(ICloudFileDiscoveryResult.Failure("timed out")),
                ),
            )
        }
    }

    private fun withFixture(block: (NSURL, NSURL, NSURL) -> Unit) {
        val rootUrl = assertNotNull(
            NSURL.fileURLWithPath(NSTemporaryDirectory())
                .URLByAppendingPathComponent("ikloud-test-${NSUUID().UUIDString}"),
        )
        val fileManager = NSFileManager.defaultManager
        fileManager.createDirectoryAtURL(
            url = rootUrl,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )

        val databaseUrl = assertNotNull(rootUrl.URLByAppendingPathComponent("database.db"))
        val containerUrl = assertNotNull(rootUrl.URLByAppendingPathComponent("container.db"))
        val tempUrl = assertNotNull(rootUrl.URLByAppendingPathComponent("temporary.db"))

        try {
            block(databaseUrl, containerUrl, tempUrl)
        } finally {
            fileManager.removeItemAtURL(rootUrl, null)
        }
    }

    private fun writeBytes(url: NSURL, bytes: ByteArray) {
        check(
            bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
                    .writeToURL(url, atomically = true)
            },
        )
    }

    private fun readBytes(url: NSURL): ByteArray {
        val data = NSData.dataWithContentsOfURL(url)
        assertNotNull(data)
        val bytes = ByteArray(data.length.toInt())
        bytes.usePinned { pinnedBytes ->
            data.getBytes(pinnedBytes.addressOf(0), length = data.length)
        }
        return bytes
    }
}

private class FakeICloudFileDiscovery(
    private val result: ICloudFileDiscoveryResult,
) : ICloudFileDiscovery {
    override suspend fun discoverAndMaterialize(
        targetUrl: NSURL,
        timeoutMillis: Long,
    ): ICloudFileDiscoveryResult = result
}
