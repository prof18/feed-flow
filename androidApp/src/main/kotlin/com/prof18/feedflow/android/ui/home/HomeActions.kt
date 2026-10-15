package com.prof18.feedflow.android.ui.home

import androidx.compose.runtime.Stable
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.DrawerItem.DrawerFeedSource
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedItemUrlTitle
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.VisibleFeedItem

@Stable
data class FeedListActions(
    val onClearOldArticlesClicked: () -> Unit,
    val onDeleteDatabaseClick: () -> Unit,
    val refreshData: () -> Unit,
    val requestNewData: () -> Unit,
    val markAllRead: () -> Unit,
    val onBackToTimelineClick: () -> Unit,
    val onVisibleFeedItemsChanged: (List<VisibleFeedItem>) -> Unit,
    val markAsRead: (FeedItemId) -> Unit,
    val openUrl: (FeedItemUrlInfo) -> Unit,
    val openInBrowser: (FeedItemUrlInfo) -> Unit,
    val updateBookmarkStatus: (FeedItemId, Boolean) -> Unit,
    val updateReadStatus: (FeedItemId, Boolean) -> Unit,
    val markAllAboveAsRead: (String) -> Unit,
    val markAllBelowAsRead: (String) -> Unit,
    val onNavigateNext: () -> Unit = {},
)

@Stable
data class FeedManagementActions(
    val onAddFeedClick: () -> Unit,
    val onFeedFilterSelected: (FeedFilter) -> Unit,
    val onEditFeedClick: (FeedSource) -> Unit,
    val onDeleteFeedSourceClick: (FeedSource) -> Unit,
    val onPinFeedClick: (FeedSource) -> Unit,
    val onEditCategoryClick: (CategoryId, CategoryName) -> Unit,
    val validateCategoryName: (CategoryId?, CategoryName) -> CategoryNameValidationResult,
    val onDeleteCategoryClick: (CategoryId) -> Unit,
    val onChangeFeedCategoryClick: (FeedSource) -> Unit,
    val onOpenWebsite: (String) -> Unit,
    val onMoveFeedSourcesToCategory: (List<FeedSource>, FeedSourceCategory?) -> Unit = { _, _ -> },
    val onReorderPinnedFeedSources: (List<FeedSource>) -> Unit = { _ -> },
    val onReorderCategories: (List<DrawerFeedSource.FeedSourceCategoryWrapper>) -> Unit = { _ -> },
    val onReorderFeedSources: (List<FeedSource>) -> Unit = { _ -> },
    val onDeleteAllFeedsInCategoryClick: (List<FeedSource>) -> Unit = { _ -> },
    val onDeleteAllFeedsInCategoryByIdClick: (CategoryId) -> Unit = { _ -> },
    val onMarkAllReadForFeedSourceClick: (FeedSource) -> Unit = { _ -> },
    val onMarkAllReadForCategoryClick: (FeedSourceCategory) -> Unit = { _ -> },
)

@Stable
data class ShareBehavior(
    val onShareClick: (FeedItemUrlTitle) -> Unit,
    val shareLinkTitle: String,
    val shareCommentsTitle: String,
)
