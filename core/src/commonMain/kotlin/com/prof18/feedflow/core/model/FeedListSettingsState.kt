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
    val fontScale: Int = 0,
    val leftSwipeActionType: SwipeActionType = SwipeActionType.TOGGLE_READ_STATUS,
    val rightSwipeActionType: SwipeActionType = SwipeActionType.TOGGLE_BOOKMARK_STATUS,
    val isRemoveTitleFromDescriptionEnabled: Boolean = false,
    val feedOrder: FeedOrder = FeedOrder.NEWEST_FIRST,
    val isHideUnreadDotEnabled: Boolean = false,
    val isHideFeedSourceEnabled: Boolean = false,
    val descriptionLineLimit: DescriptionLineLimit = DescriptionLineLimit.THREE,
)
