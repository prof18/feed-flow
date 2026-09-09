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

internal data class DropboxCloudFile(
    val fileId: String,
    val bytes: ByteArray,
    val revision: String,
)

/** A deterministic, in-memory stand-in for the byte-file portions of cloud sync providers. */
internal class CloudStore {
    var beforeDownload: () -> Unit = {}
    var beforeUploadRead: suspend () -> Unit = {}
    var uploadFailure: Exception? = null

    /** Used to model an acknowledged Dropbox write whose response is lost. */
    var afterUploadFailure: Exception? = null
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
        val revision: String? = null,
    )

    private val visibleFiles = mutableMapOf<FileKey, StoredFile>()
    private val iCloudStagedFiles = mutableMapOf<String, MutableMap<FileKey, StoredFile>>()
    private val fileIds = mutableMapOf<FileKey, String>()
    private val eventLog = mutableListOf<CloudStoreEvent>()
    private var nextDriveId = 1
    private var nextDropboxRevision = 1L

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
        val revision = if (provider == CloudProvider.DROPBOX) (nextDropboxRevision++).toString() else null
        val storedFile = StoredFile(fileId, bytes.copyOf(), revision)

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

    /**
     * Dropbox's create-only/conditional overwrite primitive. The check and write are
     * deliberately synchronous so a test cannot introduce a race between them.
     */
    fun uploadDropbox(
        account: String,
        name: String,
        bytes: ByteArray,
        deviceId: String,
        expectedRevision: String?,
    ): DropboxCloudFile {
        uploadFailure?.let { throw it }
        val key = FileKey(CloudProvider.DROPBOX, account, name)
        val existing = visibleFiles[key]
        if ((expectedRevision == null && existing != null) ||
            (expectedRevision != null && existing?.revision != expectedRevision)
        ) {
            throw com.prof18.feedflow.feedsync.dropbox.DropboxUploadConflictException()
        }
        val fileId = fileIds.getOrPut(key) { createFileId(CloudProvider.DROPBOX, key) }
        val storedFile = StoredFile(fileId, bytes.copyOf(), nextDropboxRevision++.toString())
        visibleFiles[key] = storedFile
        eventLog += CloudStoreEvent(
            operation = CloudStoreOperation.UPLOAD,
            provider = CloudProvider.DROPBOX,
            account = account,
            name = name,
            deviceId = deviceId,
            fileId = fileId,
        )
        failAfterAcceptedUpload()
        return DropboxCloudFile(fileId, storedFile.bytes.copyOf(), requireNotNull(storedFile.revision))
    }

    private fun failAfterAcceptedUpload() {
        afterUploadFailure?.let {
            afterUploadFailure = null
            throw it
        }
    }

    /** Reads only visible cloud state. iCloud local saves become visible after [propagate]. */
    fun download(provider: CloudProvider, account: String, name: String): ByteArray {
        beforeDownload()
        downloadFailure?.let { throw it }
        downloadedBytes?.let { return it.copyOf() }
        val key = FileKey(provider, account, name)
        return (visibleFiles[key] ?: throw CloudBackupNotFoundException()).bytes.copyOf()
    }

    /** Captures Dropbox bytes and revision from one visible-file read. */
    fun downloadDropbox(account: String, name: String): DropboxCloudFile {
        beforeDownload()
        downloadFailure?.let { throw it }
        downloadedBytes?.let { return DropboxCloudFile(name, it.copyOf(), "fixture-override") }
        val key = FileKey(CloudProvider.DROPBOX, account, name)
        val file = visibleFiles[key] ?: throw CloudBackupNotFoundException()
        return DropboxCloudFile(file.fileId, file.bytes.copyOf(), requireNotNull(file.revision))
    }

    /** Simulates a remote Dropbox deletion after a client has read a revision. */
    fun deleteDropbox(account: String, name: String) {
        visibleFiles.remove(FileKey(CloudProvider.DROPBOX, account, name))
    }

    /** Makes all files staged by [deviceId] visible in iCloud and clears that local stage. */
    fun propagate(deviceId: String) {
        val stagedFiles = iCloudStagedFiles.remove(deviceId) ?: return
        stagedFiles.forEach { (key, storedFile) ->
            visibleFiles[key] = StoredFile(storedFile.fileId, storedFile.bytes.copyOf(), storedFile.revision)
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
