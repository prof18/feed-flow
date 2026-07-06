package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val DragAutoScrollDeltaEpsilon = 0.5f
private const val DragAutoScrollFrameDelayMs = 16L
private const val DragEdgeThresholdDp = 48
private const val DragMaxScrollPerTickDp = 18
private const val ReorderEndMarginFraction = 0.5f

internal class DesktopDrawerDragState(
    private val listState: LazyListState,
    private val coroutineScope: CoroutineScope,
    private val edgeThresholdPx: Float,
    private val maxScrollPerTickPx: Float,
) {
    var dragPayload by mutableStateOf<DrawerDragPayload?>(null)
        private set
    private var dragPositionInWindow by mutableStateOf<Offset?>(null)
    private val categoryDropTargets = mutableStateMapOf<String, Rect>()
    private val dropTargetCategories = mutableStateMapOf<String, FeedSourceCategory?>()
    private val reorderSlots = mutableStateMapOf<String, ReorderSlot>()
    private val categoryHeaderSlots = mutableStateMapOf<String, CategoryHeaderSlot>()
    private var listBoundsInWindow by mutableStateOf<Rect?>(null)
    private var autoScrollJob: Job? = null
    private var autoScrollDelta by mutableStateOf(0f)

    fun startDrag(payload: DrawerDragPayload, positionInWindow: Offset) {
        dragPayload = payload
        dragPositionInWindow = positionInWindow
        updateAutoScroll()
    }

    fun updateDragPosition(positionInWindow: Offset) {
        dragPositionInWindow = positionInWindow
        updateAutoScroll()
    }

    fun clear() {
        dragPayload = null
        dragPositionInWindow = null
        stopAutoScroll()
    }

    fun updateListBounds(coordinates: LayoutCoordinates) {
        val position = coordinates.positionInWindow()
        val size = coordinates.size
        listBoundsInWindow = Rect(position, Size(size.width.toFloat(), size.height.toFloat()))
    }

    fun updateCategoryDropTarget(category: FeedSourceCategory?, coordinates: LayoutCoordinates) {
        val position = coordinates.positionInWindow()
        val size = coordinates.size
        val rect = Rect(position, Size(size.width.toFloat(), size.height.toFloat()))
        val key = desktopDrawerCategorySectionKey(category)
        categoryDropTargets[key] = rect
        dropTargetCategories[key] = category
    }

    fun removeCategoryDropTarget(category: FeedSourceCategory?) {
        val key = desktopDrawerCategorySectionKey(category)
        categoryDropTargets.remove(key)
        dropTargetCategories.remove(key)
    }

    fun updateReorderSlot(slotKey: String, slot: ReorderSlot) {
        reorderSlots[slotKey] = slot
    }

    fun removeReorderSlot(slotKey: String) {
        reorderSlots.remove(slotKey)
    }

    fun updateCategoryHeaderSlot(slot: CategoryHeaderSlot) {
        categoryHeaderSlots[slot.sectionKey] = slot
    }

    fun removeCategoryHeaderSlot(sectionKey: String) {
        categoryHeaderSlots.remove(sectionKey)
    }

    fun isDragOver(category: FeedSourceCategory?): Boolean {
        val decision = currentDropDecision() as? DrawerDropDecision.MoveToCategory ?: return false
        return decision.category?.id == category?.id
    }

    fun currentDropDecision(): DrawerDropDecision? {
        val position = dragPositionInWindow ?: return null
        return when (val payload = dragPayload) {
            is DrawerDragPayload.FeedSources -> currentFeedSourceDropDecision(payload, position)
            is DrawerDragPayload.Category -> currentCategoryDropDecision(payload, position)
            null -> null
        }
    }

    fun dragOffsetInContainer(containerCoordinates: LayoutCoordinates): Offset? {
        val position = dragPositionInWindow ?: return null
        val containerPosition = containerCoordinates.positionInWindow()
        return position - containerPosition
    }

    fun isDragging(feedSource: FeedSource): Boolean {
        val feedSources = (dragPayload as? DrawerDragPayload.FeedSources)?.feedSources ?: return false
        return feedSources.any { it.id == feedSource.id }
    }

    fun dragCount(): Int = when (val payload = dragPayload) {
        is DrawerDragPayload.FeedSources -> payload.feedSources.size
        is DrawerDragPayload.Category -> 1
        null -> 0
    }

    private fun currentFeedSourceDropDecision(
        payload: DrawerDragPayload.FeedSources,
        position: Offset,
    ): DrawerDropDecision? {
        if (payload.feedSources.size == 1) {
            val reorderDecision = currentFeedSourceReorderDecision(payload, position)
            if (reorderDecision != null) {
                return reorderDecision
            }
        }

        return currentMoveToCategoryDecision(payload.feedSources, position)
    }

    private fun currentFeedSourceReorderDecision(
        payload: DrawerDragPayload.FeedSources,
        position: Offset,
    ): DrawerDropDecision.ReorderInSection? {
        val sectionSlots = reorderSlots.values
            .filter { it.sectionKey == payload.sourceSectionKey && it.reorderEnabled }
            .sortedBy { it.index }

        if (sectionSlots.size <= 1 || !positionInSlotsVerticalSpan(position, sectionSlots)) {
            return null
        }

        val insertionIndex = insertionIndexForSlots(position, sectionSlots) ?: return null
        if (insertionIndex == payload.sourceIndex || insertionIndex == payload.sourceIndex + 1) {
            return null
        }

        return DrawerDropDecision.ReorderInSection(
            sectionKey = payload.sourceSectionKey,
            insertionIndex = insertionIndex,
        )
    }

    private fun currentMoveToCategoryDecision(
        feedSources: List<FeedSource>,
        position: Offset,
    ): DrawerDropDecision.MoveToCategory? {
        val headerTargetKey = categoryDropTargets.entries
            .firstOrNull { it.value.contains(position) }
            ?.key
        val rowTarget = reorderSlots.values
            .firstOrNull {
                it.isCategoryDropTarget && it.rectInWindow.contains(position)
            }

        val targetCategory = when {
            headerTargetKey != null -> dropTargetCategories[headerTargetKey]
            rowTarget != null -> rowTarget.category
            else -> return null
        }

        val canMoveAnySource = feedSources.any { it.category?.id != targetCategory?.id }
        if (!canMoveAnySource) {
            return null
        }

        return DrawerDropDecision.MoveToCategory(targetCategory)
    }

    private fun currentCategoryDropDecision(
        payload: DrawerDragPayload.Category,
        position: Offset,
    ): DrawerDropDecision.ReorderCategories? {
        val slots = categoryHeaderSlots.values.sortedBy { it.index }
        if (slots.size <= 1 || !positionInCategoryHeaderSpan(position, slots)) {
            return null
        }

        val insertionIndex = insertionIndexForCategorySlots(position, slots)
            ?: return null

        if (insertionIndex == payload.sourceIndex || insertionIndex == payload.sourceIndex + 1) {
            return null
        }

        return DrawerDropDecision.ReorderCategories(insertionIndex)
    }

    private fun positionInSlotsVerticalSpan(
        position: Offset,
        slots: List<ReorderSlot>,
    ): Boolean {
        val left = slots.minOf { it.rectInWindow.left }
        val right = slots.maxOf { it.rectInWindow.right }
        val top = slots.minOf { it.rectInWindow.top }
        val lastSlot = slots.maxBy { it.index }
        val endMargin = lastSlot.rectInWindow.height * ReorderEndMarginFraction
        val bottom = lastSlot.rectInWindow.bottom + endMargin
        return position.x in left..right && position.y in top..bottom
    }

    private fun positionInCategoryHeaderSpan(
        position: Offset,
        slots: List<CategoryHeaderSlot>,
    ): Boolean {
        val left = slots.minOf { it.rectInWindow.left }
        val right = slots.maxOf { it.rectInWindow.right }
        val top = slots.minOf { it.rectInWindow.top }
        val lastSlot = slots.maxBy { it.index }
        val endMargin = lastSlot.rectInWindow.height * ReorderEndMarginFraction
        val bottom = lastSlot.rectInWindow.bottom + endMargin
        return position.x in left..right && position.y in top..bottom
    }

    private fun insertionIndexForSlots(
        position: Offset,
        slots: List<ReorderSlot>,
    ): Int? {
        val targetSlot = slots.firstOrNull { position.y <= it.rectInWindow.bottom }
            ?: return slots.lastOrNull()?.let { it.index + 1 }
        return if (position.y < targetSlot.rectInWindow.center.y) {
            targetSlot.index
        } else {
            targetSlot.index + 1
        }
    }

    private fun insertionIndexForCategorySlots(
        position: Offset,
        slots: List<CategoryHeaderSlot>,
    ): Int? {
        val targetSlot = slots.firstOrNull { position.y <= it.rectInWindow.bottom }
            ?: return slots.lastOrNull()?.let { it.index + 1 }
        return if (position.y < targetSlot.rectInWindow.center.y) {
            targetSlot.index
        } else {
            targetSlot.index + 1
        }
    }

    private fun updateAutoScroll() {
        val bounds = listBoundsInWindow ?: return stopAutoScroll()
        val position = dragPositionInWindow ?: return stopAutoScroll()
        if (dragPayload == null) return stopAutoScroll()

        val topDistance = position.y - bounds.top
        val bottomDistance = bounds.bottom - position.y
        val desiredDelta = when {
            topDistance in 0f..edgeThresholdPx -> {
                val progress = 1f - (topDistance / edgeThresholdPx)
                -maxScrollPerTickPx * progress
            }
            bottomDistance in 0f..edgeThresholdPx -> {
                val progress = 1f - (bottomDistance / edgeThresholdPx)
                maxScrollPerTickPx * progress
            }
            else -> 0f
        }

        val cannotScroll = desiredDelta == 0f ||
            (desiredDelta < 0 && !listState.canScrollBackward) ||
            (desiredDelta > 0 && !listState.canScrollForward)

        if (cannotScroll) {
            stopAutoScroll()
            return
        }

        if (autoScrollJob != null && abs(desiredDelta - autoScrollDelta) < DragAutoScrollDeltaEpsilon) {
            return
        }

        autoScrollDelta = desiredDelta
        autoScrollJob?.cancel()
        autoScrollJob = coroutineScope.launch {
            while (isActive) {
                listState.scrollBy(autoScrollDelta)
                delay(DragAutoScrollFrameDelayMs)
            }
        }
    }

    private fun stopAutoScroll() {
        autoScrollJob?.cancel()
        autoScrollJob = null
        autoScrollDelta = 0f
    }
}

@Composable
internal fun rememberDesktopDrawerDragState(listState: LazyListState): DesktopDrawerDragState {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val edgeThresholdPx = with(density) { DragEdgeThresholdDp.dp.toPx() }
    val maxScrollPerTickPx = with(density) { DragMaxScrollPerTickDp.dp.toPx() }
    return remember(listState, edgeThresholdPx, maxScrollPerTickPx) {
        DesktopDrawerDragState(
            listState = listState,
            coroutineScope = coroutineScope,
            edgeThresholdPx = edgeThresholdPx,
            maxScrollPerTickPx = maxScrollPerTickPx,
        )
    }
}
