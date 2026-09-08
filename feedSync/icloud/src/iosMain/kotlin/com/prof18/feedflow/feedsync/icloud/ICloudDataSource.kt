package com.prof18.feedflow.feedsync.icloud

import co.touchlab.kermit.Logger
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
) : ICloudDataSource {
    override suspend fun performUpload(databasePath: NSURL, databaseName: String): ICloudUploadResult {
        val iCloudUrl = getICloudFolderURL(databaseName)
            ?: return ICloudUploadResult.Error.ICloudUrlNotAvailable

        val stagedUrl = iCloudUrl.URLByDeletingLastPathComponent
            ?.URLByAppendingPathComponent(".$databaseName-${NSUUID.UUID().UUIDString}.upload")
            ?: return ICloudUploadResult.Error.ICloudUrlNotAvailable
        try {
            memScoped {
                val errorPtr: ObjCObjectVar<NSError?> = alloc()
                val fileManager = NSFileManager.defaultManager
                if (!fileManager.copyItemAtURL(databasePath, stagedUrl, errorPtr.ptr)) {
                    return ICloudUploadResult.Error.UploadFailed(errorPtr.value.toString())
                }
                val replaced = if (fileManager.fileExistsAtPath(requireNotNull(iCloudUrl.path))) {
                    fileManager.replaceItemAtURL(iCloudUrl, stagedUrl, null, 0u, null, errorPtr.ptr)
                } else {
                    fileManager.moveItemAtURL(stagedUrl, iCloudUrl, errorPtr.ptr)
                }
                if (!replaced || errorPtr.value != null) {
                    return ICloudUploadResult.Error.UploadFailed(errorPtr.value.toString())
                }
            }
        } finally {
            NSFileManager.defaultManager.removeItemAtURL(stagedUrl, null)
        }

        logger.d { "Upload to iCloud successfully" }
        return ICloudUploadResult.Success
    }

    override suspend fun performDownload(databaseName: String): ICloudDownloadResult {
        val iCloudUrl = getICloudFolderURL(databaseName)
            ?: return ICloudDownloadResult.Error.ICloudUrlNotAvailable

        val tempUrl = getTemporaryFileUrl(databaseName)
            ?: return ICloudDownloadResult.Error.TemporaryUrlNotAvailable

        NSFileManager.defaultManager.removeItemAtURL(
            tempUrl,
            null,
        )

        memScoped {
            val errorPtr: ObjCObjectVar<NSError?> = alloc()

            val copied = NSFileManager.defaultManager.copyItemAtURL(
                srcURL = iCloudUrl,
                toURL = tempUrl,
                error = errorPtr.ptr,
            )

            if (!copied || errorPtr.value != null) {
                logger.e { "Error downloading from iCloud: ${errorPtr.value}" }
                val error = errorPtr.value
                return if (error != null && error.domain == NSCocoaErrorDomain &&
                    error.code == NSFileReadNoSuchFileError
                ) {
                    ICloudDownloadResult.Error.FileNotFound
                } else {
                    ICloudDownloadResult.Error.DownloadFailed(error.toString())
                }
            }

            logger.d { "Download from iCloud successfully" }
            return ICloudDownloadResult.Success(destinationUrl = tempUrl)
        }
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
        data object FileNotFound : Error()
        data object CopyOperationFailed : Error()
        data object FileAlreadyExists : Error()
        data class DownloadFailed(val errorMessage: String) : Error()
    }
}
