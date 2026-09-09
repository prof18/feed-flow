package com.prof18.feedflow.feedsync.icloud

import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSCocoaErrorDomain
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileReadNoSuchFileError
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.getBytes
import platform.Foundation.writeToURL
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ICloudDataSourceImplTest {

    @Test
    fun `local folder resolver supports upload and download success path`() = runBlocking {
        val temporaryDirectory = NSTemporaryDirectory().trimEnd('/')
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
            fileCoordinator = FakeICloudFileCoordinator(),
        )

        val firstUpload = dataSource.performUpload(source, databaseName)
        assertIs<ICloudUploadResult.Success>(firstUpload, firstUpload.toString())
        val cloudFile = requireNotNull(NSURL.fileURLWithPath(cloudRoot).URLByAppendingPathComponent(databaseName))
        assertContentEquals(bytes, requireNotNull(NSData.create(contentsOfURL = cloudFile)).toByteArray())

        assertIs<ICloudDownloadResult.Success>(dataSource.performDownload(databaseName))
        val temporaryFile = requireNotNull(
            NSURL.fileURLWithPath(temporaryRoot).URLByAppendingPathComponent(databaseName),
        )
        assertContentEquals(bytes, requireNotNull(NSData.create(contentsOfURL = temporaryFile)).toByteArray())

        val updated = byteArrayOf(13, 21, 34)
        updated.toNSData().writeToURL(source, atomically = true)
        assertIs<ICloudUploadResult.Success>(dataSource.performUpload(source, databaseName))
        assertContentEquals(updated, requireNotNull(NSData.create(contentsOfURL = cloudFile)).toByteArray())

        NSFileManager.defaultManager.removeItemAtURL(source, null)
        assertIs<ICloudUploadResult.Error>(dataSource.performUpload(source, databaseName))
        assertContentEquals(updated, requireNotNull(NSData.create(contentsOfURL = cloudFile)).toByteArray())
        assertIs<ICloudDownloadResult.Error.FileNotFound>(dataSource.performDownload("missing.db"))

        NSFileManager.defaultManager.removeItemAtPath(root, null)
        Unit
    }

    @Test
    fun `coordination denial preserves existing upload and download targets`() = runBlocking {
        withFixture { root, cloudRoot, temporaryRoot ->
            val databaseName = "database.db"
            val source = NSURL.fileURLWithPath("$root/source.db")
            val cloudFile = assertNotNull(cloudRoot.URLByAppendingPathComponent(databaseName))
            val temporaryFile = assertNotNull(temporaryRoot.URLByAppendingPathComponent(databaseName))
            val cloudBytes = "existing cloud snapshot".encodeToByteArray()
            val temporaryBytes = "existing local snapshot".encodeToByteArray()
            writeBytes(source, "new database snapshot".encodeToByteArray())
            writeBytes(cloudFile, cloudBytes)
            writeBytes(temporaryFile, temporaryBytes)
            val coordinator = FakeICloudFileCoordinator(
                readError = testError(),
                writeError = testError(),
            )
            val dataSource = createDataSource(cloudRoot, temporaryRoot, coordinator)

            assertIs<ICloudUploadResult.Error>(dataSource.performUpload(source, databaseName))
            assertContentEquals(cloudBytes, readBytes(cloudFile))
            assertIs<ICloudDownloadResult.Error>(dataSource.performDownload(databaseName))
            assertContentEquals(temporaryBytes, readBytes(temporaryFile))
            assertFalse(coordinator.readAccessorCalled)
            assertFalse(coordinator.writeAccessorCalled)
        }
    }

    @Test
    fun `coordination accessor relocation controls the actual cloud file`() = runBlocking {
        withFixture { root, cloudRoot, temporaryRoot ->
            val databaseName = "database.db"
            val source = NSURL.fileURLWithPath("$root/source.db")
            val nominalCloudFile = assertNotNull(cloudRoot.URLByAppendingPathComponent(databaseName))
            val relocatedRoot = NSURL.fileURLWithPath("$root/relocated")
            createDirectory(assertNotNull(relocatedRoot.path))
            val relocatedCloudFile = assertNotNull(relocatedRoot.URLByAppendingPathComponent(databaseName))
            val nominalBytes = "nominal cloud snapshot".encodeToByteArray()
            val uploadedBytes = "uploaded snapshot".encodeToByteArray()
            writeBytes(source, uploadedBytes)
            writeBytes(nominalCloudFile, nominalBytes)
            val coordinator = FakeICloudFileCoordinator(
                readAccessorUrl = relocatedCloudFile,
                writeAccessorUrl = relocatedCloudFile,
            )
            val dataSource = createDataSource(cloudRoot, temporaryRoot, coordinator)

            assertIs<ICloudUploadResult.Success>(dataSource.performUpload(source, databaseName))
            assertContentEquals(uploadedBytes, readBytes(relocatedCloudFile))
            assertContentEquals(nominalBytes, readBytes(nominalCloudFile))

            val relocatedReadBytes = "relocated read snapshot".encodeToByteArray()
            writeBytes(relocatedCloudFile, relocatedReadBytes)
            val result = assertIs<ICloudDownloadResult.Success>(dataSource.performDownload(databaseName))
            assertContentEquals(relocatedReadBytes, readBytes(result.destinationUrl))
            assertContentEquals(nominalBytes, readBytes(nominalCloudFile))
        }
    }

    @Test
    fun `coordination without an accessor invocation cannot report success`() = runBlocking {
        withFixture { root, cloudRoot, temporaryRoot ->
            val databaseName = "database.db"
            val source = NSURL.fileURLWithPath("$root/source.db")
            val cloudFile = assertNotNull(cloudRoot.URLByAppendingPathComponent(databaseName))
            val temporaryFile = assertNotNull(temporaryRoot.URLByAppendingPathComponent(databaseName))
            val cloudBytes = "existing cloud snapshot".encodeToByteArray()
            val temporaryBytes = "existing local snapshot".encodeToByteArray()
            writeBytes(source, "new database snapshot".encodeToByteArray())
            writeBytes(cloudFile, cloudBytes)
            writeBytes(temporaryFile, temporaryBytes)
            val coordinator = FakeICloudFileCoordinator(invokeAccessor = false)
            val dataSource = createDataSource(cloudRoot, temporaryRoot, coordinator)

            assertIs<ICloudUploadResult.Error>(dataSource.performUpload(source, databaseName))
            assertContentEquals(cloudBytes, readBytes(cloudFile))
            assertIs<ICloudDownloadResult.Error>(dataSource.performDownload(databaseName))
            assertContentEquals(temporaryBytes, readBytes(temporaryFile))
        }
    }

    @Test
    fun `missing file coordination error remains file not found`() = runBlocking {
        withFixture { _, cloudRoot, temporaryRoot ->
            val coordinator = FakeICloudFileCoordinator(
                readError = testError(
                    code = NSFileReadNoSuchFileError,
                    domain = requireNotNull(NSCocoaErrorDomain),
                ),
            )
            val dataSource = createDataSource(cloudRoot, temporaryRoot, coordinator)

            assertIs<ICloudDownloadResult.Error.FileNotFound>(dataSource.performDownload("missing.db"))
        }
    }

    private suspend fun withFixture(block: suspend (String, NSURL, NSURL) -> Unit) {
        val root = "${NSTemporaryDirectory().trimEnd('/')}/feedflow-icloud-${NSUUID.UUID().UUIDString}"
        val cloudRoot = NSURL.fileURLWithPath("$root/cloud")
        val temporaryRoot = NSURL.fileURLWithPath("$root/temporary")
        createDirectory(assertNotNull(cloudRoot.path))
        createDirectory(assertNotNull(temporaryRoot.path))
        try {
            block(root, cloudRoot, temporaryRoot)
        } finally {
            NSFileManager.defaultManager.removeItemAtPath(root, null)
        }
    }

    private fun createDataSource(
        cloudRoot: NSURL,
        temporaryRoot: NSURL,
        coordinator: ICloudFileCoordinator,
    ): ICloudDataSourceImpl = ICloudDataSourceImpl(
        logger = Logger(StaticConfig(logWriterList = emptyList())),
        localBaseFolderURL = cloudRoot,
        localTemporaryFolderURL = temporaryRoot,
        fileCoordinator = coordinator,
    )

    private fun writeBytes(url: NSURL, bytes: ByteArray) {
        check(bytes.toNSData().writeToURL(url, atomically = true))
    }

    private fun readBytes(url: NSURL): ByteArray = assertNotNull(NSData.create(contentsOfURL = url)).toByteArray()

    private fun testError(
        code: Long = 1L,
        domain: String = "ICloudDataSourceImplTest",
    ): NSError = NSError.errorWithDomain(domain, code, null)

    private fun createDirectory(path: String) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }
}

private class FakeICloudFileCoordinator(
    private val readError: NSError? = null,
    private val writeError: NSError? = null,
    private val readAccessorUrl: NSURL? = null,
    private val writeAccessorUrl: NSURL? = null,
    private val invokeAccessor: Boolean = true,
) : ICloudFileCoordinator {
    var readAccessorCalled = false
        private set
    var writeAccessorCalled = false
        private set

    override fun read(url: NSURL, accessor: (NSURL) -> Unit): NSError? {
        if (readError != null) return readError
        if (invokeAccessor) {
            readAccessorCalled = true
            accessor(readAccessorUrl ?: url)
        }
        return null
    }

    override fun write(url: NSURL, accessor: (NSURL) -> Unit): NSError? {
        if (writeError != null) return writeError
        if (invokeAccessor) {
            writeAccessorCalled = true
            accessor(writeAccessorUrl ?: url)
        }
        return null
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
