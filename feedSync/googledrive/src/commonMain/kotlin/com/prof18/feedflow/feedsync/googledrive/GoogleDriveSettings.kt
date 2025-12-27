package com.prof18.feedflow.feedsync.googledrive

import com.russhwolf.settings.Settings

class GoogleDriveSettings(
    private val settings: Settings,
) {
    fun setGoogleDriveLinked(linked: Boolean) =
        settings.putBoolean(GoogleDriveSettingsFields.GOOGLE_DRIVE_LINKED.name, linked)

    fun isGoogleDriveLinked(): Boolean =
        settings.getBoolean(GoogleDriveSettingsFields.GOOGLE_DRIVE_LINKED.name, false)

    fun setLastUploadTimestamp(timestamp: Long) =
        settings.putLong(
            GoogleDriveSettingsFields.LAST_UPLOAD_TIMESTAMP.name,
            timestamp,
        )

    fun getLastUploadTimestamp(): Long? = settings.getLongOrNull(GoogleDriveSettingsFields.LAST_UPLOAD_TIMESTAMP.name)

    fun setLastDownloadTimestamp(timestamp: Long) =
        settings.putLong(GoogleDriveSettingsFields.LAST_DOWNLOAD_TIMESTAMP.name, timestamp)

    fun getLastDownloadTimestamp(): Long? = settings.getLongOrNull(
        GoogleDriveSettingsFields.LAST_DOWNLOAD_TIMESTAMP.name,
    )

    fun setBackupFileId(fileId: String) =
        settings.putString(GoogleDriveSettingsFields.BACKUP_FILE_ID.name, fileId)

    fun getBackupFileId(): String? =
        settings.getStringOrNull(GoogleDriveSettingsFields.BACKUP_FILE_ID.name)

    fun clearAll() {
        GoogleDriveSettingsFields.entries.forEach {
            settings.remove(it.name)
        }
    }

    private enum class GoogleDriveSettingsFields {
        LAST_UPLOAD_TIMESTAMP,
        LAST_DOWNLOAD_TIMESTAMP,
        BACKUP_FILE_ID,
        GOOGLE_DRIVE_LINKED,
    }
}
