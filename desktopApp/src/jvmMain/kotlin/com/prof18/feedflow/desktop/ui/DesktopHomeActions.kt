package com.prof18.feedflow.desktop.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.UriHandler
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
import com.prof18.feedflow.desktop.utils.openUriSafely

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
    val refreshEnabled: Boolean = true,
    val bulkActionsEnabled: Boolean = true,
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
    val onOpenWebsite: (String) -> Unit,
    val onMoveFeedSourcesToCategory: (List<FeedSource>, FeedSourceCategory?) -> Unit = { _, _ -> },
    val onReorderPinnedFeedSources: (List<FeedSource>) -> Unit = { _ -> },
    val onReorderCategories: (List<DrawerFeedSource.FeedSourceCategoryWrapper>) -> Unit = { _ -> },
    val onReorderFeedSources: (List<FeedSource>) -> Unit = { _ -> },
    val onDeleteAllFeedsInCategoryClick: (List<FeedSource>) -> Unit = { _ -> },
    val onDeleteAllFeedsInCategoryByIdClick: (CategoryId) -> Unit = { _ -> },
    val onMarkAllReadForFeedSourceClick: (FeedSource) -> Unit = { _ -> },
    val onMarkAllReadForCategoryClick: (FeedSourceCategory) -> Unit = { _ -> },
    val onFeedSuggestionsClick: () -> Unit = {},
    val onImportFeedsClick: () -> Unit = {},
)

@Stable
data class ShareBehavior(
    val onShareClick: (FeedItemUrlTitle) -> Unit,
    val shareLinkTitle: String,
    val shareCommentsTitle: String,
)

internal fun desktopExternalLinkAction(
    uriHandler: UriHandler,
    onBrowserError: () -> Unit,
): (String) -> Unit = { url ->
    if (!uriHandler.openUriSafely(url)) {
        onBrowserError()
    }
}
