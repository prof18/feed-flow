package com.prof18.feedflow.feedsync.dropbox

import com.russhwolf.settings.Settings

class DropboxSettings(
    private val settings: Settings,
) {
    fun setDropboxData(data: String) =
        settings.putString(DropboxSettingsFields.DROPBOX_DATA.name, data)

    fun clearDropboxData() =
        settings.remove(DropboxSettingsFields.DROPBOX_DATA.name)

    fun getDropboxData(): String? =
        settings.getStringOrNull(DropboxSettingsFields.DROPBOX_DATA.name)

    fun setLastUploadTimestamp(timestamp: Long) =
        settings.putLong(DropboxSettingsFields.LAST_UPLOAD_TIMESTAMP.name, timestamp)

    fun getLastUploadTimestamp(): Long? =
        settings.getLongOrNull(DropboxSettingsFields.LAST_UPLOAD_TIMESTAMP.name)

    fun setLastDownloadTimestamp(timestamp: Long) =
        settings.putLong(DropboxSettingsFields.LAST_DOWNLOAD_TIMESTAMP.name, timestamp)

    fun getLastDownloadTimestamp(): Long? =
        settings.getLongOrNull(DropboxSettingsFields.LAST_DOWNLOAD_TIMESTAMP.name)

    private enum class DropboxSettingsFields {
        DROPBOX_DATA,
        LAST_UPLOAD_TIMESTAMP,
        LAST_DOWNLOAD_TIMESTAMP,
    }
}
