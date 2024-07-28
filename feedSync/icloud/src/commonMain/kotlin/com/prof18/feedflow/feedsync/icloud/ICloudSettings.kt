package com.prof18.feedflow.feedsync.icloud

import com.russhwolf.settings.Settings

class ICloudSettings(
    private val settings: Settings,
) {
    fun setUseICloud(value: Boolean) =
        settings.putBoolean(ICloudSettingsFields.USE_ICLOUD.name, value)

    fun getUseICloud(): Boolean =
        settings.getBoolean(
            key = ICloudSettingsFields.USE_ICLOUD.name,
            defaultValue = false,
        )

    fun setLastUploadTimestamp(timestamp: Long) =
        settings.putLong(ICloudSettingsFields.ICLOUD_LAST_UPLOAD_TIMESTAMP.name, timestamp)

    fun getLastUploadTimestamp(): Long? =
        settings.getLongOrNull(ICloudSettingsFields.ICLOUD_LAST_UPLOAD_TIMESTAMP.name)

    fun setLastDownloadTimestamp(timestamp: Long) =
        settings.putLong(ICloudSettingsFields.ICLOUD_LAST_DOWNLOAD_TIMESTAMP.name, timestamp)

    fun getLastDownloadTimestamp(): Long? =
        settings.getLongOrNull(ICloudSettingsFields.ICLOUD_LAST_DOWNLOAD_TIMESTAMP.name)

    private enum class ICloudSettingsFields {
        USE_ICLOUD,
        ICLOUD_LAST_UPLOAD_TIMESTAMP,
        ICLOUD_LAST_DOWNLOAD_TIMESTAMP,
    }
}
