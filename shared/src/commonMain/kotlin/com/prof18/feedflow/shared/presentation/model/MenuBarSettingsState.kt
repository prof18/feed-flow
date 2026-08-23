package com.prof18.feedflow.shared.presentation.model

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.domain.model.SyncPeriod

data class MenuBarSettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDesktopMultiPaneLayoutEnabled: Boolean = true,
    val isMarkReadWhenScrollingEnabled: Boolean = true,
    val isShowReadItemsEnabled: Boolean = false,
    val isHideReadItemsEnabled: Boolean = false,
    val articleOpenMode: ArticleOpenMode = ArticleOpenMode.FULL_ARTICLE,
    val isSaveReaderModeContentEnabled: Boolean = false,
    val isPrefetchArticleContentEnabled: Boolean = false,
    val isKleadParserEnabled: Boolean = false,
    val isRefreshFeedsOnLaunchEnabled: Boolean = true,
    val syncPeriod: SyncPeriod = SyncPeriod.NEVER,
    val isReduceMotionEnabled: Boolean = false,
    val isHideUnreadCountEnabled: Boolean = false,
    val autoDeletePeriod: AutoDeletePeriod = AutoDeletePeriod.DISABLED,
    val isCrashReportingEnabled: Boolean = true,
    val feedOrder: FeedOrder = FeedOrder.NEWEST_FIRST,
)
