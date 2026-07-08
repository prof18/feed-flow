package com.prof18.feedflow.android.home.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.home.AddFeedOptionsBottomSheet
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.ui.components.FeedSourceLogoImage
import com.prof18.feedflow.shared.ui.feedsuggestions.FeedSuggestionsE2eIds
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import sh.calvin.reorderable.ReorderableColumn

private val drawerActionsTopPadding = 8.dp
private val drawerActionsBottomPadding = 16.dp
private val drawerActionsListTopPadding = 80.dp
private val drawerActionsFadeHeight = 110.dp
private const val UncategorizedCategoryKey = "uncategorized"
private val expandedCategoryIdsSaver = listSaver<ImmutableList<String>, String>(
    save = { it.toList() },
    restore = { it.toImmutableList() },
)

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
    val pinnedFeedSourceItems = remember(displayState.navDrawerState.pinnedFeedSources) {
        displayState.navDrawerState.pinnedFeedSources
            .filterIsInstance<DrawerItem.DrawerFeedSource>()
            .toImmutableList()
    }

    val categorySections = remember(
        displayState.navDrawerState.feedSourcesByCategory,
        displayState.navDrawerState.feedSourcesWithoutCategory,
    ) {
        buildDrawerCategorySections(
            feedSourcesByCategory = displayState.navDrawerState.feedSourcesByCategory,
            feedSourcesWithoutCategory = displayState.navDrawerState.feedSourcesWithoutCategory,
        )
    }
    var expandedCategoryIds by rememberSaveable(stateSaver = expandedCategoryIdsSaver) {
        mutableStateOf(persistentListOf())
    }
    var isPinnedEditMode by rememberSaveable { mutableStateOf(false) }
    var isFeedSourcesEditMode by rememberSaveable { mutableStateOf(false) }

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
                    DrawerReorderableSectionHeader(
                        title = LocalFeedFlowStrings.current.drawerTitlePinnedFeeds,
                        showReorderToggle = pinnedFeedSourceItems.size > 1,
                        isEditMode = isPinnedEditMode,
                        reorderToggleTestTag = DrawerE2eIds.PINNED_REORDER_TOGGLE,
                        onToggleEditMode = { isPinnedEditMode = !isPinnedEditMode },
                    )
                }

                item {
                    AndroidDrawerPinnedFeedSources(
                        orderedPinnedFeedSources = pinnedFeedSourceItems,
                        isEditMode = isPinnedEditMode,
                        currentFeedFilter = displayState.currentFeedFilter,
                        feedManagementActions = feedManagementActions,
                        onFeedSourceClick = onFeedSourceClick,
                        onPinnedFeedSourcesReordered = { updatedItems ->
                            feedManagementActions.onReorderPinnedFeedSources(updatedItems.map { it.feedSource })
                        },
                    )
                }
            }

            if (displayState.navDrawerState.feedSourcesByCategory.isNotEmpty() ||
                displayState.navDrawerState.feedSourcesWithoutCategory.isNotEmpty()
            ) {
                item {
                    DrawerDivider()
                }

                item {
                    DrawerReorderableSectionHeader(
                        title = LocalFeedFlowStrings.current.drawerTitleFeedSources,
                        showReorderToggle = categorySections.size > 1 ||
                            categorySections.any { it.drawerFeedSources.size > 1 },
                        isEditMode = isFeedSourcesEditMode,
                        reorderToggleTestTag = DrawerE2eIds.FEED_SOURCES_REORDER_TOGGLE,
                        onToggleEditMode = { isFeedSourcesEditMode = !isFeedSourcesEditMode },
                    )
                }

                item {
                    AndroidDrawerCategorySections(
                        orderedCategorySections = categorySections,
                        isEditMode = isFeedSourcesEditMode,
                        expandedCategoryIds = expandedCategoryIds,
                        currentFeedFilter = displayState.currentFeedFilter,
                        feedManagementActions = feedManagementActions,
                        onFeedFilterSelected = onFeedFilterSelected,
                        onFeedSourceClick = onFeedSourceClick,
                        onExpandedCategoryIdsChange = { expandedCategoryIds = it },
                        onCategoriesReordered = { updatedItems ->
                            feedManagementActions.onReorderCategories(
                                updatedItems.map { it.categoryWrapper },
                            )
                        },
                        onFeedSourcesReordered = { _, updatedItems ->
                            feedManagementActions.onReorderFeedSources(updatedItems.map { it.feedSource })
                        },
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
private fun AndroidDrawerPinnedFeedSources(
    orderedPinnedFeedSources: ImmutableList<DrawerItem.DrawerFeedSource>,
    isEditMode: Boolean,
    currentFeedFilter: FeedFilter,
    feedManagementActions: FeedManagementActions,
    onFeedSourceClick: (FeedSource) -> Unit,
    onPinnedFeedSourcesReordered: (ImmutableList<DrawerItem.DrawerFeedSource>) -> Unit,
) {
    val canReorderPinnedFeedSources = isEditMode && orderedPinnedFeedSources.size > 1
    if (canReorderPinnedFeedSources) {
        ReorderableColumn(
            orderedPinnedFeedSources,
            { fromIndex, toIndex ->
                onPinnedFeedSourcesReordered(
                    orderedPinnedFeedSources.moveItem(fromIndex = fromIndex, toIndex = toIndex).toImmutableList(),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds(),
        ) { _, feedSourceWrapper, _ ->
            ReorderableItem {
                AndroidDrawerFeedSourceItem(
                    modifier = Modifier
                        .testTag(DrawerE2eIds.feedSource(feedSourceWrapper.feedSource.id)),
                    feedSourceWrapper = feedSourceWrapper,
                    currentFeedFilter = currentFeedFilter,
                    feedManagementActions = feedManagementActions,
                    onFeedSourceClick = onFeedSourceClick,
                    dragHandle = {
                        DrawerReorderDragHandle(
                            Modifier
                                .testTag(
                                    DrawerE2eIds.pinnedFeedSourceReorderHandle(feedSourceWrapper.feedSource.id),
                                )
                                .draggableHandle(),
                        )
                    },
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            orderedPinnedFeedSources.forEach { feedSourceWrapper ->
                AndroidDrawerFeedSourceItem(
                    modifier = Modifier.testTag(DrawerE2eIds.feedSource(feedSourceWrapper.feedSource.id)),
                    feedSourceWrapper = feedSourceWrapper,
                    currentFeedFilter = currentFeedFilter,
                    feedManagementActions = feedManagementActions,
                    onFeedSourceClick = onFeedSourceClick,
                )
            }
        }
    }
}

@Composable
private fun AndroidDrawerCategorySections(
    orderedCategorySections: ImmutableList<DrawerCategorySection>,
    isEditMode: Boolean,
    expandedCategoryIds: ImmutableList<String>,
    currentFeedFilter: FeedFilter,
    feedManagementActions: FeedManagementActions,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    onFeedSourceClick: (FeedSource) -> Unit,
    onExpandedCategoryIdsChange: (ImmutableList<String>) -> Unit,
    onCategoriesReordered: (ImmutableList<DrawerCategorySection>) -> Unit,
    onFeedSourcesReordered: (String, ImmutableList<DrawerItem.DrawerFeedSource>) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val canReorderCategories = isEditMode && orderedCategorySections.size > 1
        if (canReorderCategories) {
            ReorderableColumn(
                orderedCategorySections,
                { fromIndex, toIndex ->
                    onCategoriesReordered(
                        orderedCategorySections.moveItem(fromIndex = fromIndex, toIndex = toIndex).toImmutableList(),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds(),
            ) { _, categorySection, _ ->
                ReorderableItem {
                    AndroidDrawerCategorySectionContent(
                        categorySection = categorySection,
                        categoryDragHandle = {
                            DrawerReorderDragHandle(
                                Modifier
                                    .testTag(DrawerE2eIds.categoryReorderHandle(categorySection.categoryId))
                                    .draggableHandle(),
                            )
                        },
                        isEditMode = isEditMode,
                        expandedCategoryIds = expandedCategoryIds,
                        currentFeedFilter = currentFeedFilter,
                        feedManagementActions = feedManagementActions,
                        onFeedFilterSelected = onFeedFilterSelected,
                        onFeedSourceClick = onFeedSourceClick,
                        onExpandedCategoryIdsChange = onExpandedCategoryIdsChange,
                        onFeedSourcesReordered = onFeedSourcesReordered,
                    )
                }
            }
        } else {
            orderedCategorySections.forEach { categorySection ->
                AndroidDrawerCategorySectionContent(
                    categorySection = categorySection,
                    categoryDragHandle = null,
                    isEditMode = isEditMode,
                    expandedCategoryIds = expandedCategoryIds,
                    currentFeedFilter = currentFeedFilter,
                    feedManagementActions = feedManagementActions,
                    onFeedFilterSelected = onFeedFilterSelected,
                    onFeedSourceClick = onFeedSourceClick,
                    onExpandedCategoryIdsChange = onExpandedCategoryIdsChange,
                    onFeedSourcesReordered = onFeedSourcesReordered,
                )
            }
        }
    }
}

@Composable
private fun AndroidDrawerCategorySectionContent(
    categorySection: DrawerCategorySection,
    categoryDragHandle: (@Composable () -> Unit)?,
    isEditMode: Boolean,
    expandedCategoryIds: ImmutableList<String>,
    currentFeedFilter: FeedFilter,
    feedManagementActions: FeedManagementActions,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    onFeedSourceClick: (FeedSource) -> Unit,
    onExpandedCategoryIdsChange: (ImmutableList<String>) -> Unit,
    onFeedSourcesReordered: (String, ImmutableList<DrawerItem.DrawerFeedSource>) -> Unit,
) {
    val categoryId = categorySection.categoryKey
    val isCategoryExpanded = categoryId in expandedCategoryIds
    Column(modifier = Modifier.fillMaxWidth()) {
        AndroidDrawerFeedSourceByCategoryItem(
            feedSourceCategoryWrapper = categorySection.categoryWrapper,
            drawerFeedSources = categorySection.drawerFeedSources,
            currentFeedFilter = currentFeedFilter,
            drawerItemVisualStyle = DefaultDrawerItemVisualStyle,
            isCategoryExpanded = isCategoryExpanded,
            onCategoryExpand = {
                onExpandedCategoryIdsChange(
                    if (isCategoryExpanded) {
                        (expandedCategoryIds - categoryId).toImmutableList()
                    } else {
                        (expandedCategoryIds + categoryId).toImmutableList()
                    },
                )
            },
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
            onMarkAllReadForFeedSourceClick = feedManagementActions.onMarkAllReadForFeedSourceClick,
            onMarkAllReadForCategoryClick = feedManagementActions.onMarkAllReadForCategoryClick,
            onDeleteAllFeedsInCategoryByIdClick = feedManagementActions.onDeleteAllFeedsInCategoryByIdClick,
            dragHandle = categoryDragHandle,
            showFeedSources = false,
        )

        if (isCategoryExpanded) {
            AndroidDrawerCategoryFeedSources(
                categorySection = categorySection,
                isEditMode = isEditMode,
                currentFeedFilter = currentFeedFilter,
                feedManagementActions = feedManagementActions,
                onFeedSourceClick = onFeedSourceClick,
                onFeedSourcesReordered = onFeedSourcesReordered,
            )
        }
    }
}

@Composable
private fun AndroidDrawerCategoryFeedSources(
    categorySection: DrawerCategorySection,
    isEditMode: Boolean,
    currentFeedFilter: FeedFilter,
    feedManagementActions: FeedManagementActions,
    onFeedSourceClick: (FeedSource) -> Unit,
    onFeedSourcesReordered: (String, ImmutableList<DrawerItem.DrawerFeedSource>) -> Unit,
) {
    val canReorderFeedSources = isEditMode && categorySection.drawerFeedSources.size > 1
    if (canReorderFeedSources) {
        ReorderableColumn(
            categorySection.drawerFeedSources,
            { fromIndex, toIndex ->
                onFeedSourcesReordered(
                    categorySection.categoryKey,
                    categorySection.drawerFeedSources
                        .moveItem(fromIndex = fromIndex, toIndex = toIndex)
                        .toImmutableList(),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds(),
        ) { _, feedSourceWrapper, _ ->
            ReorderableItem {
                AndroidDrawerFeedSourceItem(
                    modifier = Modifier
                        .testTag(DrawerE2eIds.feedSource(feedSourceWrapper.feedSource.id)),
                    feedSourceWrapper = feedSourceWrapper,
                    currentFeedFilter = currentFeedFilter,
                    feedManagementActions = feedManagementActions,
                    onFeedSourceClick = onFeedSourceClick,
                    dragHandle = {
                        DrawerReorderDragHandle(
                            Modifier
                                .testTag(DrawerE2eIds.feedSourceReorderHandle(feedSourceWrapper.feedSource.id))
                                .draggableHandle(),
                        )
                    },
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            categorySection.drawerFeedSources.forEach { feedSourceWrapper ->
                AndroidDrawerFeedSourceItem(
                    modifier = Modifier.testTag(DrawerE2eIds.feedSource(feedSourceWrapper.feedSource.id)),
                    feedSourceWrapper = feedSourceWrapper,
                    currentFeedFilter = currentFeedFilter,
                    feedManagementActions = feedManagementActions,
                    onFeedSourceClick = onFeedSourceClick,
                )
            }
        }
    }
}

@Composable
private fun AndroidDrawerFeedSourceItem(
    feedSourceWrapper: DrawerItem.DrawerFeedSource,
    currentFeedFilter: FeedFilter,
    feedManagementActions: FeedManagementActions,
    onFeedSourceClick: (FeedSource) -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    AndroidFeedSourceDrawerItem(
        modifier = modifier,
        dragHandle = dragHandle,
        label = {
            Text(
                text = feedSourceWrapper.feedSource.title,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        },
        selected = currentFeedFilter is FeedFilter.Source &&
            currentFeedFilter.feedSource == feedSourceWrapper.feedSource,
        drawerItemVisualStyle = DefaultDrawerItemVisualStyle,
        onClick = { onFeedSourceClick(feedSourceWrapper.feedSource) },
        icon = {
            DrawerFeedSourceIcon(feedSourceWrapper.feedSource)
        },
        onEditFeedClick = feedManagementActions.onEditFeedClick,
        onDeleteFeedSourceClick = feedManagementActions.onDeleteFeedSourceClick,
        onPinFeedClick = feedManagementActions.onPinFeedClick,
        onChangeFeedCategoryClick = feedManagementActions.onChangeFeedCategoryClick,
        onOpenWebsite = feedManagementActions.onOpenWebsite,
        onMarkAllReadForFeedSourceClick = feedManagementActions.onMarkAllReadForFeedSourceClick,
        feedSource = feedSourceWrapper.feedSource,
        unreadCount = feedSourceWrapper.unreadCount,
    )
}

@Composable
private fun DrawerFeedSourceIcon(feedSource: FeedSource) {
    val imageUrl = feedSource.logoUrl
    if (imageUrl != null) {
        FeedSourceLogoImage(
            size = 24.dp,
            imageUrl = imageUrl,
        )
    } else {
        Icon(
            imageVector = Icons.Default.Category,
            contentDescription = null,
        )
    }
}

private data class DrawerCategorySection(
    val categoryWrapper: DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper,
    val drawerFeedSources: ImmutableList<DrawerItem.DrawerFeedSource>,
) {
    val categoryId: String?
        get() = categoryWrapper.feedSourceCategory?.id

    val categoryKey: String
        get() = categoryId ?: UncategorizedCategoryKey
}

private fun buildDrawerCategorySections(
    feedSourcesByCategory: Map<DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper, List<DrawerItem>>,
    feedSourcesWithoutCategory: List<DrawerItem>,
): ImmutableList<DrawerCategorySection> {
    val uncategorizedFeedSources = feedSourcesWithoutCategory
        .filterIsInstance<DrawerItem.DrawerFeedSource>()
        .toImmutableList()
    val uncategorizedSection = if (uncategorizedFeedSources.isNotEmpty()) {
        DrawerCategorySection(
            categoryWrapper = DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper(
                feedSourceCategory = null,
            ),
            drawerFeedSources = uncategorizedFeedSources,
        )
    } else {
        null
    }
    val categorizedSections = feedSourcesByCategory.map { (categoryWrapper, drawerFeedSources) ->
        DrawerCategorySection(
            categoryWrapper = categoryWrapper,
            drawerFeedSources = drawerFeedSources
                .filterIsInstance<DrawerItem.DrawerFeedSource>()
                .toImmutableList(),
        )
    }
    return listOfNotNull(uncategorizedSection)
        .plus(categorizedSections)
        .toImmutableList()
}

private fun <T> List<T>.moveItem(
    fromIndex: Int,
    toIndex: Int,
): List<T> {
    if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) {
        return this
    }

    return toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
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
private fun DrawerReorderableSectionHeader(
    title: String,
    showReorderToggle: Boolean,
    isEditMode: Boolean,
    reorderToggleTestTag: String,
    onToggleEditMode: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.regular),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.regular),
            text = title,
            style = MaterialTheme.typography.labelLarge,
        )

        if (showReorderToggle) {
            IconButton(
                modifier = Modifier.testTag(reorderToggleTestTag),
                onClick = onToggleEditMode,
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Outlined.Check else Icons.AutoMirrored.Rounded.Sort,
                    contentDescription = if (isEditMode) {
                        LocalFeedFlowStrings.current.reorderModeDone
                    } else {
                        LocalFeedFlowStrings.current.reorderModeEnter
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DrawerReorderDragHandle(modifier: Modifier = Modifier) {
    IconButton(
        modifier = modifier,
        onClick = {},
    ) {
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = LocalFeedFlowStrings.current.reorderDragHandle,
        )
    }
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
        DrawerActionButton(
            modifier = Modifier.testTag(DrawerE2eIds.SETTINGS_BUTTON),
            onClick = onSettingsClick,
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = LocalFeedFlowStrings.current.settingsButton,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        DrawerActionButton(
            modifier = Modifier.testTag(FeedSuggestionsE2eIds.ADD_OPTIONS_BUTTON),
            onClick = onAddClick,
        ) {
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
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val containerColor = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    FilledIconButton(
        modifier = modifier,
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        content = content,
    )
}
