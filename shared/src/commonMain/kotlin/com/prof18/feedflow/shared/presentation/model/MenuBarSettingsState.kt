package com.prof18.feedflow.shared.presentation.model

import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.ThemeMode

data class MenuBarSettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isMarkReadWhenScrollingEnabled: Boolean = true,
    val isShowReadItemsEnabled: Boolean = false,
    val isHideReadItemsEnabled: Boolean = false,
    val isReaderModeEnabled: Boolean = false,
    val isSaveReaderModeContentEnabled: Boolean = false,
    val isPrefetchArticleContentEnabled: Boolean = false,
    val isRefreshFeedsOnLaunchEnabled: Boolean = true,
    val isShowRssParsingErrorsEnabled: Boolean = true,
    val isReduceMotionEnabled: Boolean = false,
    val autoDeletePeriod: AutoDeletePeriod = AutoDeletePeriod.DISABLED,
    val isCrashReportingEnabled: Boolean = true,
    val feedOrder: FeedOrder = FeedOrder.NEWEST_FIRST,
)
