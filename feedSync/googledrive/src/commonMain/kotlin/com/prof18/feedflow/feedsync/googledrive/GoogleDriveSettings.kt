package com.prof18.feedflow.feedsync.googledrive

import com.russhwolf.settings.Settings
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GoogleDriveSettings(
    private val settings: Settings,
) {
    // TODO: validate which data are actually necessary
    private val json = Json { ignoreUnknownKeys = true }

    fun setGoogleDriveData(data: String) =
        settings.putString(GoogleDriveSettingsFields.GOOGLE_DRIVE_DATA.name, data)

    fun clearGoogleDriveData() =
        settings.remove(GoogleDriveSettingsFields.GOOGLE_DRIVE_DATA.name)

    fun getGoogleDriveData(): String? = settings.getStringOrNull(GoogleDriveSettingsFields.GOOGLE_DRIVE_DATA.name)

    fun setGoogleDriveCredentials(credentials: GoogleDriveCredentials) {
        val jsonString = json.encodeToString(credentials)
        setGoogleDriveData(jsonString)
    }

    fun getGoogleDriveCredentials(): GoogleDriveCredentials? {
        val jsonString = getGoogleDriveData() ?: return null
        return try {
            json.decodeFromString<GoogleDriveCredentials>(jsonString)
        } catch (_: SerializationException) {
            null
        }
    }

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
        GOOGLE_DRIVE_DATA,
    }
}
