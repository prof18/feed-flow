package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.core.model.CloudBackupNotFoundException

internal enum class CloudProvider {
    DROPBOX,
    GOOGLE_DRIVE,
    ICLOUD,
}

internal enum class CloudStoreOperation {
    UPLOAD,
    PROPAGATE,
}

internal data class CloudStoreEvent(
    val operation: CloudStoreOperation,
    val provider: CloudProvider,
    val account: String,
    val name: String,
    val deviceId: String,
    val fileId: String,
)

internal data class CloudStoreFile(
    val provider: CloudProvider,
    val account: String,
    val name: String,
    val fileId: String,
    val bytes: ByteArray,
)

/** A deterministic, in-memory stand-in for the byte-file portions of cloud sync providers. */
internal class CloudStore {
    var beforeDownload: () -> Unit = {}
    var beforeUploadRead: suspend () -> Unit = {}
    var uploadFailure: Exception? = null
    var downloadFailure: Exception? = null
    var downloadedBytes: ByteArray? = null

    private data class FileKey(
        val provider: CloudProvider,
        val account: String,
        val name: String,
    )

    private data class StoredFile(
        val fileId: String,
        val bytes: ByteArray,
    )

    private val visibleFiles = mutableMapOf<FileKey, StoredFile>()
    private val iCloudStagedFiles = mutableMapOf<String, MutableMap<FileKey, StoredFile>>()
    private val fileIds = mutableMapOf<FileKey, String>()
    private val eventLog = mutableListOf<CloudStoreEvent>()
    private var nextDriveId = 1

    /** Uploads bytes and returns the provider's stable opaque file identifier. */
    fun upload(
        provider: CloudProvider,
        account: String,
        name: String,
        bytes: ByteArray,
        deviceId: String,
    ): String {
        uploadFailure?.let { throw it }
        val key = FileKey(provider, account, name)
        val fileId = fileIds.getOrPut(key) { createFileId(provider, key) }
        val storedFile = StoredFile(fileId, bytes.copyOf())

        if (provider == CloudProvider.ICLOUD) {
            iCloudStagedFiles.getOrPut(deviceId) { mutableMapOf() }[key] = storedFile
        } else {
            visibleFiles[key] = storedFile
        }

        eventLog += CloudStoreEvent(
            operation = CloudStoreOperation.UPLOAD,
            provider = provider,
            account = account,
            name = name,
            deviceId = deviceId,
            fileId = fileId,
        )
        return fileId
    }

    /** Reads only visible cloud state. iCloud local saves become visible after [propagate]. */
    fun download(provider: CloudProvider, account: String, name: String): ByteArray {
        beforeDownload()
        downloadFailure?.let { throw it }
        downloadedBytes?.let { return it.copyOf() }
        val key = FileKey(provider, account, name)
        return (visibleFiles[key] ?: throw CloudBackupNotFoundException()).bytes.copyOf()
    }

    /** Makes all files staged by [deviceId] visible in iCloud and clears that local stage. */
    fun propagate(deviceId: String) {
        val stagedFiles = iCloudStagedFiles.remove(deviceId) ?: return
        stagedFiles.forEach { (key, storedFile) ->
            visibleFiles[key] = StoredFile(storedFile.fileId, storedFile.bytes.copyOf())
            eventLog += CloudStoreEvent(
                operation = CloudStoreOperation.PROPAGATE,
                provider = key.provider,
                account = key.account,
                name = key.name,
                deviceId = deviceId,
                fileId = storedFile.fileId,
            )
        }
    }

    fun fileCount(provider: CloudProvider): Int =
        visibleFiles.count { (key, _) -> key.provider == provider }

    fun fileCount(provider: CloudProvider, account: String): Int =
        visibleFiles.count { (key, _) -> key.provider == provider && key.account == account }

    /** Returns detached copies so callers cannot mutate store state through the snapshot. */
    fun snapshot(): List<CloudStoreFile> = visibleFiles.map { (key, storedFile) ->
        CloudStoreFile(
            provider = key.provider,
            account = key.account,
            name = key.name,
            fileId = storedFile.fileId,
            bytes = storedFile.bytes.copyOf(),
        )
    }

    val events: List<CloudStoreEvent>
        get() = eventLog.toList()

    private fun createFileId(provider: CloudProvider, key: FileKey): String = when (provider) {
        CloudProvider.DROPBOX -> key.name
        CloudProvider.GOOGLE_DRIVE -> "drive-file-${nextDriveId++}"
        CloudProvider.ICLOUD -> "icloud-file:${key.account}:${key.name}"
    }
}
