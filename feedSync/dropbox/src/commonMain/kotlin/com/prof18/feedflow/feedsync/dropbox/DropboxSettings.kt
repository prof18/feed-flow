package com.prof18.feedflow.feedsync.dropbox

import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings

class DropboxSettings(
    private val settings: Settings,
    private val logger: Logger,
) {
    fun setDropboxData(data: String) = withErrorHandling {
        settings.putString(DropboxSettingsFields.DROPBOX_DATA.name, data)
    }

    fun clearDropboxData() = withErrorHandling {
        settings.remove(DropboxSettingsFields.DROPBOX_DATA.name)
    }

    fun getDropboxData(): String? = try {
        settings.getStringOrNull(DropboxSettingsFields.DROPBOX_DATA.name)
    } catch (_: Throwable) {
        null
    }

    fun setLastUploadTimestamp(timestamp: Long) = withErrorHandling {
        settings.putLong(
            DropboxSettingsFields.LAST_UPLOAD_TIMESTAMP.name,
            timestamp,
        )
    }

    fun getLastUploadTimestamp(): Long? = try {
        settings.getLongOrNull(DropboxSettingsFields.LAST_UPLOAD_TIMESTAMP.name)
    } catch (_: Throwable) {
        null
    }

    fun setLastDownloadTimestamp(timestamp: Long) = withErrorHandling {
        settings.putLong(DropboxSettingsFields.LAST_DOWNLOAD_TIMESTAMP.name, timestamp)
    }

    fun getLastDownloadTimestamp(): Long? = try {
        settings.getLongOrNull(DropboxSettingsFields.LAST_DOWNLOAD_TIMESTAMP.name)
    } catch (_: Throwable) {
        null
    }

    private enum class DropboxSettingsFields {
        DROPBOX_DATA, LAST_UPLOAD_TIMESTAMP, LAST_DOWNLOAD_TIMESTAMP,
    }

    private fun withErrorHandling(
        body: () -> Unit,
    ) {
        try {
            body()
        } catch (e: Exception) {
            logger.e(e) { "Error while accessing dropbox settings" }
        }
    }
}
