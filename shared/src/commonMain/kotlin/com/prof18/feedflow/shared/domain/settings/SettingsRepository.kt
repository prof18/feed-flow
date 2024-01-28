package com.prof18.feedflow.shared.domain.settings

import com.prof18.feedflow.shared.data.SettingsHelper

class SettingsRepository internal constructor(
    private val settingsHelper: SettingsHelper,
) {
    fun setMarkFeedAsReadWhenScrolling(value: Boolean) =
        settingsHelper.setMarkFeedAsReadWhenScrolling(value)

    fun isMarkFeedAsReadWhenScrollingEnabled(): Boolean =
        settingsHelper.getMarkFeedAsReadWhenScrolling()

    fun isShowReadArticlesTimelineEnabled(): Boolean =
        settingsHelper.getShowReadArticlesTimeline()

    fun setShowReadArticlesTimeline(value: Boolean) =
        settingsHelper.setShowReadArticlesTimeline(value)
}
