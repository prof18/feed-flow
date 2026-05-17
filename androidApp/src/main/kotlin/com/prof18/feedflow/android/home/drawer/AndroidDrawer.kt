package com.prof18.feedflow.android.home.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

private val drawerActionsTopPadding = 8.dp
private val drawerActionsBottomPadding = 16.dp
private val drawerActionsListTopPadding = 80.dp
private val drawerActionsFadeHeight = 110.dp

@Composable
fun AndroidDrawer(
    displayState: HomeDisplayState,
    feedManagementActions: FeedManagementActions,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    onSettingsClick: () -> Unit,
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

    val drawerBackgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(drawerBackgroundColor),
    ) {
        val statusBarTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.regular),
            contentPadding = PaddingValues(
                top = drawerActionsListTopPadding + statusBarTopInset,
                bottom = Spacing.regular + navBarBottomInset,
            ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                DrawerSectionTitle(LocalFeedFlowStrings.current.drawerTitleLibrary)
            }

            item {
                val timelineItem = remember(displayState.navDrawerState.timeline) {
                    displayState.navDrawerState.timeline
                        .filterIsInstance<DrawerItem.Timeline>()
                        .firstOrNull()
                        ?: DrawerItem.Timeline(unreadCount = 0)
                }

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
                val bookmarksItem = remember(displayState.navDrawerState.bookmarks) {
                    displayState.navDrawerState.bookmarks
                        .filterIsInstance<DrawerItem.Bookmarks>()
                        .firstOrNull()
                        ?: DrawerItem.Bookmarks(unreadCount = 0)
                }
                DrawerBookmarksItem(
                    currentFeedFilter = displayState.currentFeedFilter,
                    onFeedFilterSelected = onFeedFilterSelected,
                    drawerItem = bookmarksItem,
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

                        val pinnedFeedSourceItems = remember(displayState.navDrawerState.pinnedFeedSources) {
                            displayState.navDrawerState.pinnedFeedSources
                                .filterIsInstance<DrawerItem.DrawerFeedSource>().toImmutableList()
                        }
                        AndroidDrawerFeedSourcesList(
                            drawerFeedSources = pinnedFeedSourceItems,
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

        DrawerTopFade(
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = drawerBackgroundColor,
            extraTopHeight = statusBarTopInset,
        )

        DrawerTopActions(
            onSettingsClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(
                    start = Spacing.regular,
                    top = drawerActionsTopPadding,
                    end = Spacing.regular,
                    bottom = drawerActionsBottomPadding,
                ),
            onAddClick = { showAddFeedOptionsSheet = true },
        )
    }
}

@Composable
private fun DrawerTopFade(
    backgroundColor: Color,
    extraTopHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val totalHeight = drawerActionsFadeHeight + extraTopHeight
    val opaqueFraction = if (totalHeight.value > 0f) {
        (extraTopHeight.value / totalHeight.value).coerceIn(0f, 1f)
    } else {
        0f
    }

    @Suppress("MagicNumber")
    val midFraction = opaqueFraction + (1f - opaqueFraction) * 0.5f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to backgroundColor,
                        opaqueFraction to backgroundColor,
                        midFraction to backgroundColor.copy(alpha = 0.85f),
                        1f to Color.Transparent,
                    ),
                ),
            ),
    )
}

@Composable
private fun DrawerSectionTitle(title: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.regular)
            .padding(bottom = Spacing.regular),
        text = title,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun DrawerTopActions(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DrawerActionButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = LocalFeedFlowStrings.current.settingsButton,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        DrawerActionButton(onClick = onAddClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = LocalFeedFlowStrings.current.addFeed,
            )
        }
    }
}

@Composable
private fun DrawerActionButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        content = content,
    )
}
