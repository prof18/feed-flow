package com.prof18.feedflow.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

internal class SettingsHelper(
    private val settings: Settings,
) {
    fun getFavouriteBrowserId(): String? =
        settings.getStringOrNull(SettingsFields.FAVOURITE_BROWSER_ID.name)

    fun saveFavouriteBrowserId(browserId: String) =
        settings.set(SettingsFields.FAVOURITE_BROWSER_ID.name, browserId)

    fun isFeedSourceImageMigrationDone(): Boolean =
        settings.getBoolean(SettingsFields.FEED_SOURCE_IMAGE_MIGRATION.name, false)

    fun setFeedSourceImageMigrationDone() =
        settings.set(SettingsFields.FEED_SOURCE_IMAGE_MIGRATION.name, true)
}

internal enum class SettingsFields {
    FAVOURITE_BROWSER_ID,
    FEED_SOURCE_IMAGE_MIGRATION,
}
