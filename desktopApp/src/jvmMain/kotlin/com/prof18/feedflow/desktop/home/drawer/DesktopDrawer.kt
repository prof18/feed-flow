package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.desktop.home.DesktopFeedSelectionState
import com.prof18.feedflow.desktop.home.projectDesktopFeedTree
import com.prof18.feedflow.desktop.home.rememberDesktopFeedTreeState
import com.prof18.feedflow.desktop.home.reorderVisibleFeeds
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.LocalReduceMotion
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.components.DesktopPopupMenuEntry
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet

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
    contentPadding: PaddingValues = PaddingValues(DesktopDesignTokens.spaceSection),
) {
    val reduceMotion = LocalReduceMotion.current
    val listState = rememberLazyListState()
    val dragState = rememberDesktopDrawerDragState(listState)
    var drawerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var selection by remember { mutableStateOf(DesktopFeedSelectionState()) }
    val selectedFeedSourceIds = selection.selectedIds.toPersistentSet()
    val tree = remember(displayState.navDrawerState) { projectDesktopFeedTree(displayState.navDrawerState) }
    val treeState = rememberDesktopFeedTreeState()
    val scope = rememberCoroutineScope()
    val keyboard = remember(listState, scope) { DesktopFeedTreeKeyboard(listState, scope) }
    val categoryTransitions = tree.sections.associate { section ->
        section.id to key(section.id) {
            updateTransition(targetState = treeState.isExpanded(section.id), label = "Sidebar category expansion")
        }
    }
    val retainedCollapsedSections = categoryTransitions.filter { (id, transition) ->
        !reduceMotion && !treeState.isExpanded(id) && transition.currentState
    }.keys
    val onMarkAllReadForFeedSource = feedManagementActions.onMarkAllReadForFeedSourceClick
    val expansionRunning = categoryTransitions.values.any { it.isRunning || it.currentState != it.targetState }
    // Collapsed rows leave the list in the same frame the expansion ends. Re-enabling placement
    // animation in that frame makes the rows below fly in from the viewport edge, so wait until
    // the list has laid out without them.
    var placementAnimationEnabled by remember { mutableStateOf(true) }
    LaunchedEffect(expansionRunning) {
        if (expansionRunning) {
            placementAnimationEnabled = false
        } else {
            repeat(PLACEMENT_SETTLE_FRAMES) { withFrameNanos { } }
            placementAnimationEnabled = true
        }
    }
    val animatePlacement = !reduceMotion && placementAnimationEnabled && !expansionRunning
    val keyboardRows = desktopTreeKeyboardRows(tree, treeState, retainedCollapsedSections)
    val allFeedSources = remember(tree) {
        (tree.pinned + tree.sections.flatMap { it.members }).associate { it.feedSource.id to it.feedSource }
    }
    val latestFeedSources by rememberUpdatedState(allFeedSources)
    SideEffect { dragState.updateFeedSources(allFeedSources) }
    val visibleIds = (tree.pinned + tree.sections.filter { treeState.isExpanded(it.id) }.flatMap { it.visibleFeeds })
        .map { it.feedSource.id }
    val selectedSources = allFeedSources.values.filter { it.id in selectedFeedSourceIds }
    var moveTargetIds by remember { mutableStateOf<Set<String>?>(null) }
    var deleteTargetIds by remember { mutableStateOf<Set<String>?>(null) }

    // Moving a single feed uses the same destination dialog as moving a selection.
    val changeFeedCategory: (FeedSource) -> Unit = { source -> moveTargetIds = setOf(source.id) }
    val onFeedFilterSelectedWithClear: (FeedFilter) -> Unit = { filter ->
        selection = DesktopFeedSelectionState()
        onFeedFilterSelected(filter)
    }

    val onFeedSourceClick: (FeedSource, Boolean, Boolean) -> Unit = { feedSource, toggle, range ->
        selection = selection.select(
            id = feedSource.id,
            visibleIds = visibleIds,
            toggle = toggle,
            range = range,
            activeId = (displayState.currentFeedFilter as? FeedFilter.Source)?.feedSource?.id,
        )
        if (!toggle && !range) {
            onFeedFilterSelected(FeedFilter.Source(feedSource))
        }
    }

    val onMoveFeedSourcesToCategory: (List<FeedSource>, FeedSourceCategory?) -> Unit =
        { feedSources, category ->
            val currentSources = feedSources.mapNotNull { latestFeedSources[it.id] }
                .distinctBy { it.id }.filter { it.category?.id != category?.id }
            if (currentSources.isNotEmpty()) {
                feedManagementActions.onMoveFeedSourcesToCategory(currentSources, category)
            }
            selection = DesktopFeedSelectionState()
        }

    val pinnedFeedSourceItems = tree.pinned.toImmutableList()
    val categorySections = tree.sections.map { section ->
        DesktopDrawerCategorySection(
            categoryWrapper = section.categoryWrapper,
            drawerFeedSources = section.visibleFeeds.toImmutableList(),
            members = section.members.toImmutableList(),
            stateKey = section.id,
        )
    }
    SideEffect(tree) {
        treeState.reconcile(tree)
        selection = selection.reconcile(tree.sourceIds)
    }
    val strings = LocalFeedFlowStrings.current
    val batchMenuEntries = if (selectedSources.size > 1) {
        persistentListOf(
            DesktopPopupMenuEntry.Action(
                strings.desktopMoveSelectedFeeds,
                onClick = { moveTargetIds = selectedFeedSourceIds },
            ),
            DesktopPopupMenuEntry.Action(
                strings.desktopDeleteSelectedFeeds,
                onClick = { deleteTargetIds = selectedFeedSourceIds },
            ),
        )
    } else {
        null
    }
    val onContextTarget: (FeedSource) -> Unit = { selection = selection.contextTarget(it.id) }

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
                    val sectionProjection = tree.sections.first { it.id == section.stateKey }
                    feedManagementActions.onReorderFeedSources(
                        sectionProjection.reorderVisibleFeeds(fromIndex, insertionIndex).map { it.feedSource },
                    )
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
            .padding(contentPadding)
            .onGloballyPositioned { drawerCoordinates = it }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape && dragState.dragPayload != null) {
                    dragState.clear()
                    true
                } else {
                    false
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            if (selectedSources.isNotEmpty()) {
                DesktopFeedManagementToolbar(
                    modifier = Modifier.fillMaxWidth(),
                    selectedCount = selectedSources.size,
                    onClear = { selection = DesktopFeedSelectionState() },
                    onMove = { moveTargetIds = selectedFeedSourceIds },
                    onDelete = { deleteTargetIds = selectedFeedSourceIds },
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { dragState.updateListBounds(it) }
                    .onPreviewKeyEvent { event ->
                        keyboard.handle(event, keyboardRows, treeState) { id, toggle ->
                            latestFeedSources[id]?.let { onFeedSourceClick(it, toggle, true) }
                        }
                    },
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
                        modifier = desktopTreeRowFocusModifier("timeline", keyboard),
                    )
                }

                item {
                    DrawerReadItem(
                        currentFeedFilter = displayState.currentFeedFilter,
                        onFeedFilterSelected = onFeedFilterSelectedWithClear,
                        drawerItemVisualStyle = desktopDrawerItemVisualStyle(),
                        modifier = desktopTreeRowFocusModifier("read", keyboard),
                    )
                }

                item {
                    DrawerBookmarksItem(
                        currentFeedFilter = displayState.currentFeedFilter,
                        onFeedFilterSelected = onFeedFilterSelectedWithClear,
                        drawerItemVisualStyle = desktopDrawerItemVisualStyle(),
                        modifier = desktopTreeRowFocusModifier("bookmarks", keyboard),
                    )
                }

                item {
                    DesktopDrawerFeedSourcesByCategories(
                        navDrawerState = displayState.navDrawerState,
                        onDeleteAllFeedsInCategoryClick = feedManagementActions.onDeleteAllFeedsInCategoryClick,
                        onAddFeedClick = feedManagementActions.onAddFeedClick,
                        onFeedSuggestionsClick = feedManagementActions.onFeedSuggestionsClick,
                        onImportFeedsClick = feedManagementActions.onImportFeedsClick,
                        onExpandAll = { tree.sections.forEach { treeState.setExpanded(it.id, true) } },
                        onCollapseAll = { tree.sections.forEach { treeState.setExpanded(it.id, false) } },
                    )
                }
                if (displayState.navDrawerState.pinnedFeedSources.isNotEmpty()) {
                    val canReorderPinnedFeedSources = pinnedFeedSourceItems.size > 1

                    item {
                        DrawerDivider()
                    }

                    item {
                        SidebarSectionLabel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, bottom = 4.dp),
                            text = LocalFeedFlowStrings.current.drawerTitlePinnedFeeds,
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
                            onContextTarget = onContextTarget,
                            batchMenuEntries = batchMenuEntries,
                            onEditFeedClick = feedManagementActions.onEditFeedClick,
                            onDeleteFeedSourceClick = feedManagementActions.onDeleteFeedSourceClick,
                            onPinFeedClick = feedManagementActions.onPinFeedClick,
                            onChangeFeedCategoryClick = changeFeedCategory,
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
                            modifier = desktopTreeRowFocusModifier("feed:${feedSourceWrapper.feedSource.id}", keyboard)
                                .then(sidebarPlacementModifier(enabled = animatePlacement)),
                        )
                    }
                }
                val canReorderCategories = categorySections.size > 1
                categorySections.forEachIndexed { categoryIndex, categorySection ->
                    val category = categorySection.categoryWrapper.feedSourceCategory
                    val canReorderCategory = canReorderCategories
                    val itemKey = categorySectionKey(categorySection)
                    val categoryId = categorySection.categoryKey
                    val isCategoryExpanded = treeState.isExpanded(categorySection.stateKey)
                    val expansion = categoryTransitions.getValue(categorySection.stateKey)
                    val isLastCategory = categoryIndex == categorySections.lastIndex

                    item(key = itemKey) {
                        DesktopDrawerFeedSourceByCategoryItem(
                            modifier = sidebarPlacementModifier(enabled = animatePlacement),
                            topSpacing = if (categoryIndex > 0) DesktopDesignTokens.spaceMedium else 0.dp,
                            headerModifier = desktopTreeRowFocusModifier(categorySection.stateKey, keyboard)
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
                            disclosureModifier = Modifier.onFocusChanged {
                                if (it.isFocused) keyboard.focused(categorySection.stateKey)
                            },
                            feedSourceCategoryWrapper = categorySection.categoryWrapper,
                            drawerFeedSources = categorySection.members,
                            currentFeedFilter = displayState.currentFeedFilter,
                            drawerItemVisualStyle = desktopDrawerItemVisualStyle(),
                            isCategoryExpanded = isCategoryExpanded,
                            onCategoryExpand = {
                                treeState.setExpanded(categorySection.stateKey, !isCategoryExpanded)
                            },
                            onFeedFilterSelected = onFeedFilterSelectedWithClear,
                            selectedFeedSourceIds = selectedFeedSourceIds,
                            onFeedSourceClick = onFeedSourceClick,
                            selectedFeedSourcesProvider = { selectedFeedSourceIds.mapNotNull { allFeedSources[it] } },
                            onEditFeedClick = feedManagementActions.onEditFeedClick,
                            onDeleteFeedSourceClick = feedManagementActions.onDeleteFeedSourceClick,
                            onPinFeedClick = feedManagementActions.onPinFeedClick,
                            onChangeFeedCategoryClick = changeFeedCategory,
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

                    if (isCategoryExpanded || categorySection.stateKey in retainedCollapsedSections) {
                        val canReorderFeedSources = categorySection.drawerFeedSources.size > 1
                        itemsIndexed(
                            items = categorySection.drawerFeedSources,
                            key = { _, item -> categoryFeedSourceKey(categoryId, item.feedSource.id) },
                        ) { index, feedSourceWrapper ->
                            expansion.AnimatedVisibility(
                                visible = { it },
                                modifier = sidebarPlacementModifier(enabled = animatePlacement),
                                enter = if (reduceMotion) EnterTransition.None else SidebarExpansionEnter,
                                exit = if (reduceMotion) ExitTransition.None else SidebarExpansionExit,
                            ) {
                                DesktopDrawerFeedSourcesList(
                                    drawerFeedSources = persistentListOf(feedSourceWrapper),
                                    currentFeedFilter = displayState.currentFeedFilter,
                                    drawerItemVisualStyle = desktopDrawerItemVisualStyle(),
                                    selectedFeedSourceIds = selectedFeedSourceIds,
                                    onFeedSourceClick = onFeedSourceClick,
                                    selectedFeedSourcesProvider = {
                                        selectedFeedSourceIds.mapNotNull { allFeedSources[it] }
                                    },
                                    onContextTarget = onContextTarget,
                                    batchMenuEntries = batchMenuEntries,
                                    onEditFeedClick = feedManagementActions.onEditFeedClick,
                                    onDeleteFeedSourceClick = feedManagementActions.onDeleteFeedSourceClick,
                                    onPinFeedClick = feedManagementActions.onPinFeedClick,
                                    onChangeFeedCategoryClick = changeFeedCategory,
                                    onOpenWebsite = feedManagementActions.onOpenWebsite,
                                    onMarkAllReadForFeedSourceClick = onMarkAllReadForFeedSource,
                                    onMoveFeedSourcesToCategory = onMoveFeedSourcesToCategory,
                                    onReorderFeedSource = onReorderFeedSourceInSection,
                                    dragState = dragState,
                                    sectionKey = categoryId,
                                    indexInSection = index,
                                    sectionSize = categorySection.drawerFeedSources.size,
                                    sectionCategory = category,
                                    isCategoryDropTarget = true,
                                    reorderEnabled = canReorderFeedSources,
                                    modifier = desktopTreeRowFocusModifier(
                                        "feed:${feedSourceWrapper.feedSource.id}",
                                        keyboard,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }

        DragGhost(
            dragState = dragState,
            drawerCoordinates = drawerCoordinates,
        )
    }
    moveTargetIds?.let { targetIds ->
        val targets = targetIds.mapNotNull { allFeedSources[it] }
        DesktopFeedMoveDialog(
            categories = tree.sections.mapNotNull { it.categoryWrapper.feedSourceCategory }.toImmutableList(),
            feedSources = targets.toImmutableList(),
            onMove = { sources, category ->
                onMoveFeedSourcesToCategory(sources, category)
                moveTargetIds = null
            },
            onDismiss = { moveTargetIds = null },
        )
    }
    deleteTargetIds?.let { targetIds ->
        val targets = targetIds.mapNotNull { allFeedSources[it] }
        com.prof18.feedflow.desktop.ui.components.ConfirmationDialog(
            title = strings.desktopDeleteSelectedFeedsTitle,
            message = strings.desktopDeleteSelectedFeedsMessage(targets.size),
            confirmLabel = strings.deleteFeed,
            onConfirm = {
                feedManagementActions.onDeleteAllFeedsInCategoryClick(targets)
                selection = DesktopFeedSelectionState()
            },
            onDismiss = { deleteTargetIds = null },
        )
    }
}

private data class DesktopDrawerCategorySection(
    val categoryWrapper: DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper,
    val drawerFeedSources: ImmutableList<DrawerItem.DrawerFeedSource>,
    val members: ImmutableList<DrawerItem.DrawerFeedSource>,
    val stateKey: String,
) {
    val categoryId: String?
        get() = categoryWrapper.feedSourceCategory?.id

    val categoryKey: String
        get() = categoryId ?: DesktopDrawerUncategorizedSectionKey
}

private const val PLACEMENT_SETTLE_FRAMES = 2

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
