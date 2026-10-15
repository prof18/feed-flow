package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.ui.ConditionalAnimatedVisibility
import com.prof18.feedflow.desktop.ui.components.DesktopPopupMenuEntry
import com.prof18.feedflow.desktop.ui.components.FeedSourceLogoImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import org.jetbrains.jewel.foundation.theme.LocalContentColor
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DesktopDrawerFeedSourcesList(
    drawerFeedSources: ImmutableList<DrawerItem.DrawerFeedSource>,
    currentFeedFilter: FeedFilter,
    drawerItemVisualStyle: DrawerItemVisualStyle,
    selectedFeedSourceIds: ImmutableSet<String>,
    onFeedSourceClick: (FeedSource, Boolean, Boolean) -> Unit,
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
    onContextTarget: (FeedSource) -> Unit = {},
    batchMenuEntries: ImmutableList<DesktopPopupMenuEntry>? = null,
) {
    Column(
        modifier = modifier,
    ) {
        drawerFeedSources.forEachIndexed { rowIndex, feedSourceWrapper ->
            val windowInfo = LocalWindowInfo.current
            var pointerModifiers by remember(feedSourceWrapper.feedSource.id) {
                mutableStateOf<PointerKeyboardModifiers?>(null)
            }
            val isMultiSelected = selectedFeedSourceIds.contains(feedSourceWrapper.feedSource.id)
            val rowIndexInSection = indexInSection + rowIndex
            val slotKey = "$sectionKey:${feedSourceWrapper.feedSource.id}"

            ReorderSlotCleanup(
                dragState = dragState,
                slotKey = slotKey,
            )

            DesktopFeedSourceDrawerItem(
                label = {
                    Tooltip(tooltip = { Text(feedSourceWrapper.feedSource.title) }) {
                        Text(
                            text = feedSourceWrapper.feedSource.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                selected = currentFeedFilter is FeedFilter.Source &&
                    currentFeedFilter.feedSource == feedSourceWrapper.feedSource,
                drawerItemVisualStyle = drawerItemVisualStyle,
                onClick = {
                    val modifiers = pointerModifiers ?: windowInfo.keyboardModifiers
                    val toggle = if (getDesktopOS().isMacOs()) modifiers.isMetaPressed else modifiers.isCtrlPressed
                    onFeedSourceClick(feedSourceWrapper.feedSource, toggle, modifiers.isShiftPressed)
                    pointerModifiers = null
                },
                icon = {
                    val imageUrl = feedSourceWrapper.feedSource.logoUrl
                    if (imageUrl != null) {
                        FeedSourceLogoImage(
                            size = 16.dp,
                            cornerRadius = 4.dp,
                            imageUrl = imageUrl,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(16.dp),
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
                onContextClick = { onContextTarget(feedSourceWrapper.feedSource) },
                batchMenuEntries = batchMenuEntries.takeIf { isMultiSelected },
                onPointerModifiersChanged = { pointerModifiers = it },
                isMultiSelected = isMultiSelected,
                modifier = Modifier
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Enter || event.key == Key.Spacebar) {
                            if (event.type == KeyEventType.KeyDown) {
                                val toggle = if (getDesktopOS().isMacOs()) event.isMetaPressed else event.isCtrlPressed
                                onFeedSourceClick(feedSourceWrapper.feedSource, toggle, event.isShiftPressed)
                            }
                            true
                        } else {
                            false
                        }
                    }
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
    onFeedSourceClick: (FeedSource, Boolean, Boolean) -> Unit,
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
