package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList

private const val PinnedFeedSourceKeyPrefix = "desktop-pinned-feed-source:"
private const val DrawerCategoryKeyPrefix = "desktop-drawer-category:"
private const val DrawerFeedSourceKeyPrefix = "desktop-drawer-feed-source:"

@Composable
@Suppress("CyclomaticComplexMethod")
fun DesktopDrawer(
    displayState: HomeDisplayState,
    feedManagementActions: FeedManagementActions,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    modifier: Modifier = Modifier,
    onFeedSuggestionsClick: () -> Unit = {},
    onImportExportClick: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val dragState = rememberDesktopDrawerDragState(listState)
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

    val pinnedFeedSourceItems = remember(displayState.navDrawerState.pinnedFeedSources) {
        displayState.navDrawerState.pinnedFeedSources
            .filterIsInstance<DrawerItem.DrawerFeedSource>()
            .toImmutableList()
    }

    val categorySections = remember(
        displayState.navDrawerState.feedSourcesByCategory,
        displayState.navDrawerState.feedSourcesWithoutCategory,
    ) {
        buildDesktopDrawerCategorySections(
            feedSourcesByCategory = displayState.navDrawerState.feedSourcesByCategory,
            feedSourcesWithoutCategory = displayState.navDrawerState.feedSourcesWithoutCategory,
        )
    }
    var expandedCategoryIds by rememberSaveable { mutableStateOf(emptyList<String>()) }

    val onReorderPinnedFeedSource: (Int, FeedSource) -> Unit = { insertionIndex, feedSource ->
        val fromIndex = pinnedFeedSourceItems.indexOfFirst { it.feedSource.id == feedSource.id }
        if (fromIndex != -1) {
            val updatedItems = pinnedFeedSourceItems
                .moveItemToIndex(fromIndex = fromIndex, toInsertionIndex = insertionIndex)
                .toImmutableList()
            feedManagementActions.onReorderPinnedFeedSources(updatedItems.map { it.feedSource })
        }
    }

    val onReorderFeedSourceInSection: (String, Int, FeedSource) -> Unit =
        reorderFeedSource@{ sectionKey, insertionIndex, feedSource ->
            if (sectionKey == DesktopDrawerPinnedSectionKey) {
                onReorderPinnedFeedSource(insertionIndex, feedSource)
            } else {
                val section = categorySections.firstOrNull { it.categoryKey == sectionKey }
                    ?: return@reorderFeedSource
                val fromIndex = section.drawerFeedSources.indexOfFirst { it.feedSource.id == feedSource.id }
                if (fromIndex != -1) {
                    val updatedFeedSources = section.drawerFeedSources
                        .moveItemToIndex(fromIndex = fromIndex, toInsertionIndex = insertionIndex)
                        .toImmutableList()
                    feedManagementActions.onReorderFeedSources(updatedFeedSources.map { it.feedSource })
                }
            }
        }

    val onReorderCategory: (String, Int) -> Unit = { sectionKey, insertionIndex ->
        val fromIndex = categorySections.indexOfFirst { it.categoryKey == sectionKey }
        if (fromIndex != -1) {
            val updatedItems = categorySections
                .moveItemToIndex(fromIndex = fromIndex, toInsertionIndex = insertionIndex)
                .toImmutableList()
            feedManagementActions.onReorderCategories(
                updatedItems.map { it.categoryWrapper },
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.regular)
            .onGloballyPositioned { drawerCoordinates = it },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { dragState.updateListBounds(it) },
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                val timelineItem = remember(displayState.navDrawerState.timeline) {
                    displayState.navDrawerState.timeline
                        .filterIsInstance<DrawerItem.Timeline>()
                        .firstOrNull()
                        ?: DrawerItem.Timeline(unreadCount = 0)
                }

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
                val bookmarksItem = remember(displayState.navDrawerState.bookmarks) {
                    displayState.navDrawerState.bookmarks
                        .filterIsInstance<DrawerItem.Bookmarks>()
                        .firstOrNull()
                        ?: DrawerItem.Bookmarks(unreadCount = 0)
                }
                DrawerBookmarksItem(
                    currentFeedFilter = displayState.currentFeedFilter,
                    onFeedFilterSelected = onFeedFilterSelectedWithClear,
                    drawerItem = bookmarksItem,
                    drawerItemVisualStyle = desktopDrawerItemVisualStyle(),
                )
            }

            if (displayState.navDrawerState.pinnedFeedSources.isNotEmpty()) {
                val canReorderPinnedFeedSources = pinnedFeedSourceItems.size > 1

                item {
                    DrawerDivider()
                }

                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = Spacing.regular)
                            .padding(bottom = Spacing.regular),
                        text = LocalFeedFlowStrings.current.drawerTitlePinnedFeeds,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                itemsIndexed(
                    items = pinnedFeedSourceItems,
                    key = { _, item -> pinnedFeedSourceKey(item.feedSource.id) },
                ) { index, feedSourceWrapper ->
                    DesktopDrawerFeedSourcesList(
                        drawerFeedSources = persistentListOf(feedSourceWrapper),
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
                        onMarkAllReadForFeedSourceClick = feedManagementActions.onMarkAllReadForFeedSourceClick,
                        onMoveFeedSourcesToCategory = onMoveFeedSourcesToCategory,
                        onReorderFeedSource = onReorderFeedSourceInSection,
                        dragState = dragState,
                        sectionKey = DesktopDrawerPinnedSectionKey,
                        indexInSection = index,
                        sectionSize = pinnedFeedSourceItems.size,
                        sectionCategory = null,
                        isCategoryDropTarget = false,
                        reorderEnabled = canReorderPinnedFeedSources,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            item {
                DesktopDrawerFeedSourcesByCategories(
                    navDrawerState = displayState.navDrawerState,
                    onDeleteAllFeedsInCategoryClick = feedManagementActions.onDeleteAllFeedsInCategoryClick,
                    onAddFeedClick = feedManagementActions.onAddFeedClick,
                    onFeedSuggestionsClick = onFeedSuggestionsClick,
                    onImportExportClick = onImportExportClick,
                )
            }

            val canReorderCategories = categorySections.size > 1
            categorySections.forEachIndexed { categoryIndex, categorySection ->
                val category = categorySection.categoryWrapper.feedSourceCategory
                val canReorderCategory = canReorderCategories
                val itemKey = categorySectionKey(categorySection)
                val categoryId = categorySection.categoryKey
                val isCategoryExpanded = categoryId in expandedCategoryIds
                val isLastCategory = categoryIndex == categorySections.lastIndex

                item(key = itemKey) {
                    DesktopDrawerFeedSourceByCategoryItem(
                        modifier = Modifier.animateItem(),
                        headerModifier = Modifier
                            .categoryHeaderReorderSlot(
                                dragState = dragState,
                                sectionKey = categoryId,
                                index = categoryIndex,
                            )
                            .categoryDragSource(
                                dragState = dragState,
                                categoryKey = categoryId,
                                categoryTitle = category?.title ?: LocalFeedFlowStrings.current.noCategory,
                                index = categoryIndex,
                                enabled = canReorderCategory,
                                onDropReorder = { insertionIndex ->
                                    onReorderCategory(categoryId, insertionIndex)
                                },
                            )
                            .categoryReorderInsertionIndicator(
                                dragState = dragState,
                                index = categoryIndex,
                                isLast = isLastCategory,
                            ),
                        feedSourceCategoryWrapper = categorySection.categoryWrapper,
                        drawerFeedSources = categorySection.drawerFeedSources,
                        currentFeedFilter = displayState.currentFeedFilter,
                        drawerItemVisualStyle = desktopDrawerItemVisualStyle(),
                        isCategoryExpanded = isCategoryExpanded,
                        onCategoryExpand = {
                            expandedCategoryIds = if (isCategoryExpanded) {
                                expandedCategoryIds - categoryId
                            } else {
                                expandedCategoryIds + categoryId
                            }
                        },
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
                        validateCategoryName = feedManagementActions.validateCategoryName,
                        onDeleteCategoryClick = feedManagementActions.onDeleteCategoryClick,
                        onDeleteAllFeedsInCategoryClick = feedManagementActions.onDeleteAllFeedsInCategoryClick,
                        onMarkAllReadForFeedSourceClick = feedManagementActions.onMarkAllReadForFeedSourceClick,
                        onMarkAllReadForCategoryClick = feedManagementActions.onMarkAllReadForCategoryClick,
                        onMoveFeedSourcesToCategory = onMoveFeedSourcesToCategory,
                        dragState = dragState,
                        showFeedSources = false,
                    )
                }

                if (categoryId in expandedCategoryIds) {
                    val canReorderFeedSources = categorySection.drawerFeedSources.size > 1
                    itemsIndexed(
                        items = categorySection.drawerFeedSources,
                        key = { _, item -> categoryFeedSourceKey(categoryId, item.feedSource.id) },
                    ) { index, feedSourceWrapper ->
                        DesktopDrawerFeedSourcesList(
                            drawerFeedSources = persistentListOf(feedSourceWrapper),
                            currentFeedFilter = displayState.currentFeedFilter,
                            drawerItemVisualStyle = desktopDrawerItemVisualStyle(),
                            selectedFeedSourceIds = selectedFeedSourceIds,
                            onFeedSourceClick = onFeedSourceClick,
                            selectedFeedSourcesProvider = {
                                selectedFeedSourceIds.mapNotNull { allFeedSources[it] }
                            },
                            onEditFeedClick = feedManagementActions.onEditFeedClick,
                            onDeleteFeedSourceClick = feedManagementActions.onDeleteFeedSourceClick,
                            onPinFeedClick = feedManagementActions.onPinFeedClick,
                            onChangeFeedCategoryClick = feedManagementActions.onChangeFeedCategoryClick,
                            onOpenWebsite = feedManagementActions.onOpenWebsite,
                            onMarkAllReadForFeedSourceClick = feedManagementActions.onMarkAllReadForFeedSourceClick,
                            onMoveFeedSourcesToCategory = onMoveFeedSourcesToCategory,
                            onReorderFeedSource = onReorderFeedSourceInSection,
                            dragState = dragState,
                            sectionKey = categoryId,
                            indexInSection = index,
                            sectionSize = categorySection.drawerFeedSources.size,
                            sectionCategory = category,
                            isCategoryDropTarget = true,
                            reorderEnabled = canReorderFeedSources,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }

        DragGhost(
            dragState = dragState,
            drawerCoordinates = drawerCoordinates,
        )
    }
}

private data class DesktopDrawerCategorySection(
    val categoryWrapper: DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper,
    val drawerFeedSources: ImmutableList<DrawerItem.DrawerFeedSource>,
) {
    val categoryId: String?
        get() = categoryWrapper.feedSourceCategory?.id

    val categoryKey: String
        get() = categoryId ?: DesktopDrawerUncategorizedSectionKey
}

private fun buildDesktopDrawerCategorySections(
    feedSourcesByCategory: Map<DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper, List<DrawerItem>>,
    feedSourcesWithoutCategory: List<DrawerItem>,
): ImmutableList<DesktopDrawerCategorySection> {
    val uncategorizedFeedSources = feedSourcesWithoutCategory
        .filterIsInstance<DrawerItem.DrawerFeedSource>()
        .toImmutableList()
    val uncategorizedSection = if (uncategorizedFeedSources.isNotEmpty()) {
        DesktopDrawerCategorySection(
            categoryWrapper = DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper(
                feedSourceCategory = null,
            ),
            drawerFeedSources = uncategorizedFeedSources,
        )
    } else {
        null
    }
    val categorizedSections = feedSourcesByCategory.map { (categoryWrapper, drawerFeedSources) ->
        DesktopDrawerCategorySection(
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

private fun pinnedFeedSourceKey(feedSourceId: String): String =
    "$PinnedFeedSourceKeyPrefix$feedSourceId"

private fun categorySectionKey(section: DesktopDrawerCategorySection): String =
    "$DrawerCategoryKeyPrefix${section.categoryKey}"

private fun categoryFeedSourceKey(categoryId: String, feedSourceId: String): String =
    "$DrawerFeedSourceKeyPrefix$categoryId:$feedSourceId"

private fun <T> List<T>.moveItemToIndex(
    fromIndex: Int,
    toInsertionIndex: Int,
): List<T> {
    if (fromIndex !in indices || toInsertionIndex !in 0..size) {
        return this
    }

    return toMutableList().apply {
        val item = removeAt(fromIndex)
        val adjustedIndex = if (fromIndex < toInsertionIndex) {
            toInsertionIndex - 1
        } else {
            toInsertionIndex
        }
        add(adjustedIndex, item)
    }
}
