package com.prof18.feedflow.shared.domain.settings

import com.prof18.feedflow.shared.data.SettingsHelper

class SettingsRepository internal constructor(
    private val settingsHelper: SettingsHelper,
) {

    private var isReaderModeEnabled: Boolean? = null

    fun setMarkFeedAsReadWhenScrolling(value: Boolean) =
        settingsHelper.setMarkFeedAsReadWhenScrolling(value)

    fun isMarkFeedAsReadWhenScrollingEnabled(): Boolean =
        settingsHelper.getMarkFeedAsReadWhenScrolling()

    fun isShowReadArticlesTimelineEnabled(): Boolean =
        settingsHelper.getShowReadArticlesTimeline()

    fun setShowReadArticlesTimeline(value: Boolean) =
        settingsHelper.setShowReadArticlesTimeline(value)

    fun isUseReaderModeEnabled(): Boolean {
        if (isReaderModeEnabled != null) {
            return requireNotNull(isReaderModeEnabled)
        } else {
            val value = settingsHelper.getUseReaderMode()
            isReaderModeEnabled = value
            return value
        }
    }

    fun setUseReaderMode(value: Boolean) {
        isReaderModeEnabled = value
        settingsHelper.setUseReaderMode(value)
    }
}
