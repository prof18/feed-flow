package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsRepository(
    private val settings: Settings,
) {
    private var isReaderModeEnabled: Boolean? = null

    private val isSyncUploadRequiredMutableFlow = MutableStateFlow(getIsSyncUploadRequired())
    val isSyncUploadRequired: StateFlow<Boolean> = isSyncUploadRequiredMutableFlow.asStateFlow()

    fun getFavouriteBrowserId(): String? =
        settings.getStringOrNull(SettingsFields.FAVOURITE_BROWSER_ID.name)

    fun saveFavouriteBrowserId(browserId: String) =
        settings.set(SettingsFields.FAVOURITE_BROWSER_ID.name, browserId)

    internal fun getMarkFeedAsReadWhenScrolling(): Boolean =
        settings.getBoolean(SettingsFields.MARK_FEED_AS_READ_WHEN_SCROLLING.name, true)

    internal fun setMarkFeedAsReadWhenScrolling(value: Boolean) =
        settings.set(SettingsFields.MARK_FEED_AS_READ_WHEN_SCROLLING.name, value)

    internal fun getShowReadArticlesTimeline(): Boolean =
        settings.getBoolean(SettingsFields.SHOW_READ_ARTICLES_TIMELINE.name, false)

    internal fun setShowReadArticlesTimeline(value: Boolean) =
        settings.set(SettingsFields.SHOW_READ_ARTICLES_TIMELINE.name, value)

    fun isUseReaderModeEnabled(): Boolean {
        if (isReaderModeEnabled != null) {
            return requireNotNull(isReaderModeEnabled)
        } else {
            val value = settings.getBoolean(SettingsFields.USE_READER_MODE.name, false)
            isReaderModeEnabled = value
            return value
        }
    }

    internal fun setUseReaderMode(value: Boolean) {
        isReaderModeEnabled = value
        settings[SettingsFields.USE_READER_MODE.name] = value
    }

    internal fun getIsSyncUploadRequired(): Boolean =
        settings.getBoolean(SettingsFields.IS_SYNC_UPLOAD_REQUIRED.name, false)

    internal fun setIsSyncUploadRequired(value: Boolean) {
        isSyncUploadRequiredMutableFlow.update { value }
        settings[SettingsFields.IS_SYNC_UPLOAD_REQUIRED.name] = value
    }

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

    internal fun getAutoDeletePeriod(): AutoDeletePeriod =
        settings.getString(SettingsFields.AUTO_DELETE_PERIOD.name, AutoDeletePeriod.DISABLED.name)
            .let { AutoDeletePeriod.valueOf(it) }

    internal fun setAutoDeletePeriod(period: AutoDeletePeriod) =
        settings.set(SettingsFields.AUTO_DELETE_PERIOD.name, period.name)

    fun getHideImages(): Boolean =
        settings.getBoolean(SettingsFields.HIDE_IMAGES.name, false)

    fun setHideImages(value: Boolean) =
        settings.set(SettingsFields.HIDE_IMAGES.name, value)

    fun getCrashReportingEnabled(): Boolean =
        settings.getBoolean(SettingsFields.CRASH_REPORTING_ENABLED.name, true)

    fun setCrashReportingEnabled(value: Boolean) =
        settings.set(SettingsFields.CRASH_REPORTING_ENABLED.name, value)

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
    CRASH_REPORTING_ENABLED,
}
