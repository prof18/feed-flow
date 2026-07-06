package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.BackgroundSyncRestrictions
import com.prof18.feedflow.core.model.NotificationMode
import com.prof18.feedflow.core.model.ReaderModeDefaults
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.domain.model.SyncPeriod
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
    private var saveItemContentOnOpenEnabled: Boolean? = null
    private var prefetchArticleContentEnabled: Boolean? = null

    private val isSyncUploadRequiredMutableFlow = MutableStateFlow(getIsSyncUploadRequired())
    val isSyncUploadRequired: StateFlow<Boolean> = isSyncUploadRequiredMutableFlow.asStateFlow()

    private val syncPeriodMutableFlow = MutableStateFlow(getSyncPeriod())
    val syncPeriodFlow: StateFlow<SyncPeriod> = syncPeriodMutableFlow.asStateFlow()

    private val backgroundSyncRestrictionsMutableFlow =
        MutableStateFlow(getBackgroundSyncRestrictions())
    val backgroundSyncRestrictionsFlow: StateFlow<BackgroundSyncRestrictions> =
        backgroundSyncRestrictionsMutableFlow.asStateFlow()

    private val themeModeMutableFlow = MutableStateFlow(getThemeMode())
    val themeModeFlow: StateFlow<ThemeMode> = themeModeMutableFlow.asStateFlow()

    private val reduceMotionEnabledMutableFlow = MutableStateFlow(getReduceMotionEnabled())
    val reduceMotionEnabledFlow: StateFlow<Boolean> = reduceMotionEnabledMutableFlow.asStateFlow()

    private val showReadArticlesTimelineMutableFlow = MutableStateFlow(getShowReadArticlesTimeline())
    internal val showReadArticlesTimelineFlow: StateFlow<Boolean> = showReadArticlesTimelineMutableFlow.asStateFlow()

    private val markFeedAsReadWhenScrollingMutableFlow = MutableStateFlow(getMarkFeedAsReadWhenScrolling())
    internal val markFeedAsReadWhenScrollingFlow: StateFlow<Boolean> =
        markFeedAsReadWhenScrollingMutableFlow.asStateFlow()

    private val hideReadItemsMutableFlow = MutableStateFlow(getHideReadItems())
    internal val hideReadItemsFlow: StateFlow<Boolean> = hideReadItemsMutableFlow.asStateFlow()

    private val uncategorizedPositionMutableFlow = MutableStateFlow(getUncategorizedPosition())
    internal val uncategorizedPositionFlow: StateFlow<Int> = uncategorizedPositionMutableFlow.asStateFlow()

    fun getFavouriteBrowserId(): String? =
        settings.getStringOrNull(SettingsFields.FAVOURITE_BROWSER_ID.name)

    fun saveFavouriteBrowserId(browserId: String) =
        settings.set(SettingsFields.FAVOURITE_BROWSER_ID.name, browserId)

    fun clearFavouriteBrowserId() =
        settings.remove(SettingsFields.FAVOURITE_BROWSER_ID.name)

    internal fun getMarkFeedAsReadWhenScrolling(): Boolean =
        settings.getBoolean(SettingsFields.MARK_FEED_AS_READ_WHEN_SCROLLING.name, true)

    internal fun setMarkFeedAsReadWhenScrolling(value: Boolean) {
        settings.set(SettingsFields.MARK_FEED_AS_READ_WHEN_SCROLLING.name, value)
        markFeedAsReadWhenScrollingMutableFlow.update { value }
    }

    internal fun getShowReadArticlesTimeline(): Boolean =
        settings.getBoolean(SettingsFields.SHOW_READ_ARTICLES_TIMELINE.name, false)

    internal fun setShowReadArticlesTimeline(value: Boolean) {
        settings.set(SettingsFields.SHOW_READ_ARTICLES_TIMELINE.name, value)
        showReadArticlesTimelineMutableFlow.update { value }
    }

    internal fun getHideReadItems(): Boolean =
        settings.getBoolean(SettingsFields.HIDE_READ_ITEMS.name, false)

    internal fun setHideReadItems(value: Boolean) {
        settings.set(SettingsFields.HIDE_READ_ITEMS.name, value)
        hideReadItemsMutableFlow.update { value }
    }

    internal fun getUncategorizedPosition(): Int =
        settings.getInt(SettingsFields.UNCATEGORIZED_CATEGORY_POSITION.name, 0)

    internal fun setUncategorizedPosition(value: Int) {
        settings.set(SettingsFields.UNCATEGORIZED_CATEGORY_POSITION.name, value)
        uncategorizedPositionMutableFlow.update { value }
    }

    fun isUseReaderModeEnabled(): Boolean {
        if (isReaderModeEnabled != null) {
            return requireNotNull(isReaderModeEnabled)
        } else {
            val value = settings.getBoolean(SettingsFields.USE_READER_MODE.name, true)
            isReaderModeEnabled = value
            return value
        }
    }

    internal fun setUseReaderMode(value: Boolean) {
        isReaderModeEnabled = value
        settings[SettingsFields.USE_READER_MODE.name] = value
    }

    fun isSaveItemContentOnOpenEnabled(): Boolean {
        if (saveItemContentOnOpenEnabled != null) {
            return requireNotNull(saveItemContentOnOpenEnabled)
        } else {
            val value = settings.getBoolean(SettingsFields.SAVE_ITEM_CONTENT_ON_OPEN.name, false)
            saveItemContentOnOpenEnabled = value
            return value
        }
    }

    fun setSaveItemContentOnOpen(value: Boolean) {
        saveItemContentOnOpenEnabled = value
        settings[SettingsFields.SAVE_ITEM_CONTENT_ON_OPEN.name] = value
    }

    fun isPrefetchArticleContentEnabled(): Boolean {
        if (prefetchArticleContentEnabled != null) {
            return requireNotNull(prefetchArticleContentEnabled)
        } else {
            val value = settings.getBoolean(SettingsFields.PREFETCH_ARTICLE_CONTENT.name, false)
            prefetchArticleContentEnabled = value
            return value
        }
    }

    fun setPrefetchArticleContent(value: Boolean) {
        prefetchArticleContentEnabled = value
        settings[SettingsFields.PREFETCH_ARTICLE_CONTENT.name] = value
    }

    internal fun getIsSyncUploadRequired(): Boolean =
        settings.getBoolean(SettingsFields.IS_SYNC_UPLOAD_REQUIRED.name, false)

    internal fun setIsSyncUploadRequired(value: Boolean) {
        isSyncUploadRequiredMutableFlow.update { value }
        settings[SettingsFields.IS_SYNC_UPLOAD_REQUIRED.name] = value
    }

    fun getReaderModeFontSize(): Int =
        settings.getInt(SettingsFields.READER_MODE_FONT_SIZE.name, ReaderModeDefaults.FONT_SIZE)

    fun setReaderModeFontSize(value: Int) =
        settings.set(SettingsFields.READER_MODE_FONT_SIZE.name, value)

    fun getReaderModeLineHeight(): Int =
        settings.getInt(SettingsFields.READER_MODE_LINE_HEIGHT.name, ReaderModeDefaults.LINE_HEIGHT)

    fun setReaderModeLineHeight(value: Int) =
        settings.set(SettingsFields.READER_MODE_LINE_HEIGHT.name, value)

    internal fun getAutoDeletePeriod(): AutoDeletePeriod =
        settings.getString(SettingsFields.AUTO_DELETE_PERIOD.name, AutoDeletePeriod.DISABLED.name)
            .let { AutoDeletePeriod.valueOf(it) }

    internal fun setAutoDeletePeriod(period: AutoDeletePeriod) =
        settings.set(SettingsFields.AUTO_DELETE_PERIOD.name, period.name)

    fun getCrashReportingEnabled(): Boolean =
        settings.getBoolean(SettingsFields.CRASH_REPORTING_ENABLED.name, true)

    fun setCrashReportingEnabled(value: Boolean) =
        settings.set(SettingsFields.CRASH_REPORTING_ENABLED.name, value)

    fun getSyncPeriod(): SyncPeriod =
        settings.getString(SettingsFields.SYNC_PERIOD.name, SyncPeriod.NEVER.name)
            .let { SyncPeriod.valueOf(it) }

    fun setSyncPeriod(period: SyncPeriod) {
        settings[SettingsFields.SYNC_PERIOD.name] = period.name
        syncPeriodMutableFlow.update { period }
    }

    fun getBackgroundSyncRestrictions(): BackgroundSyncRestrictions =
        BackgroundSyncRestrictions(
            syncOnlyOnWifi = settings.getBoolean(
                SettingsFields.BACKGROUND_SYNC_WIFI_ONLY.name,
                false,
            ),
            syncOnlyWhenCharging = settings.getBoolean(
                SettingsFields.BACKGROUND_SYNC_CHARGING_ONLY.name,
                false,
            ),
        )

    fun setBackgroundSyncRestrictions(restrictions: BackgroundSyncRestrictions) {
        settings[SettingsFields.BACKGROUND_SYNC_WIFI_ONLY.name] = restrictions.syncOnlyOnWifi
        settings[SettingsFields.BACKGROUND_SYNC_CHARGING_ONLY.name] =
            restrictions.syncOnlyWhenCharging
        backgroundSyncRestrictionsMutableFlow.update { restrictions }
    }

    fun setBackgroundSyncOnlyOnWifi(value: Boolean) {
        setBackgroundSyncRestrictions(
            getBackgroundSyncRestrictions().copy(syncOnlyOnWifi = value),
        )
    }

    fun setBackgroundSyncOnlyWhenCharging(value: Boolean) {
        setBackgroundSyncRestrictions(
            getBackgroundSyncRestrictions().copy(syncOnlyWhenCharging = value),
        )
    }

    internal fun getRefreshFeedsOnLaunch(): Boolean =
        settings.getBoolean(SettingsFields.REFRESH_FEEDS_ON_LAUNCH.name, true)

    internal fun setRefreshFeedsOnLaunch(value: Boolean) =
        settings.set(SettingsFields.REFRESH_FEEDS_ON_LAUNCH.name, value)

    fun getThemeMode(): ThemeMode =
        settings.getString(SettingsFields.THEME_MODE.name, ThemeMode.SYSTEM.name)
            .let { ThemeMode.valueOf(it) }

    fun setThemeMode(mode: ThemeMode) {
        settings[SettingsFields.THEME_MODE.name] = mode.name
        themeModeMutableFlow.update { mode }
    }

    fun getReduceMotionEnabled(): Boolean =
        settings.getBoolean(SettingsFields.REDUCE_MOTION_ENABLED.name, false)

    fun setReduceMotionEnabled(value: Boolean) {
        settings[SettingsFields.REDUCE_MOTION_ENABLED.name] = value
        reduceMotionEnabledMutableFlow.update { value }
    }

    fun getNotificationMode(): NotificationMode =
        settings.getString(SettingsFields.NOTIFICATION_MODE.name, NotificationMode.FEED_SOURCE.name)
            .let { NotificationMode.valueOf(it) }

    fun setNotificationMode(mode: NotificationMode) =
        settings.set(SettingsFields.NOTIFICATION_MODE.name, mode.name)

    internal companion object {
        const val DEFAULT_READER_MODE_FONT_SIZE = ReaderModeDefaults.FONT_SIZE
    }
}

private enum class SettingsFields {
    FAVOURITE_BROWSER_ID,
    MARK_FEED_AS_READ_WHEN_SCROLLING,
    SHOW_READ_ARTICLES_TIMELINE,
    HIDE_READ_ITEMS,
    USE_READER_MODE,
    SAVE_ITEM_CONTENT_ON_OPEN,
    PREFETCH_ARTICLE_CONTENT,
    IS_SYNC_UPLOAD_REQUIRED,
    READER_MODE_FONT_SIZE,
    READER_MODE_LINE_HEIGHT,
    AUTO_DELETE_PERIOD,
    CRASH_REPORTING_ENABLED,
    SYNC_PERIOD,
    BACKGROUND_SYNC_WIFI_ONLY,
    BACKGROUND_SYNC_CHARGING_ONLY,
    THEME_MODE,
    REDUCE_MOTION_ENABLED,
    REFRESH_FEEDS_ON_LAUNCH,
    NOTIFICATION_MODE,
    UNCATEGORIZED_CATEGORY_POSITION,
}
