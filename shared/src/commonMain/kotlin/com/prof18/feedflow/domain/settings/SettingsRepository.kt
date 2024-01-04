package com.prof18.feedflow.domain.settings

import com.prof18.feedflow.data.SettingsHelper

class SettingsRepository internal constructor(
    private val settingsHelper: SettingsHelper,
) {
    fun setMarkFeedAsReadWhenScrolling(value: Boolean) =
        settingsHelper.setMarkFeedAsReadWhenScrolling(value)

    fun isMarkFeedAsReadWhenScrollingEnabled(): Boolean =
        settingsHelper.getMarkFeedAsReadWhenScrolling()
}
