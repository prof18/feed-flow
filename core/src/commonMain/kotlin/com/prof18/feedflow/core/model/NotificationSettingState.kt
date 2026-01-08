package com.prof18.feedflow.core.model

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList

@Stable
data class NotificationSettingState(
    val feedSources: ImmutableList<FeedSourceNotificationPreference>,
    val isEnabledForAll: Boolean,
    val notificationMode: NotificationMode,
)

data class FeedSourceNotificationPreference(
    val feedSourceId: String,
    val feedSourceTitle: String,
    val isEnabled: Boolean,
)
