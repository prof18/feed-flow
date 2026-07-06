package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory

private const val DraggingAlpha = 0.35f
private const val ReorderInsertionIndicatorStrokeDp = 2

internal fun Modifier.dropTargetModifier(
    dragState: DesktopDrawerDragState,
    category: FeedSourceCategory?,
    isDropTargetActive: Boolean,
    highlightColor: Color,
    shape: Shape,
): Modifier {
    val highlightModifier = if (isDropTargetActive) {
        Modifier.border(
            width = 1.dp,
            color = highlightColor.copy(alpha = 0.6f),
            shape = shape,
        )
    } else {
        Modifier
    }

    return this
        .onGloballyPositioned { dragState.updateCategoryDropTarget(category, it) }
        .then(highlightModifier)
}

internal fun Modifier.reorderSlot(
    dragState: DesktopDrawerDragState,
    slotKey: String,
    sectionKey: String,
    index: Int,
    category: FeedSourceCategory?,
    isCategoryDropTarget: Boolean,
    reorderEnabled: Boolean,
): Modifier = this.onGloballyPositioned { coordinates ->
    val position = coordinates.positionInWindow()
    val size = coordinates.size
    dragState.updateReorderSlot(
        slotKey = slotKey,
        slot = ReorderSlot(
            sectionKey = sectionKey,
            index = index,
            rectInWindow = Rect(position, Size(size.width.toFloat(), size.height.toFloat())),
            category = category,
            isCategoryDropTarget = isCategoryDropTarget,
            reorderEnabled = reorderEnabled,
        ),
    )
}

internal fun Modifier.categoryHeaderReorderSlot(
    dragState: DesktopDrawerDragState,
    sectionKey: String,
    index: Int,
): Modifier = this.onGloballyPositioned { coordinates ->
    val position = coordinates.positionInWindow()
    val size = coordinates.size
    dragState.updateCategoryHeaderSlot(
        CategoryHeaderSlot(
            sectionKey = sectionKey,
            index = index,
            rectInWindow = Rect(position, Size(size.width.toFloat(), size.height.toFloat())),
        ),
    )
}

@Suppress("ModifierComposed")
internal fun Modifier.feedSourceReorderInsertionIndicator(
    dragState: DesktopDrawerDragState,
    sectionKey: String,
    index: Int,
    isLast: Boolean,
): Modifier = composed {
    val color = androidx.compose.material3.MaterialTheme.colorScheme.primary
    this.drawWithContent {
        drawContent()
        val decision = dragState.currentDropDecision() as? DrawerDropDecision.ReorderInSection
        if (decision?.sectionKey != sectionKey) {
            return@drawWithContent
        }

        val strokeWidth = ReorderInsertionIndicatorStrokeDp.dp.toPx()
        when (decision.insertionIndex) {
            index -> drawLine(
                color = color,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = strokeWidth,
            )
            index + 1 -> if (isLast) {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth,
                )
            }
        }
    }
}

@Suppress("ModifierComposed")
internal fun Modifier.categoryReorderInsertionIndicator(
    dragState: DesktopDrawerDragState,
    index: Int,
    isLast: Boolean,
): Modifier = composed {
    val color = androidx.compose.material3.MaterialTheme.colorScheme.primary
    this.drawWithContent {
        drawContent()
        val decision = dragState.currentDropDecision() as? DrawerDropDecision.ReorderCategories
            ?: return@drawWithContent
        val strokeWidth = ReorderInsertionIndicatorStrokeDp.dp.toPx()
        when (decision.insertionIndex) {
            index -> drawLine(
                color = color,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = strokeWidth,
            )
            index + 1 -> if (isLast) {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth,
                )
            }
        }
    }
}

