package com.prof18.feedflow.shared.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

internal class SettingsHelper(
    private val settings: Settings,
) {
    fun getFavouriteBrowserId(): String? =
        settings.getStringOrNull(SettingsFields.FAVOURITE_BROWSER_ID.name)

    fun saveFavouriteBrowserId(browserId: String) =
        settings.set(SettingsFields.FAVOURITE_BROWSER_ID.name, browserId)

    fun getMarkFeedAsReadWhenScrolling(): Boolean =
        settings.getBoolean(SettingsFields.MARK_FEED_AS_READ_WHEN_SCROLLING.name, true)

    fun setMarkFeedAsReadWhenScrolling(value: Boolean) =
        settings.set(SettingsFields.MARK_FEED_AS_READ_WHEN_SCROLLING.name, value)

    fun getShowReadArticlesTimeline(): Boolean =
        settings.getBoolean(SettingsFields.SHOW_READ_ARTICLES_TIMELINE.name, false)

    fun setShowReadArticlesTimeline(value: Boolean) =
        settings.set(SettingsFields.SHOW_READ_ARTICLES_TIMELINE.name, value)

    fun getUseReaderMode(): Boolean =
        settings.getBoolean(SettingsFields.USE_READER_MODE.name, false)

    fun setUseReaderMode(value: Boolean) =
        settings.set(SettingsFields.USE_READER_MODE.name, value)

    fun getIsSyncUploadRequired(): Boolean =
        settings.getBoolean(SettingsFields.IS_SYNC_UPLOAD_REQUIRED.name, false)

    fun setIsSyncUploadRequired(value: Boolean) =
        settings.set(SettingsFields.IS_SYNC_UPLOAD_REQUIRED.name, value)

    fun getRemoveTitleFromDescription(): Boolean =
        settings.getBoolean(SettingsFields.REMOVE_TITLE_FROM_DESCRIPTION.name, false)

    fun setRemoveTitleFromDescription(value: Boolean) =
        settings.set(SettingsFields.REMOVE_TITLE_FROM_DESCRIPTION.name, value)

    fun getReaderModeFontSize(): Int =
        settings.getInt(SettingsFields.READER_MODE_FONT_SIZE.name, DEFAULT_READER_MODE_FONT_SIZE)

    fun setReaderModeFontSize(value: Int) =
        settings.set(SettingsFields.READER_MODE_FONT_SIZE.name, value)

    private companion object {
        const val DEFAULT_READER_MODE_FONT_SIZE = 16
    }
}

internal enum class SettingsFields {
    FAVOURITE_BROWSER_ID,
    MARK_FEED_AS_READ_WHEN_SCROLLING,
    SHOW_READ_ARTICLES_TIMELINE,
    USE_READER_MODE,
    IS_SYNC_UPLOAD_REQUIRED,
    REMOVE_TITLE_FROM_DESCRIPTION,
    READER_MODE_FONT_SIZE,
}
