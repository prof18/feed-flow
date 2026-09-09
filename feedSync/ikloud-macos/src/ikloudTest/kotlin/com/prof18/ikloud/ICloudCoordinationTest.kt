package com.prof18.ikloud

import com.prof18.feedflow.feedsync.icloud.apple.LocalICloudFileDiscovery
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSCocoaErrorDomain
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileReadNoSuchFileError
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.getBytes
import platform.Foundation.writeToURL
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ICloudCoordinationTest {

    @Test
    fun deniedWritePreservesTheExistingCloudFile() {
        withFixture { databaseUrl, cloudUrl, _ ->
            val previous = "existing cloud snapshot".encodeToByteArray()
            writeBytes(cloudUrl, previous)
            writeBytes(databaseUrl, "new database snapshot".encodeToByteArray())

            val coordinator = FakeCoordinator(writeError = testError())

            assertEquals(2, uploadToICloud(databaseUrl, cloudUrl, coordinator))
            assertContentEquals(previous, readBytes(cloudUrl))
            assertFalse(coordinator.accessorCalled)
        }
    }

    @Test
    fun deniedReadPreservesTheDestinationFile() {
        withFixture { _, cloudUrl, destinationUrl ->
            val previous = "existing destination".encodeToByteArray()
            writeBytes(destinationUrl, previous)

            val coordinator = FakeCoordinator(readError = testError())

            assertEquals(3, downloadToFile(cloudUrl, destinationUrl, coordinator, LocalICloudFileDiscovery()))
            assertContentEquals(previous, readBytes(destinationUrl))
            assertFalse(coordinator.accessorCalled)
        }
    }

    @Test
    fun writeWithNoErrorAndNoCallbackDoesNotReportSuccess() {
        withFixture { databaseUrl, cloudUrl, _ ->
            writeBytes(cloudUrl, "existing cloud snapshot".encodeToByteArray())
            writeBytes(databaseUrl, "new database snapshot".encodeToByteArray())

            val coordinator = FakeCoordinator(invokeAccessor = false)

            assertEquals(2, uploadToICloud(databaseUrl, cloudUrl, coordinator))
            assertContentEquals("existing cloud snapshot".encodeToByteArray(), readBytes(cloudUrl))
        }
    }

    @Test
    fun readWithNoErrorAndNoCallbackDoesNotReportSuccess() {
        withFixture { _, cloudUrl, destinationUrl ->
            writeBytes(destinationUrl, "existing destination".encodeToByteArray())
            writeBytes(cloudUrl, "cloud snapshot".encodeToByteArray())

            val coordinator = FakeCoordinator(invokeAccessor = false)

            assertEquals(3, downloadToFile(cloudUrl, destinationUrl, coordinator, LocalICloudFileDiscovery()))
            assertContentEquals("existing destination".encodeToByteArray(), readBytes(destinationUrl))
        }
    }

    @Test
    fun redirectedWriteUsesTheAccessorUrl() {
        withFixture { databaseUrl, cloudUrl, _ ->
            val redirectedUrl = assertNotNull(cloudUrl.URLByAppendingPathExtension("coordinated"))
            val nominalBytes = "nominal cloud snapshot".encodeToByteArray()
            val databaseBytes = "database snapshot".encodeToByteArray()
            writeBytes(cloudUrl, nominalBytes)
            writeBytes(databaseUrl, databaseBytes)

            val coordinator = FakeCoordinator(writeAccessorUrl = redirectedUrl)

            assertEquals(0, uploadToICloud(databaseUrl, cloudUrl, coordinator))
            assertContentEquals(databaseBytes, readBytes(redirectedUrl))
            assertContentEquals(nominalBytes, readBytes(cloudUrl))
        }
    }

    @Test
    fun redirectedReadUsesTheAccessorUrl() {
        withFixture { _, cloudUrl, destinationUrl ->
            val redirectedUrl = assertNotNull(cloudUrl.URLByAppendingPathExtension("coordinated"))
            val nominalBytes = "nominal cloud snapshot".encodeToByteArray()
            writeBytes(cloudUrl, nominalBytes)
            writeBytes(redirectedUrl, "redirected cloud snapshot".encodeToByteArray())

            val coordinator = FakeCoordinator(readAccessorUrl = redirectedUrl)

            assertEquals(0, downloadToFile(cloudUrl, destinationUrl, coordinator, LocalICloudFileDiscovery()))
            assertContentEquals("redirected cloud snapshot".encodeToByteArray(), readBytes(destinationUrl))
            assertContentEquals(nominalBytes, readBytes(cloudUrl))
        }
    }

    @Test
    fun missingFileReadErrorMapsToFileNotFound() {
        withFixture { _, cloudUrl, destinationUrl ->
            val coordinator = FakeCoordinator(
                readError = testError(
                    code = NSFileReadNoSuchFileError,
                    domain = requireNotNull(NSCocoaErrorDomain),
                ),
            )

            assertEquals(5, downloadToFile(cloudUrl, destinationUrl, coordinator, LocalICloudFileDiscovery()))
        }
    }

    private fun withFixture(block: (NSURL, NSURL, NSURL) -> Unit) {
        val rootUrl = assertNotNull(
            NSURL.fileURLWithPath(NSTemporaryDirectory())
                .URLByAppendingPathComponent("ikloud-coordination-test-${NSUUID().UUIDString}"),
        )
        val fileManager = NSFileManager.defaultManager
        fileManager.createDirectoryAtURL(rootUrl, true, null, null)
        val databaseUrl = assertNotNull(rootUrl.URLByAppendingPathComponent("database.db"))
        val cloudUrl = assertNotNull(rootUrl.URLByAppendingPathComponent("cloud.db"))
        val destinationUrl = assertNotNull(rootUrl.URLByAppendingPathComponent("destination.db"))
        try {
            block(databaseUrl, cloudUrl, destinationUrl)
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
        val data = assertNotNull(NSData.dataWithContentsOfURL(url))
        val bytes = ByteArray(data.length.toInt())
        bytes.usePinned { pinned -> data.getBytes(pinned.addressOf(0), data.length) }
        return bytes
    }

    private fun testError(
        code: Long = 1L,
        domain: String = "ICloudCoordinationTest",
    ): NSError = NSError.errorWithDomain(domain, code, null)

    private class FakeCoordinator(
        private val readError: NSError? = null,
        private val writeError: NSError? = null,
        private val readAccessorUrl: NSURL? = null,
        private val writeAccessorUrl: NSURL? = null,
        private val invokeAccessor: Boolean = true,
    ) : ICloudFileCoordinator {
        var accessorCalled = false

        override fun read(url: NSURL, accessor: (NSURL) -> Unit): NSError? {
            if (readError != null) return readError
            if (invokeAccessor) {
                accessorCalled = true
                accessor(readAccessorUrl ?: url)
            }
            return null
        }

        override fun write(url: NSURL, accessor: (NSURL) -> Unit): NSError? {
            if (writeError != null) return writeError
            if (invokeAccessor) {
                accessorCalled = true
                accessor(writeAccessorUrl ?: url)
            }
            return null
        }
    }
}
