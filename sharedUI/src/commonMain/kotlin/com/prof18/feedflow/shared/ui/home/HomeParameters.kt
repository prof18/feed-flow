package com.prof18.feedflow.shared.ui.home

import androidx.compose.runtime.Stable
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedItemUrlTitle
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedUpdateStatus
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.core.model.SwipeActions
import kotlinx.collections.immutable.ImmutableList

@Stable
class HomeDisplayState(
    val feedItems: ImmutableList<FeedItem>,
    val navDrawerState: NavDrawerState,
    val unReadCount: Long,
    val feedUpdateStatus: FeedUpdateStatus,
    val feedFontSizes: FeedFontSizes,
    val currentFeedFilter: FeedFilter,
    val swipeActions: SwipeActions,
    val feedLayout: FeedLayout,
    val isSyncUploadRequired: Boolean = false,
)

@Stable
class FeedListActions(
    val onClearOldArticlesClicked: () -> Unit,
    val onDeleteDatabaseClick: () -> Unit,
    val refreshData: () -> Unit,
    val requestNewData: () -> Unit,
    val forceRefreshData: () -> Unit,
    val markAllRead: () -> Unit,
    val onBackToTimelineClick: () -> Unit,
    val markAsReadOnScroll: (Int) -> Unit,
    val markAsRead: (FeedItemId) -> Unit,
    val openUrl: (FeedItemUrlInfo) -> Unit,
    val updateBookmarkStatus: (FeedItemId, Boolean) -> Unit,
    val updateReadStatus: (FeedItemId, Boolean) -> Unit,
)

@Stable
class FeedManagementActions(
    val onAddFeedClick: () -> Unit,
    val onFeedFilterSelected: (FeedFilter) -> Unit,
    val onEditFeedClick: (FeedSource) -> Unit,
    val onDeleteFeedSourceClick: (FeedSource) -> Unit,
    val onPinFeedClick: (FeedSource) -> Unit,
    val onEditCategoryClick: (CategoryId, CategoryName) -> Unit,
    val onDeleteCategoryClick: (CategoryId) -> Unit,
    val onOpenWebsite: (String) -> Unit,
)

@Stable
class ShareBehavior(
    val onShareClick: (FeedItemUrlTitle) -> Unit,
    val shareLinkTitle: String,
    val shareCommentsTitle: String,
)
