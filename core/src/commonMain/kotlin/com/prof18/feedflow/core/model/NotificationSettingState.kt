package com.prof18.feedflow.core.model

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList

@Stable
data class NotificationSettingState(
    val feedSources: ImmutableList<FeedSourceNotificationPreference>,
    val isEnabledForAll: Boolean,
)

data class FeedSourceNotificationPreference(
    val feedSourceId: String,
    val feedSourceTitle: String,
    val isEnabled: Boolean,
)
