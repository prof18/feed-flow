package com.prof18.feedflow.feedsync.googledrive

import com.russhwolf.settings.Settings

class GoogleDriveSettings(
    private val settings: Settings,
) {
    fun setGoogleDriveData(data: String) =
        settings.putString(GoogleDriveSettingsFields.GOOGLE_DRIVE_DATA.name, data)

    fun clearGoogleDriveData() =
        settings.remove(GoogleDriveSettingsFields.GOOGLE_DRIVE_DATA.name)

    fun getGoogleDriveData(): String? = settings.getStringOrNull(GoogleDriveSettingsFields.GOOGLE_DRIVE_DATA.name)

    fun setLastUploadTimestamp(timestamp: Long) =
        settings.putLong(
            GoogleDriveSettingsFields.LAST_UPLOAD_TIMESTAMP.name,
            timestamp,
        )

    fun getLastUploadTimestamp(): Long? = settings.getLongOrNull(GoogleDriveSettingsFields.LAST_UPLOAD_TIMESTAMP.name)

    fun setLastDownloadTimestamp(timestamp: Long) =
        settings.putLong(GoogleDriveSettingsFields.LAST_DOWNLOAD_TIMESTAMP.name, timestamp)

    fun getLastDownloadTimestamp(): Long? = settings.getLongOrNull(GoogleDriveSettingsFields.LAST_DOWNLOAD_TIMESTAMP.name)

    private enum class GoogleDriveSettingsFields {
        GOOGLE_DRIVE_DATA, LAST_UPLOAD_TIMESTAMP, LAST_DOWNLOAD_TIMESTAMP,
    }
}
