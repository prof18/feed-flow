package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock

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

    private val syncPeriodMutableFlow = MutableStateFlow(getSyncPeriod())
    val syncPeriodFlow: StateFlow<SyncPeriod> = syncPeriodMutableFlow.asStateFlow()

    private val isExperimentalParsingEnabledMutableFlow = MutableStateFlow(
        isExperimentalParsingEnabled(),
    )
    val isExperimentalParsingEnabledFlow: StateFlow<Boolean> = isExperimentalParsingEnabledMutableFlow.asStateFlow()

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

    fun isExperimentalParsingEnabled(): Boolean =
        settings.getBoolean(SettingsFields.USE_EXPERIMENTAL_PARSING.name, false)

    fun setExperimentalParsing(value: Boolean) {
        settings[SettingsFields.USE_EXPERIMENTAL_PARSING.name] = value
        isExperimentalParsingEnabledMutableFlow.update { value }
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
    USE_EXPERIMENTAL_PARSING,
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
}
