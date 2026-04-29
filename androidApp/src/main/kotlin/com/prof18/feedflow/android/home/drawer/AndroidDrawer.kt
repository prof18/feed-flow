package com.prof18.feedflow.android.home.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.toImmutableList

@Composable
fun AndroidDrawer(
    displayState: HomeDisplayState,
    feedManagementActions: FeedManagementActions,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    modifier: Modifier = Modifier,
    onFeedSuggestionsClick: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
) {
    val onFeedSourceClick: (FeedSource) -> Unit = { feedSource ->
        onFeedFilterSelected(FeedFilter.Source(feedSource))
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.regular)
            .padding(top = Spacing.regular),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            val timelineItem = displayState.navDrawerState.timeline
                .filterIsInstance<DrawerItem.Timeline>()
                .firstOrNull()
                ?: DrawerItem.Timeline(unreadCount = 0)

            DrawerTimelineItem(
                currentFeedFilter = displayState.currentFeedFilter,
                onFeedFilterSelected = onFeedFilterSelected,
                drawerItem = timelineItem,
                drawerItemVisualStyle = DefaultDrawerItemVisualStyle,
            )
        }

        item {
            DrawerReadItem(
                currentFeedFilter = displayState.currentFeedFilter,
                onFeedFilterSelected = onFeedFilterSelected,
                drawerItemVisualStyle = DefaultDrawerItemVisualStyle,
            )
        }

        item {
            DrawerBookmarksItem(
                currentFeedFilter = displayState.currentFeedFilter,
                onFeedFilterSelected = onFeedFilterSelected,
                drawerItem = displayState.navDrawerState.bookmarks
                    .filterIsInstance<DrawerItem.Bookmarks>()
                    .firstOrNull()
                    ?: DrawerItem.Bookmarks(unreadCount = 0),
                drawerItemVisualStyle = DefaultDrawerItemVisualStyle,
            )
        }

        item {
            DrawerFeedSuggestionsItem(
                onFeedSuggestionsClick = onFeedSuggestionsClick,
                drawerItemVisualStyle = DefaultDrawerItemVisualStyle,
            )
        }

        item {
            DrawerAddItem(
                onAddFeedClicked = feedManagementActions.onAddFeedClick,
                drawerItemVisualStyle = DefaultDrawerItemVisualStyle,
            )
        }

        if (displayState.navDrawerState.pinnedFeedSources.isNotEmpty()) {
            item {
                DrawerDivider()
            }

            item {
                Column {
                    Text(
                        modifier = Modifier
                            .padding(start = Spacing.regular)
                            .padding(bottom = Spacing.regular),
                        text = LocalFeedFlowStrings.current.drawerTitlePinnedFeeds,
                        style = MaterialTheme.typography.labelLarge,
                    )

                    AndroidDrawerFeedSourcesList(
                        drawerFeedSources = displayState.navDrawerState.pinnedFeedSources
                            .filterIsInstance<DrawerItem.DrawerFeedSource>().toImmutableList(),
                        currentFeedFilter = displayState.currentFeedFilter,
                        drawerItemVisualStyle = DefaultDrawerItemVisualStyle,
                        onFeedSourceClick = onFeedSourceClick,
                        onEditFeedClick = feedManagementActions.onEditFeedClick,
                        onDeleteFeedSourceClick = feedManagementActions.onDeleteFeedSourceClick,
                        onPinFeedClick = feedManagementActions.onPinFeedClick,
                        onChangeFeedCategoryClick = feedManagementActions.onChangeFeedCategoryClick,
                        onOpenWebsite = feedManagementActions.onOpenWebsite,
                    )
                }
            }
        }

        if (displayState.navDrawerState.feedSourcesByCategory.isNotEmpty() ||
            displayState.navDrawerState.feedSourcesWithoutCategory.isNotEmpty()
        ) {
            item {
                AndroidDrawerFeedSourcesByCategories(
                    navDrawerState = displayState.navDrawerState,
                    currentFeedFilter = displayState.currentFeedFilter,
                    drawerItemVisualStyle = DefaultDrawerItemVisualStyle,
                    onFeedFilterSelected = onFeedFilterSelected,
                    onFeedSourceClick = onFeedSourceClick,
                    onEditFeedClick = feedManagementActions.onEditFeedClick,
                    onDeleteFeedSourceClick = feedManagementActions.onDeleteFeedSourceClick,
                    onPinFeedClick = feedManagementActions.onPinFeedClick,
                    onChangeFeedCategoryClick = feedManagementActions.onChangeFeedCategoryClick,
                    onOpenWebsite = feedManagementActions.onOpenWebsite,
                    onEditCategoryClick = feedManagementActions.onEditCategoryClick,
                    onDeleteCategoryClick = feedManagementActions.onDeleteCategoryClick,
                )
            }
        }
    }
}
