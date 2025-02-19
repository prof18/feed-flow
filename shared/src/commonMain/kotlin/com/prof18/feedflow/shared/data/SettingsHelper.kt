package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.model.AutoDeletePeriod
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

    fun getHideDescription(): Boolean =
        settings.getBoolean(SettingsFields.HIDE_DESCRIPTION.name, false)

    fun setHideDescription(value: Boolean) =
        settings.set(SettingsFields.HIDE_DESCRIPTION.name, value)

    fun getReaderModeFontSize(): Int =
        settings.getInt(SettingsFields.READER_MODE_FONT_SIZE.name, DEFAULT_READER_MODE_FONT_SIZE)

    fun setReaderModeFontSize(value: Int) =
        settings.set(SettingsFields.READER_MODE_FONT_SIZE.name, value)

    fun getFeedListFontScaleFactor(): Int =
        settings.getInt(SettingsFields.FEED_LIST_FONT_SCALE_FACTOR.name, DEFAULT_FEED_LIST_FONT_SCALE_FACTOR)

    fun setFeedListFontScaleFactor(value: Int) =
        settings.set(SettingsFields.FEED_LIST_FONT_SCALE_FACTOR.name, value)

    fun getAutoDeletePeriod(): AutoDeletePeriod =
        settings.getString(SettingsFields.AUTO_DELETE_PERIOD.name, AutoDeletePeriod.DISABLED.name)
            .let { AutoDeletePeriod.valueOf(it) }

    fun setAutoDeletePeriod(period: AutoDeletePeriod) =
        settings.set(SettingsFields.AUTO_DELETE_PERIOD.name, period.name)

    fun getHideImages(): Boolean =
        settings.getBoolean(SettingsFields.HIDE_IMAGES.name, false)

    fun setHideImages(value: Boolean) =
        settings.set(SettingsFields.HIDE_IMAGES.name, value)

    private companion object {
        const val DEFAULT_READER_MODE_FONT_SIZE = 16
        const val DEFAULT_FEED_LIST_FONT_SCALE_FACTOR = 0
    }
}

internal enum class SettingsFields {
    FAVOURITE_BROWSER_ID,
    MARK_FEED_AS_READ_WHEN_SCROLLING,
    SHOW_READ_ARTICLES_TIMELINE,
    USE_READER_MODE,
    IS_SYNC_UPLOAD_REQUIRED,
    REMOVE_TITLE_FROM_DESCRIPTION,
    HIDE_DESCRIPTION,
    READER_MODE_FONT_SIZE,
    FEED_LIST_FONT_SCALE_FACTOR,
    AUTO_DELETE_PERIOD,
    HIDE_IMAGES,
}
