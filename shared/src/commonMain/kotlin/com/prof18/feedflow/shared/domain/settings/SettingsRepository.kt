package com.prof18.feedflow.shared.domain.settings

import com.prof18.feedflow.shared.data.SettingsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsRepository internal constructor(
    private val settingsHelper: SettingsHelper,
) {

    private var isReaderModeEnabled: Boolean? = null

    private val isSyncUploadRequiredMutableFlow = MutableStateFlow(settingsHelper.getIsSyncUploadRequired())

    internal val isSyncUploadRequired: StateFlow<Boolean> = isSyncUploadRequiredMutableFlow.asStateFlow()

    internal fun setMarkFeedAsReadWhenScrolling(value: Boolean) =
        settingsHelper.setMarkFeedAsReadWhenScrolling(value)

    internal fun isMarkFeedAsReadWhenScrollingEnabled(): Boolean =
        settingsHelper.getMarkFeedAsReadWhenScrolling()

    internal fun isShowReadArticlesTimelineEnabled(): Boolean =
        settingsHelper.getShowReadArticlesTimeline()

    internal fun setShowReadArticlesTimeline(value: Boolean) =
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

    internal fun setUseReaderMode(value: Boolean) {
        isReaderModeEnabled = value
        settingsHelper.setUseReaderMode(value)
    }

    internal fun setIsSyncUploadRequired(value: Boolean) {
        isSyncUploadRequiredMutableFlow.update { value }
        settingsHelper.setIsSyncUploadRequired(value)
    }

    internal fun getIsSyncUploadRequired(): Boolean =
        settingsHelper.getIsSyncUploadRequired()
}
