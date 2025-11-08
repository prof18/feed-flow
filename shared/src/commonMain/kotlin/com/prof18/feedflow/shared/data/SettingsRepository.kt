package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

class SettingsRepository(
    private val settings: Settings,
) {
    private var isReaderModeEnabled: Boolean? = null

    private val isSyncUploadRequiredMutableFlow = MutableStateFlow(getIsSyncUploadRequired())
    val isSyncUploadRequired: StateFlow<Boolean> = isSyncUploadRequiredMutableFlow.asStateFlow()

    private val swipeActionsMutableFlow = MutableStateFlow(
        SwipeActions(
            leftSwipeAction = getSwipeAction(SwipeDirection.LEFT),
            rightSwipeAction = getSwipeAction(SwipeDirection.RIGHT),
        ),
    )
    val swipeActions: StateFlow<SwipeActions> = swipeActionsMutableFlow.asStateFlow()

    private val feedLayoutMutableFlow = MutableStateFlow(getFeedLayout())
    val feedLayout: StateFlow<FeedLayout> = feedLayoutMutableFlow.asStateFlow()

    private val feedWidgetLayoutMutableFlow = MutableStateFlow(getFeedWidgetLayout())
    val feedWidgetLayout: StateFlow<FeedLayout> = feedWidgetLayoutMutableFlow.asStateFlow()

    private val syncPeriodMutableFlow = MutableStateFlow(getSyncPeriod())
    val syncPeriodFlow: StateFlow<SyncPeriod> = syncPeriodMutableFlow.asStateFlow()

    private val themeModeMutableFlow = MutableStateFlow<ThemeMode>(getThemeMode())
    val themeModeFlow: StateFlow<ThemeMode> = themeModeMutableFlow.asStateFlow()

    private val reduceMotionMutableFlow = MutableStateFlow(getReduceMotion())
    val reduceMotionFlow: StateFlow<Boolean> = reduceMotionMutableFlow.asStateFlow()

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
            val value = settings.getBoolean(SettingsFields.USE_READER_MODE.name, true)
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

    fun getFeedListFontScaleFactor(): Int = settings.getInt(
        SettingsFields.FEED_LIST_FONT_SCALE_FACTOR.name,
        DEFAULT_FEED_LIST_FONT_SCALE_FACTOR,
    )

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

    fun getSyncPeriod(): SyncPeriod =
        settings.getString(SettingsFields.SYNC_PERIOD.name, SyncPeriod.NEVER.name)
            .let { SyncPeriod.valueOf(it) }

    fun setSyncPeriod(period: SyncPeriod) {
        settings[SettingsFields.SYNC_PERIOD.name] = period.name
        syncPeriodMutableFlow.update { period }
    }

    fun getFirstInstallationDate(): Long {
        val currentValue = settings.getLongOrNull(SettingsFields.FIRST_INSTALLATION_DATE.name)
        return if (currentValue == null) {
            val now = Clock.System.now().toEpochMilliseconds()
            setFirstInstallationDate(now)
            now
        } else {
            currentValue
        }
    }

    private fun setFirstInstallationDate(timestamp: Long) =
        settings.set(SettingsFields.FIRST_INSTALLATION_DATE.name, timestamp)

    fun getReviewRequestCount(): Int =
        settings.getInt(SettingsFields.REVIEW_REQUEST_COUNT.name, 0)

    fun incrementReviewRequestCount() {
        val currentCount = getReviewRequestCount()
        settings[SettingsFields.REVIEW_REQUEST_COUNT.name] = currentCount + 1
    }

    fun getLastReviewRequestDate(): Long =
        settings.getLong(SettingsFields.LAST_REVIEW_REQUEST_DATE.name, 0L)

    fun setLastReviewRequestDate(timestamp: Long) =
        settings.set(SettingsFields.LAST_REVIEW_REQUEST_DATE.name, timestamp)

    fun getLastReviewVersion(): String? =
        settings.getStringOrNull(SettingsFields.LAST_REVIEW_VERSION.name)

    fun setLastReviewVersion(version: String) =
        settings.set(SettingsFields.LAST_REVIEW_VERSION.name, version)

    fun getSwipeAction(direction: SwipeDirection): SwipeActionType {
        val fieldName = when (direction) {
            SwipeDirection.LEFT -> SettingsFields.LEFT_SWIPE_ACTION.name
            SwipeDirection.RIGHT -> SettingsFields.RIGHT_SWIPE_ACTION.name
        }
        return settings.getString(fieldName, SwipeActionType.NONE.name)
            .let { SwipeActionType.valueOf(it) }
    }

    fun setSwipeAction(direction: SwipeDirection, action: SwipeActionType) {
        val fieldName = when (direction) {
            SwipeDirection.LEFT -> SettingsFields.LEFT_SWIPE_ACTION.name
            SwipeDirection.RIGHT -> SettingsFields.RIGHT_SWIPE_ACTION.name
        }
        settings[fieldName] = action.name

        swipeActionsMutableFlow.update { currentSwipeActions ->
            when (direction) {
                SwipeDirection.LEFT -> currentSwipeActions.copy(leftSwipeAction = action)
                SwipeDirection.RIGHT -> currentSwipeActions.copy(rightSwipeAction = action)
            }
        }
    }

    fun getDateFormat(): DateFormat =
        settings.getString(SettingsFields.DATE_FORMAT.name, DateFormat.NORMAL.name)
            .let { DateFormat.valueOf(it) }

    fun setDateFormat(format: DateFormat) =
        settings.set(SettingsFields.DATE_FORMAT.name, format.name)

    fun getFeedOrder(): FeedOrder =
        settings.getString(SettingsFields.FEED_ORDER.name, FeedOrder.NEWEST_FIRST.name)
            .let { FeedOrder.valueOf(it) }

    fun setFeedOrder(order: FeedOrder) {
        settings[SettingsFields.FEED_ORDER.name] = order.name
    }

    fun getFeedLayout(): FeedLayout =
        settings.getString(SettingsFields.FEED_LAYOUT.name, FeedLayout.LIST.name)
            .let { FeedLayout.valueOf(it) }

    fun setFeedLayout(feedLayout: FeedLayout) {
        settings[SettingsFields.FEED_LAYOUT.name] = feedLayout.name
        feedLayoutMutableFlow.update { feedLayout }
    }

    fun getFeedWidgetLayout(): FeedLayout =
        settings.getString(SettingsFields.FEED_WIDGET_LAYOUT.name, FeedLayout.LIST.name)
            .let { FeedLayout.valueOf(it) }

    fun setFeedWidgetLayout(feedLayout: FeedLayout) {
        settings[SettingsFields.FEED_WIDGET_LAYOUT.name] = feedLayout.name
        feedWidgetLayoutMutableFlow.update { feedLayout }
    }

    fun getThemeMode(): ThemeMode =
        settings.getString(SettingsFields.THEME_MODE.name, ThemeMode.SYSTEM.name)
            .let { ThemeMode.valueOf(it) }

    fun setThemeMode(mode: ThemeMode) {
        settings[SettingsFields.THEME_MODE.name] = mode.name
        themeModeMutableFlow.update { mode }
    }

    fun getReduceMotion(): Boolean =
        settings.getBoolean(SettingsFields.REDUCE_MOTION_ENABLED.name, false)

    fun setReduceMotion(enabled: Boolean) {
        settings[SettingsFields.REDUCE_MOTION_ENABLED.name] = enabled
        reduceMotionMutableFlow.update { enabled }
    }

    fun getDesktopWindowWidthDp(): Int =
        settings.getInt(SettingsFields.DESKTOP_WINDOW_WIDTH_DP.name, defaultValue = 800)

    fun setDesktopWindowWidthDp(value: Int) =
        settings.set(SettingsFields.DESKTOP_WINDOW_WIDTH_DP.name, value)

    fun getDesktopWindowHeightDp(): Int =
        settings.getInt(SettingsFields.DESKTOP_WINDOW_HEIGHT_DP.name, defaultValue = 600)

    fun setDesktopWindowHeightDp(value: Int) =
        settings.set(SettingsFields.DESKTOP_WINDOW_HEIGHT_DP.name, value)

    fun getDesktopWindowXPositionDp(): Float? =
        settings.getFloatOrNull(SettingsFields.DESKTOP_WINDOW_X_POSITION_DP.name)

    fun setDesktopWindowXPositionDp(value: Float) =
        settings.set(SettingsFields.DESKTOP_WINDOW_X_POSITION_DP.name, value)

    fun getDesktopWindowYPositionDp(): Float? =
        settings.getFloatOrNull(SettingsFields.DESKTOP_WINDOW_Y_POSITION_DP.name)

    fun setDesktopWindowYPositionDp(value: Float) =
        settings.set(SettingsFields.DESKTOP_WINDOW_Y_POSITION_DP.name, value)

    private companion object {
        const val DEFAULT_READER_MODE_FONT_SIZE = 16
        const val DEFAULT_FEED_LIST_FONT_SCALE_FACTOR = 0
    }
}

internal enum class SettingsFields {
    FEED_ORDER,
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
    SYNC_PERIOD,
    FIRST_INSTALLATION_DATE,
    REVIEW_REQUEST_COUNT,
    LAST_REVIEW_REQUEST_DATE,
    LAST_REVIEW_VERSION,
    IS_KEYCHAIN_MIGRATION_DONE,
    LEFT_SWIPE_ACTION,
    RIGHT_SWIPE_ACTION,
    DATE_FORMAT,
    FEED_LAYOUT,
    FEED_WIDGET_LAYOUT,
    THEME_MODE,
    DESKTOP_WINDOW_WIDTH_DP,
    DESKTOP_WINDOW_HEIGHT_DP,
    DESKTOP_WINDOW_X_POSITION_DP,
    DESKTOP_WINDOW_Y_POSITION_DP,
    REDUCE_MOTION_ENABLED,
}
