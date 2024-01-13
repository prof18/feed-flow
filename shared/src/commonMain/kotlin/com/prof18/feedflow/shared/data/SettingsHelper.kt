package com.prof18.feedflow.shared.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

internal class SettingsHelper(
    private val settings: Settings,
) {
    fun getFavouriteBrowserId(): String? =
        settings.getStringOrNull(com.prof18.feedflow.shared.data.SettingsFields.FAVOURITE_BROWSER_ID.name)

    fun saveFavouriteBrowserId(browserId: String) =
        settings.set(com.prof18.feedflow.shared.data.SettingsFields.FAVOURITE_BROWSER_ID.name, browserId)

    fun isFeedSourceImageMigrationDone(): Boolean =
        settings.getBoolean(com.prof18.feedflow.shared.data.SettingsFields.FEED_SOURCE_IMAGE_MIGRATION.name, false)

    fun setFeedSourceImageMigrationDone() =
        settings.set(com.prof18.feedflow.shared.data.SettingsFields.FEED_SOURCE_IMAGE_MIGRATION.name, true)

    fun getMarkFeedAsReadWhenScrolling(): Boolean =
        settings.getBoolean(com.prof18.feedflow.shared.data.SettingsFields.MARK_FEED_AS_READ_WHEN_SCROLLING.name, true)

    fun setMarkFeedAsReadWhenScrolling(value: Boolean) =
        settings.set(com.prof18.feedflow.shared.data.SettingsFields.MARK_FEED_AS_READ_WHEN_SCROLLING.name, value)
}

internal enum class SettingsFields {
    FAVOURITE_BROWSER_ID,
    FEED_SOURCE_IMAGE_MIGRATION,
    MARK_FEED_AS_READ_WHEN_SCROLLING,
}
