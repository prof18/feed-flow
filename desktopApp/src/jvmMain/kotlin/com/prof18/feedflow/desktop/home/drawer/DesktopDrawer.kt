package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList

@Composable
fun DesktopDrawer(
    displayState: HomeDisplayState,
    feedManagementActions: FeedManagementActions,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    modifier: Modifier = Modifier,
    onFeedSuggestionsClick: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val dragState = rememberFeedSourceDragState(listState)
    var drawerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var selectedFeedSourceIds by remember { mutableStateOf(persistentSetOf<String>()) }

    val allFeedSources = remember(displayState.navDrawerState) {
        buildMap<String, FeedSource> {
            displayState.navDrawerState.pinnedFeedSources
                .filterIsInstance<DrawerItem.DrawerFeedSource>()
                .forEach { put(it.feedSource.id, it.feedSource) }

            displayState.navDrawerState.feedSourcesWithoutCategory
                .filterIsInstance<DrawerItem.DrawerFeedSource>()
                .forEach { put(it.feedSource.id, it.feedSource) }

            displayState.navDrawerState.feedSourcesByCategory.values
                .flatten()
                .filterIsInstance<DrawerItem.DrawerFeedSource>()
                .forEach { put(it.feedSource.id, it.feedSource) }
        }
    }

    val onFeedFilterSelectedWithClear: (FeedFilter) -> Unit = { filter ->
        selectedFeedSourceIds = persistentSetOf()
        onFeedFilterSelected(filter)
    }

    val onFeedSourceClick: (FeedSource, Boolean) -> Unit = { feedSource, isMultiSelect ->
        if (isMultiSelect) {
            val currentSelectedId = (displayState.currentFeedFilter as? FeedFilter.Source)
                ?.feedSource
                ?.id
            val initialSelection = if (
                selectedFeedSourceIds.isEmpty() && currentSelectedId != null
            ) {
                persistentSetOf(currentSelectedId)
            } else {
                selectedFeedSourceIds
            }
            selectedFeedSourceIds = if (initialSelection.contains(feedSource.id)) {
                initialSelection.remove(feedSource.id)
            } else {
                initialSelection.add(feedSource.id)
            }
        } else {
            selectedFeedSourceIds = persistentSetOf()
            onFeedFilterSelected(FeedFilter.Source(feedSource))
        }
    }

    val onMoveFeedSourcesToCategory: (List<FeedSource>, FeedSourceCategory?) -> Unit =
        { feedSources, category ->
            feedManagementActions.onMoveFeedSourcesToCategory(feedSources, category)
            selectedFeedSourceIds = persistentSetOf()
        }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.regular)
            .onGloballyPositioned { drawerCoordinates = it },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { dragState.updateListBounds(it) },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                val timelineItem = displayState.navDrawerState.timeline
                    .filterIsInstance<DrawerItem.Timeline>()
                    .firstOrNull()
                    ?: DrawerItem.Timeline(unreadCount = 0)

                DrawerTimelineItem(
                    currentFeedFilter = displayState.currentFeedFilter,
                    onFeedFilterSelected = onFeedFilterSelectedWithClear,
                    drawerItem = timelineItem,
                    drawerItemVisualStyle = desktopDrawerItemVisualStyle(),
                )
            }

            item {
                DrawerReadItem(
                    currentFeedFilter = displayState.currentFeedFilter,
                    onFeedFilterSelected = onFeedFilterSelectedWithClear,
                    drawerItemVisualStyle = desktopDrawerItemVisualStyle(),
                )
            }

            item {
                DrawerBookmarksItem(
                    currentFeedFilter = displayState.currentFeedFilter,
                    onFeedFilterSelected = onFeedFilterSelectedWithClear,
                    drawerItem = displayState.navDrawerState.bookmarks
                        .filterIsInstance<DrawerItem.Bookmarks>()
                        .firstOrNull()
                        ?: DrawerItem.Bookmarks(unreadCount = 0),
                    drawerItemVisualStyle = desktopDrawerItemVisualStyle(),
                )
            }

            item {
                DrawerFeedSuggestionsItem(
                    onFeedSuggestionsClick = onFeedSuggestionsClick,
                    drawerItemVisualStyle = desktopDrawerItemVisualStyle(),
                )
            }

            item {
                DrawerAddItem(
                    onAddFeedClicked = feedManagementActions.onAddFeedClick,
                    drawerItemVisualStyle = desktopDrawerItemVisualStyle(),
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

                        DesktopDrawerFeedSourcesList(
                            drawerFeedSources = displayState.navDrawerState.pinnedFeedSources
                                .filterIsInstance<DrawerItem.DrawerFeedSource>().toImmutableList(),
                            currentFeedFilter = displayState.currentFeedFilter,
                            drawerItemVisualStyle = desktopDrawerItemVisualStyle(),
                            selectedFeedSourceIds = selectedFeedSourceIds,
                            onFeedSourceClick = onFeedSourceClick,
                            selectedFeedSourcesProvider = { selectedFeedSourceIds.mapNotNull { allFeedSources[it] } },
                            onEditFeedClick = feedManagementActions.onEditFeedClick,
                            onDeleteFeedSourceClick = feedManagementActions.onDeleteFeedSourceClick,
                            onPinFeedClick = feedManagementActions.onPinFeedClick,
                            onChangeFeedCategoryClick = feedManagementActions.onChangeFeedCategoryClick,
                            onOpenWebsite = feedManagementActions.onOpenWebsite,
                            onMoveFeedSourcesToCategory = onMoveFeedSourcesToCategory,
                            dragState = dragState,
                        )
                    }
                }
            }

            if (displayState.navDrawerState.feedSourcesByCategory.isNotEmpty() ||
                displayState.navDrawerState.feedSourcesWithoutCategory.isNotEmpty()
            ) {
                item {
                    DesktopDrawerFeedSourcesByCategories(
                        navDrawerState = displayState.navDrawerState,
                        currentFeedFilter = displayState.currentFeedFilter,
                        drawerItemVisualStyle = desktopDrawerItemVisualStyle(),
                        onFeedFilterSelected = onFeedFilterSelectedWithClear,
                        selectedFeedSourceIds = selectedFeedSourceIds,
                        onFeedSourceClick = onFeedSourceClick,
                        selectedFeedSourcesProvider = { selectedFeedSourceIds.mapNotNull { allFeedSources[it] } },
                        onEditFeedClick = feedManagementActions.onEditFeedClick,
                        onDeleteFeedSourceClick = feedManagementActions.onDeleteFeedSourceClick,
                        onPinFeedClick = feedManagementActions.onPinFeedClick,
                        onChangeFeedCategoryClick = feedManagementActions.onChangeFeedCategoryClick,
                        onOpenWebsite = feedManagementActions.onOpenWebsite,
                        onEditCategoryClick = feedManagementActions.onEditCategoryClick,
                        onDeleteCategoryClick = feedManagementActions.onDeleteCategoryClick,
                        onMoveFeedSourcesToCategory = onMoveFeedSourcesToCategory,
                        dragState = dragState,
                    )
                }
            }
        }

        DragGhost(
            dragState = dragState,
            drawerCoordinates = drawerCoordinates,
        )
    }
}
