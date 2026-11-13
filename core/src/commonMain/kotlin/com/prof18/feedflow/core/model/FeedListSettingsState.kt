package com.prof18.feedflow.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class FeedListSettingsState(
    val isHideDescriptionEnabled: Boolean = false,
    val isHideImagesEnabled: Boolean = false,
    val isHideDateEnabled: Boolean = false,
    val dateFormat: DateFormat = DateFormat.NORMAL,
    val timeFormat: TimeFormat = TimeFormat.HOURS_24,
    val feedLayout: FeedLayout = FeedLayout.LIST,
    val isGridLayoutEnabled: Boolean = false,
    val fontScale: Int = 0,
    val leftSwipeActionType: SwipeActionType = SwipeActionType.NONE,
    val rightSwipeActionType: SwipeActionType = SwipeActionType.NONE,
    val isRemoveTitleFromDescriptionEnabled: Boolean = false,
    val feedOrder: FeedOrder = FeedOrder.NEWEST_FIRST,
    val isHideUnreadDotEnabled: Boolean = false,
    val isHideFeedSourceEnabled: Boolean = false,
    val descriptionLineLimit: DescriptionLineLimit = DescriptionLineLimit.THREE,
)
