package com.prof18.feedflow.feedsync.nextcloud

import com.russhwolf.settings.Settings

class NextcloudSettings(
    private val settings: Settings,
) {
    fun setServerUrl(url: String) =
        settings.putString(NextcloudSettingsFields.SERVER_URL.name, url)

    fun getServerUrl(): String? =
        settings.getStringOrNull(NextcloudSettingsFields.SERVER_URL.name)

    fun setUsername(username: String) =
        settings.putString(NextcloudSettingsFields.USERNAME.name, username)

    fun getUsername(): String? =
        settings.getStringOrNull(NextcloudSettingsFields.USERNAME.name)

    fun setPassword(password: String) =
        settings.putString(NextcloudSettingsFields.PASSWORD.name, password)

    fun getPassword(): String? =
        settings.getStringOrNull(NextcloudSettingsFields.PASSWORD.name)

    fun setLastUploadTimestamp(timestamp: Long) =
        settings.putLong(NextcloudSettingsFields.LAST_UPLOAD_TIMESTAMP.name, timestamp)

    fun getLastUploadTimestamp(): Long? =
        settings.getLongOrNull(NextcloudSettingsFields.LAST_UPLOAD_TIMESTAMP.name)

    fun setLastDownloadTimestamp(timestamp: Long) =
        settings.putLong(NextcloudSettingsFields.LAST_DOWNLOAD_TIMESTAMP.name, timestamp)

    fun getLastDownloadTimestamp(): Long? =
        settings.getLongOrNull(NextcloudSettingsFields.LAST_DOWNLOAD_TIMESTAMP.name)

    fun clearCredentials() {
        settings.remove(NextcloudSettingsFields.SERVER_URL.name)
        settings.remove(NextcloudSettingsFields.USERNAME.name)
        settings.remove(NextcloudSettingsFields.PASSWORD.name)
    }

    fun hasCredentials(): Boolean {
        return getServerUrl() != null &&
               getUsername() != null &&
               getPassword() != null
    }

    private enum class NextcloudSettingsFields {
        SERVER_URL,
        USERNAME,
        PASSWORD,
        LAST_UPLOAD_TIMESTAMP,
        LAST_DOWNLOAD_TIMESTAMP,
    }
}
