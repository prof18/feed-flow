package com.prof18.feedflow.core.model

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList

@Stable
data class NotificationSettingState(
    val feedSources: ImmutableList<FeedSourceNotificationPreference>,
    val feedGroups: ImmutableList<NotificationFeedGroup>,
    val searchQuery: String,
    val isEnabledForAll: Boolean,
    val notificationMode: NotificationMode,
)

data class FeedSourceNotificationPreference(
    val feedSourceId: String,
    val feedSourceTitle: String,
    val categoryTitle: String?,
    val isEnabled: Boolean,
)

data class NotificationFeedGroup(
    val categoryTitle: String?,
    val feedSources: ImmutableList<FeedSourceNotificationPreference>,
)
