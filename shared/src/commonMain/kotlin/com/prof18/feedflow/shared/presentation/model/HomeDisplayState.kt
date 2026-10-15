package com.prof18.feedflow.shared.presentation.model

import androidx.compose.runtime.Stable
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedUpdateStatus
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.core.model.SwipeActions
import kotlinx.collections.immutable.ImmutableList

@Stable
data class HomeDisplayState(
    val feedItems: ImmutableList<FeedItem>,
    val navDrawerState: NavDrawerState,
    val unReadCount: Long,
    val isUnreadCountHidden: Boolean,
    val feedUpdateStatus: FeedUpdateStatus,
    val feedFontSizes: FeedFontSizes,
    val currentFeedFilter: FeedFilter,
    val nextFeedDisplayState: NextFeedDisplayState,
    val swipeActions: SwipeActions,
    val feedLayout: FeedLayout,
    val isGridLayoutEnabled: Boolean,
    val isSyncUploadRequired: Boolean = false,
    val feedItemDisplaySettings: FeedItemDisplaySettings = FeedItemDisplaySettings(),
)

@Stable
sealed class NextFeedDisplayState {
    data class NextFeedDisplayEnabledState(val title: String) : NextFeedDisplayState()

    data object NextFeedDisplayDisabledState : NextFeedDisplayState()
}