@Suppress("ModifierComposed")
internal fun Modifier.feedSourceDragSource(
    dragState: DesktopDrawerDragState,
    feedSource: FeedSource,
    sectionKey: String,
    indexInSection: Int,
    selectedFeedSources: () -> List<FeedSource>,
    onMoveFeedSourcesToCategory: (List<FeedSource>, FeedSourceCategory?) -> Unit,
    onReorderFeedSource: (String, Int, FeedSource) -> Unit,
): Modifier = composed {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val updatedFeedSource by rememberUpdatedState(feedSource)
    val updatedSelectedFeedSources by rememberUpdatedState(selectedFeedSources)
    val updatedOnMove by rememberUpdatedState(onMoveFeedSourcesToCategory)
    val updatedOnReorder by rememberUpdatedState(onReorderFeedSource)
    val isDragging = dragState.isDragging(feedSource)

    return@composed this
        .onGloballyPositioned { coordinates = it }
        .alpha(if (isDragging) DraggingAlpha else 1f)
        .pointerInput(feedSource.id, sectionKey, indexInSection) {
            detectDragGestures(
                onDragStart = { offset ->
                    val layoutCoordinates = coordinates ?: return@detectDragGestures
                    val selectedSources = updatedSelectedFeedSources()
                    val dragSources = if (selectedSources.any { it.id == updatedFeedSource.id }) {
                        selectedSources
                    } else {
                        listOf(updatedFeedSource)
                    }
                    dragState.startDrag(
                        payload = DrawerDragPayload.FeedSources(
                            feedSources = dragSources,
                            sourceSectionKey = sectionKey,
                            sourceIndex = indexInSection,
                        ),
                        positionInWindow = layoutCoordinates.localToWindow(offset),
                    )
                },
                onDragEnd = {
                    when (val decision = dragState.currentDropDecision()) {
                        is DrawerDropDecision.MoveToCategory -> {
                            val payload = dragState.dragPayload as? DrawerDragPayload.FeedSources
                            val draggedFeedSources = payload?.feedSources.orEmpty()
                            val sourcesToMove = draggedFeedSources.filter {
                                it.category?.id != decision.category?.id
                            }
                            if (sourcesToMove.isNotEmpty()) {
                                updatedOnMove(sourcesToMove, decision.category)
                            }
                        }
                        is DrawerDropDecision.ReorderInSection -> {
                            updatedOnReorder(decision.sectionKey, decision.insertionIndex, updatedFeedSource)
                        }
                        else -> Unit
                    }
                    dragState.clear()
                },
                onDragCancel = {
                    dragState.clear()
                },
                onDrag = { change, _ ->
                    val layoutCoordinates = coordinates ?: return@detectDragGestures
                    dragState.updateDragPosition(layoutCoordinates.localToWindow(change.position))
                    change.consume()
                },
            )
        }
}

@Suppress("ModifierComposed")
internal fun Modifier.categoryDragSource(
    dragState: DesktopDrawerDragState,
    categoryKey: String,
    categoryTitle: String,
    index: Int,
    enabled: Boolean,
    onDropReorder: (Int) -> Unit,
): Modifier = composed {
    if (!enabled) {
        return@composed this
    }

    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val updatedOnDropReorder by rememberUpdatedState(onDropReorder)

    return@composed this
        .onGloballyPositioned { coordinates = it }
        .pointerInput(categoryKey, index) {
            detectDragGestures(
                onDragStart = { offset ->
                    val layoutCoordinates = coordinates ?: return@detectDragGestures
                    dragState.startDrag(
                        payload = DrawerDragPayload.Category(
                            categoryKey = categoryKey,
                            title = categoryTitle,
                            sourceIndex = index,
                        ),
                        positionInWindow = layoutCoordinates.localToWindow(offset),
                    )
                },
                onDragEnd = {
                    val decision = dragState.currentDropDecision() as? DrawerDropDecision.ReorderCategories
                    if (decision != null) {
                        updatedOnDropReorder(decision.insertionIndex)
                    }
                    dragState.clear()
                },
                onDragCancel = {
                    dragState.clear()
                },
                onDrag = { change, _ ->
                    val layoutCoordinates = coordinates ?: return@detectDragGestures
                    dragState.updateDragPosition(layoutCoordinates.localToWindow(change.position))
                    change.consume()
                },
            )
        }
}

@Composable
internal fun DrawerDropTargetCleanup(
    dragState: DesktopDrawerDragState,
    category: FeedSourceCategory?,
) {
    DisposableEffect(category) {
        onDispose { dragState.removeCategoryDropTarget(category) }
    }
}

@Composable
internal fun ReorderSlotCleanup(
    dragState: DesktopDrawerDragState,
    slotKey: String,
) {
    DisposableEffect(slotKey) {
        onDispose { dragState.removeReorderSlot(slotKey) }
    }
}

@Composable
internal fun CategoryHeaderSlotCleanup(
    dragState: DesktopDrawerDragState,
    sectionKey: String,
) {
    DisposableEffect(sectionKey) {
        onDispose { dragState.removeCategoryHeaderSlot(sectionKey) }
    }
}
