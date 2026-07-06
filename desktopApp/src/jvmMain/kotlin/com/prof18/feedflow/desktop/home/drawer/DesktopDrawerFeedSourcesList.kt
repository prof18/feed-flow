package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.shared.ui.components.FeedSourceLogoImage
import com.prof18.feedflow.shared.ui.utils.ConditionalAnimatedVisibility
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun DesktopDrawerFeedSourcesList(
    drawerFeedSources: ImmutableList<DrawerItem.DrawerFeedSource>,
    currentFeedFilter: FeedFilter,
    drawerItemVisualStyle: DrawerItemVisualStyle,
    selectedFeedSourceIds: ImmutableSet<String>,
    onFeedSourceClick: (FeedSource, Boolean) -> Unit,
    selectedFeedSourcesProvider: () -> List<FeedSource>,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onChangeFeedCategoryClick: (FeedSource) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onMarkAllReadForFeedSourceClick: (FeedSource) -> Unit,
    onMoveFeedSourcesToCategory: (List<FeedSource>, FeedSourceCategory?) -> Unit,
    onReorderFeedSource: (String, Int, FeedSource) -> Unit,
    dragState: DesktopDrawerDragState,
    sectionKey: String,
    indexInSection: Int,
    sectionSize: Int,
    sectionCategory: FeedSourceCategory?,
    isCategoryDropTarget: Boolean,
    reorderEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        drawerFeedSources.forEachIndexed { rowIndex, feedSourceWrapper ->
            val modifiers = LocalWindowInfo.current.keyboardModifiers
            val isMultiSelectPressed = modifiers.isCtrlPressed || modifiers.isMetaPressed
            val isMultiSelected = selectedFeedSourceIds.contains(feedSourceWrapper.feedSource.id)
            val rowIndexInSection = indexInSection + rowIndex
            val slotKey = "$sectionKey:${feedSourceWrapper.feedSource.id}"

            ReorderSlotCleanup(
                dragState = dragState,
                slotKey = slotKey,
            )

            DesktopFeedSourceDrawerItem(
                label = {
                    Text(
                        text = feedSourceWrapper.feedSource.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                selected = currentFeedFilter is FeedFilter.Source &&
                    currentFeedFilter.feedSource == feedSourceWrapper.feedSource,
                drawerItemVisualStyle = drawerItemVisualStyle,
                onClick = {
                    onFeedSourceClick(feedSourceWrapper.feedSource, isMultiSelectPressed)
                },
                icon = {
                    val imageUrl = feedSourceWrapper.feedSource.logoUrl
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
                },
                onEditFeedClick = onEditFeedClick,
                onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                onPinFeedClick = onPinFeedClick,
                onChangeFeedCategoryClick = onChangeFeedCategoryClick,
                onOpenWebsite = onOpenWebsite,
                onMarkAllReadForFeedSourceClick = onMarkAllReadForFeedSourceClick,
                feedSource = feedSourceWrapper.feedSource,
                unreadCount = feedSourceWrapper.unreadCount,
                isMultiSelected = isMultiSelected,
                modifier = Modifier
                    .reorderSlot(
                        dragState = dragState,
                        slotKey = slotKey,
                        sectionKey = sectionKey,
                        index = rowIndexInSection,
                        category = sectionCategory,
                        isCategoryDropTarget = isCategoryDropTarget,
                        reorderEnabled = reorderEnabled,
                    )
                    .feedSourceReorderInsertionIndicator(
                        dragState = dragState,
                        sectionKey = sectionKey,
                        index = rowIndexInSection,
                        isLast = rowIndexInSection == sectionSize - 1,
                    )
                    .feedSourceDragSource(
                        dragState = dragState,
                        feedSource = feedSourceWrapper.feedSource,
                        sectionKey = sectionKey,
                        indexInSection = rowIndexInSection,
                        selectedFeedSources = selectedFeedSourcesProvider,
                        onMoveFeedSourcesToCategory = onMoveFeedSourcesToCategory,
                        onReorderFeedSource = onReorderFeedSource,
                    ),
            )
        }
    }
}

@Composable
internal fun ColumnScope.FeedSourcesListWithCategorySelector(
    isCategoryExpanded: Boolean,
    drawerFeedSources: ImmutableList<DrawerItem.DrawerFeedSource>,
    currentFeedFilter: FeedFilter,
    drawerItemVisualStyle: DrawerItemVisualStyle,
    selectedFeedSourceIds: ImmutableSet<String>,
    onFeedSourceClick: (FeedSource, Boolean) -> Unit,
    selectedFeedSourcesProvider: () -> List<FeedSource>,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onChangeFeedCategoryClick: (FeedSource) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onMarkAllReadForFeedSourceClick: (FeedSource) -> Unit,
    onMoveFeedSourcesToCategory: (List<FeedSource>, FeedSourceCategory?) -> Unit,
    dragState: DesktopDrawerDragState,
    sectionKey: String,
    sectionCategory: FeedSourceCategory?,
    onReorderFeedSource: (String, Int, FeedSource) -> Unit = { _, _, _ -> },
) {
    ConditionalAnimatedVisibility(
        visible = isCategoryExpanded,
        enter = expandVertically(
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntSize.VisibilityThreshold,
            ),
        ),
        exit = shrinkVertically(),
    ) {
        DesktopDrawerFeedSourcesList(
            drawerFeedSources = drawerFeedSources,
            currentFeedFilter = currentFeedFilter,
            drawerItemVisualStyle = drawerItemVisualStyle,
            selectedFeedSourceIds = selectedFeedSourceIds,
            onFeedSourceClick = onFeedSourceClick,
            selectedFeedSourcesProvider = selectedFeedSourcesProvider,
            onEditFeedClick = onEditFeedClick,
            onDeleteFeedSourceClick = onDeleteFeedSourceClick,
            onPinFeedClick = onPinFeedClick,
            onChangeFeedCategoryClick = onChangeFeedCategoryClick,
            onOpenWebsite = onOpenWebsite,
            onMarkAllReadForFeedSourceClick = onMarkAllReadForFeedSourceClick,
            onMoveFeedSourcesToCategory = onMoveFeedSourcesToCategory,
            onReorderFeedSource = onReorderFeedSource,
            dragState = dragState,
            sectionKey = sectionKey,
            indexInSection = 0,
            sectionSize = drawerFeedSources.size,
            sectionCategory = sectionCategory,
            isCategoryDropTarget = true,
            reorderEnabled = drawerFeedSources.size > 1,
        )
    }
}
