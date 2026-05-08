package com.prof18.feedflow.android.home.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.home.AddFeedOptionsBottomSheet
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.toImmutableList

private val drawerAddFeedButtonListPadding = 88.dp

@Composable
fun AndroidDrawer(
    displayState: HomeDisplayState,
    feedManagementActions: FeedManagementActions,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    onAddFeedClick: () -> Unit,
    onFeedSuggestionsClick: () -> Unit,
    onImportExportClick: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val onFeedSourceClick: (FeedSource) -> Unit = { feedSource ->
        onFeedFilterSelected(FeedFilter.Source(feedSource))
    }
    var showAddFeedOptionsSheet by rememberSaveable { mutableStateOf(false) }
    val addFeedOptionsSheetState = rememberModalBottomSheetState()

    if (showAddFeedOptionsSheet) {
        AddFeedOptionsBottomSheet(
            sheetState = addFeedOptionsSheetState,
            onAddFeedClick = onAddFeedClick,
            onFeedSuggestionsClick = onFeedSuggestionsClick,
            onImportExportClick = onImportExportClick,
            onDismiss = { showAddFeedOptionsSheet = false },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.regular)
                .padding(top = Spacing.regular),
            contentPadding = PaddingValues(bottom = drawerAddFeedButtonListPadding),
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
                        validateCategoryName = feedManagementActions.validateCategoryName,
                        onDeleteCategoryClick = feedManagementActions.onDeleteCategoryClick,
                    )
                }
            }
        }

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.regular),
            onClick = { showAddFeedOptionsSheet = true },
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = LocalFeedFlowStrings.current.addFeed,
            )
        }
    }
}
