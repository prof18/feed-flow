package com.prof18.feedflow.android.widget

import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.shared.domain.model.SyncPeriod

data class WidgetSettingsState(
    val syncPeriod: SyncPeriod = SyncPeriod.ONE_HOUR,
    val feedLayout: FeedLayout = FeedLayout.LIST,
    val showHeader: Boolean = true,
    val fontScale: Int = 0,
    val backgroundColor: Int? = null,
    val backgroundOpacityPercent: Int = 100,
)
