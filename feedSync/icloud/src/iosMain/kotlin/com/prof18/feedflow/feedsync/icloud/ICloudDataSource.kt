package com.prof18.feedflow.feedsync.icloud

import co.touchlab.kermit.Logger
import com.prof18.feedflow.feedsync.icloud.apple.FoundationICloudFileDiscovery
import com.prof18.feedflow.feedsync.icloud.apple.ICloudFileDiscovery
import com.prof18.feedflow.feedsync.icloud.apple.ICloudFileDiscoveryResult
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.delay
import platform.Foundation.NSCocoaErrorDomain
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileReadNoSuchFileError
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import kotlin.time.Clock

interface ICloudDataSource {
    suspend fun performUpload(databasePath: NSURL, databaseName: String): ICloudUploadResult
    suspend fun performDownload(databaseName: String): ICloudDownloadResult
    suspend fun getICloudBaseFolderURL(
        timeoutSeconds: Int = 30,
        initialPollIntervalMs: Long = 500,
    ): NSURL?
}

class ICloudDataSourceImpl(
    private val logger: Logger,
    private val localBaseFolderURL: NSURL? = null,
    private val localTemporaryFolderURL: NSURL? = null,
    private val fileCoordinator: ICloudFileCoordinator = FoundationICloudFileCoordinator(),
    private val fileDiscovery: ICloudFileDiscovery = FoundationICloudFileDiscovery(),
) : ICloudDataSource {
    override suspend fun performUpload(databasePath: NSURL, databaseName: String): ICloudUploadResult {
        val iCloudUrl = getICloudFolderURL(databaseName)
            ?: return ICloudUploadResult.Error.ICloudUrlNotAvailable

        var accessorInvoked = false
        var uploadError: ICloudUploadResult.Error? = null
        val coordinationError = fileCoordinator.write(iCloudUrl) { coordinatedUrl ->
            accessorInvoked = true
            uploadError = upload(databasePath, coordinatedUrl, databaseName)
        }
        coordinationError?.let { return ICloudUploadResult.Error.UploadFailed(it.toString()) }
        if (!accessorInvoked) {
            return ICloudUploadResult.Error.UploadFailed("File coordination accessor was not invoked")
        }
        uploadError?.let { return it }

        logger.d { "Upload to iCloud successfully" }
        return ICloudUploadResult.Success
    }

    override suspend fun performDownload(databaseName: String): ICloudDownloadResult {
        val iCloudUrl = getICloudFolderURL(databaseName)
            ?: return ICloudDownloadResult.Error.ICloudUrlNotAvailable

        val discoveredUrl = when (val result = fileDiscovery.discoverAndMaterialize(iCloudUrl)) {
            is ICloudFileDiscoveryResult.Available -> result.url
            ICloudFileDiscoveryResult.ConfirmedMissing -> return ICloudDownloadResult.Error.RemoteFileNotFound
            is ICloudFileDiscoveryResult.Failure -> return ICloudDownloadResult.Error.DownloadFailed(result.message)
        }

        val tempUrl = getTemporaryFileUrl(databaseName)
            ?: return ICloudDownloadResult.Error.TemporaryUrlNotAvailable

        var accessorInvoked = false
        var downloadError: ICloudDownloadResult.Error? = null
        val coordinationError = fileCoordinator.read(discoveredUrl) { coordinatedUrl ->
            accessorInvoked = true
            downloadError = download(coordinatedUrl, tempUrl)
        }
        coordinationError?.let { return mapDownloadError(it) }
        if (!accessorInvoked) {
            return ICloudDownloadResult.Error.DownloadFailed("File coordination accessor was not invoked")
        }
        downloadError?.let { return it }

        logger.d { "Download from iCloud successfully" }
        return ICloudDownloadResult.Success(destinationUrl = tempUrl)
    }

    private fun upload(databasePath: NSURL, coordinatedUrl: NSURL, databaseName: String): ICloudUploadResult.Error? {
        val coordinatedPath = coordinatedUrl.path
            ?: return ICloudUploadResult.Error.UploadFailed("Coordinated URL has no file path")
        val stagedUrl = coordinatedUrl.URLByDeletingLastPathComponent
            ?.URLByAppendingPathComponent(".$databaseName-${NSUUID.UUID().UUIDString}.upload")
            ?: return ICloudUploadResult.Error.ICloudUrlNotAvailable

        try {
            return memScoped {
                val errorPtr: ObjCObjectVar<NSError?> = alloc()
                errorPtr.value = null
                val fileManager = NSFileManager.defaultManager
                val parentUrl = coordinatedUrl.URLByDeletingLastPathComponent
                    ?: return@memScoped ICloudUploadResult.Error.ICloudUrlNotAvailable
                if (!fileManager.createDirectoryAtURL(parentUrl, true, null, errorPtr.ptr)) {
                    return@memScoped ICloudUploadResult.Error.UploadFailed(errorPtr.value.toString())
                }
                errorPtr.value = null
                if (!fileManager.copyItemAtURL(databasePath, stagedUrl, errorPtr.ptr)) {
                    return@memScoped ICloudUploadResult.Error.UploadFailed(errorPtr.value.toString())
                }

                val replaced = if (fileManager.fileExistsAtPath(coordinatedPath)) {
                    fileManager.replaceItemAtURL(coordinatedUrl, stagedUrl, null, 0u, null, errorPtr.ptr)
                } else {
                    fileManager.moveItemAtURL(stagedUrl, coordinatedUrl, errorPtr.ptr)
                }
                if (!replaced || errorPtr.value != null) {
                    ICloudUploadResult.Error.UploadFailed(errorPtr.value.toString())
                } else {
                    null
                }
            }
        } finally {
            NSFileManager.defaultManager.removeItemAtURL(stagedUrl, null)
        }
    }

    private fun download(coordinatedUrl: NSURL, tempUrl: NSURL): ICloudDownloadResult.Error? = memScoped {
        NSFileManager.defaultManager.removeItemAtURL(tempUrl, null)

        val errorPtr: ObjCObjectVar<NSError?> = alloc()
        errorPtr.value = null
        val copied = NSFileManager.defaultManager.copyItemAtURL(
            srcURL = coordinatedUrl,
            toURL = tempUrl,
            error = errorPtr.ptr,
        )
        if (copied && errorPtr.value == null) {
            return@memScoped null
        }

        logger.e { "Error downloading from iCloud: ${errorPtr.value}" }
        mapDownloadError(errorPtr.value)
    }

    private fun mapDownloadError(error: NSError?): ICloudDownloadResult.Error =
        if (error != null && error.domain == NSCocoaErrorDomain && error.code == NSFileReadNoSuchFileError) {
            ICloudDownloadResult.Error.FileNotFound
        } else {
            ICloudDownloadResult.Error.DownloadFailed(error.toString())
        }

    private suspend fun getICloudFolderURL(databaseName: String): NSURL? =
        getICloudBaseFolderURL()?.URLByAppendingPathComponent(databaseName)

    override suspend fun getICloudBaseFolderURL(
        timeoutSeconds: Int,
        initialPollIntervalMs: Long,
    ): NSURL? {
        localBaseFolderURL?.let { return it }

        val startTime = Clock.System.now()
        var currentPollInterval = initialPollIntervalMs

        while ((Clock.System.now() - startTime).inWholeSeconds < timeoutSeconds) {
            val url = NSFileManager.defaultManager
                .URLForUbiquityContainerIdentifier("iCloud.com.prof18.feedflow")
                ?.URLByAppendingPathComponent("Documents")

            if (url != null) {
                return url
            }

            delay(currentPollInterval)
            @Suppress("MagicNumber")
            currentPollInterval = (currentPollInterval * 1.5).toLong().coerceAtMost(maximumValue = 5000L)
        }

        return null
    }

    private fun getTemporaryFileUrl(databaseName: String): NSURL? {
        localTemporaryFolderURL?.let { return it.URLByAppendingPathComponent(databaseName) }

        val documentsDirectory: NSURL? = NSFileManager.defaultManager.URLsForDirectory(
            directory = NSDocumentDirectory,
            inDomains = NSUserDomainMask,
        ).firstOrNull() as? NSURL?
        return documentsDirectory?.URLByAppendingPathComponent(databaseName)
    }
}

sealed class ICloudUploadResult {
    data object Success : ICloudUploadResult()
    sealed class Error : ICloudUploadResult() {
        data object ICloudUrlNotAvailable : Error()
        data class UploadFailed(val errorMessage: String) : Error()
    }
}

sealed class ICloudDownloadResult {
    data class Success(val destinationUrl: NSURL) : ICloudDownloadResult()
    sealed class Error : ICloudDownloadResult() {
        data object ICloudUrlNotAvailable : Error()
        data object TemporaryUrlNotAvailable : Error()

        /** Metadata discovery completed without the requested remote backup. */
        data object RemoteFileNotFound : Error()

        /** A previously discovered local file disappeared during coordinated copying. */
        data object FileNotFound : Error()
        data object CopyOperationFailed : Error()
        data object FileAlreadyExists : Error()
        data class DownloadFailed(val errorMessage: String) : Error()
    }
}
