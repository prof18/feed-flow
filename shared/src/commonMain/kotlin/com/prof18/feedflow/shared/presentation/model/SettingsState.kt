package com.prof18.feedflow.shared.presentation.model

import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedItemType
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.shared.domain.model.SyncPeriod

data class SettingsState(
    val isMarkReadWhenScrollingEnabled: Boolean = true,
    val isShowReadItemsEnabled: Boolean = false,
    val isReaderModeEnabled: Boolean = false,
    val isExperimentalParsingEnabled: Boolean = false,
    val isRemoveTitleFromDescriptionEnabled: Boolean = false,
    val isHideDescriptionEnabled: Boolean = false,
    val isHideImagesEnabled: Boolean = false,
    val autoDeletePeriod: AutoDeletePeriod = AutoDeletePeriod.DISABLED,
    val isCrashReportingEnabled: Boolean = true,
    val syncPeriod: SyncPeriod = SyncPeriod.ONE_HOUR,
    val leftSwipeActionType: SwipeActionType = SwipeActionType.NONE,
    val rightSwipeActionType: SwipeActionType = SwipeActionType.NONE,
    val dateFormat: DateFormat = DateFormat.NORMAL,
    val feedItemType: FeedItemType = FeedItemType.LIST_TILE
)
