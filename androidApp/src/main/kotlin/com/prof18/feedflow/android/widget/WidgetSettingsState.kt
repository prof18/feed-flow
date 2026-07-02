package com.prof18.feedflow.android.widget

import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode

data class WidgetSettingsState(
    val syncPeriod: SyncPeriod = SyncPeriod.ONE_HOUR,
    val feedLayout: WidgetFeedLayout = WidgetFeedLayout.LIST,
    val showHeader: Boolean = true,
    val fontScale: Int = 0,
    val backgroundColor: Int? = null,
    val backgroundOpacityPercent: Int = 100,
    val textColorMode: WidgetTextColorMode = WidgetTextColorMode.AUTOMATIC,
    val hideImages: Boolean = false,
)
