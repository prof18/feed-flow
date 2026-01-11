package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.shared.ui.components.FeedSourceLogoImage
import com.prof18.feedflow.shared.ui.style.Spacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val UncategorizedDropKey = "__feedflow_uncategorized__"
private const val DragAutoScrollDeltaEpsilon = 0.5f
private const val DragAutoScrollFrameDelayMs = 16L
private const val DragEdgeThresholdDp = 48
private const val DragGhostPointerOffsetDp = 12
private const val DragMaxScrollPerTickDp = 18
private const val DraggingAlpha = 0.35f

internal class FeedSourceDragState(
    val isEnabled: Boolean,
    private val listState: LazyListState,
    private val coroutineScope: CoroutineScope,
    private val edgeThresholdPx: Float,
    private val maxScrollPerTickPx: Float,
) {
    var draggedFeedSources by mutableStateOf<List<FeedSource>>(emptyList())
        private set
    private var dragPositionInWindow by mutableStateOf<Offset?>(null)
    private val dropTargets = mutableStateMapOf<String, Rect>()
    private val dropTargetCategories = mutableStateMapOf<String, FeedSourceCategory?>()
    private var listBoundsInWindow by mutableStateOf<Rect?>(null)
    private var autoScrollJob: Job? = null
    private var autoScrollDelta by mutableStateOf(0f)

    fun startDrag(feedSources: List<FeedSource>, positionInWindow: Offset) {
        if (!isEnabled) return
        draggedFeedSources = feedSources
        dragPositionInWindow = positionInWindow
        updateAutoScroll()
    }

    fun updateDragPosition(positionInWindow: Offset) {
        if (!isEnabled) return
        dragPositionInWindow = positionInWindow
        updateAutoScroll()
    }

    fun clear() {
        draggedFeedSources = emptyList()
        dragPositionInWindow = null
        stopAutoScroll()
    }

    fun updateListBounds(coordinates: LayoutCoordinates) {
        if (!isEnabled) return
        val position = coordinates.positionInWindow()
        val size = coordinates.size
        listBoundsInWindow = Rect(position, Size(size.width.toFloat(), size.height.toFloat()))
    }

    fun updateDropTarget(category: FeedSourceCategory?, coordinates: LayoutCoordinates) {
        if (!isEnabled) return
        val position = coordinates.positionInWindow()
        val size = coordinates.size
        val rect = Rect(position, Size(size.width.toFloat(), size.height.toFloat()))
        val key = categoryKey(category)
        dropTargets[key] = rect
        dropTargetCategories[key] = category
    }

    fun removeDropTarget(category: FeedSourceCategory?) {
        if (!isEnabled) return
        val key = categoryKey(category)
        dropTargets.remove(key)
        dropTargetCategories.remove(key)
    }

    fun isDragOver(category: FeedSourceCategory?): Boolean {
        val position = dragPositionInWindow ?: return false
        val key = dropTargets.entries.firstOrNull { it.value.contains(position) }?.key ?: return false
        return key == categoryKey(category)
    }

    fun dropTargetKeyAtCurrentPosition(): String? {
        val position = dragPositionInWindow ?: return null
        return dropTargets.entries.firstOrNull { it.value.contains(position) }?.key
    }

    fun dropTargetCategoryForKey(key: String): FeedSourceCategory? = dropTargetCategories[key]

    fun dragOffsetInContainer(containerCoordinates: LayoutCoordinates): Offset? {
        val position = dragPositionInWindow ?: return null
        val containerPosition = containerCoordinates.positionInWindow()
        return position - containerPosition
    }

    fun isDragging(feedSource: FeedSource): Boolean =
        draggedFeedSources.any { it.id == feedSource.id }

    fun dragCount(): Int = draggedFeedSources.size

    private fun categoryKey(category: FeedSourceCategory?): String = category?.id ?: UncategorizedDropKey

    private fun updateAutoScroll() {
        val bounds = listBoundsInWindow ?: return stopAutoScroll()
        val position = dragPositionInWindow ?: return stopAutoScroll()
        if (draggedFeedSources.isEmpty()) return stopAutoScroll()

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
internal fun rememberFeedSourceDragState(listState: LazyListState): FeedSourceDragState {
    val isEnabled = isDragAndDropEnabled()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val edgeThresholdPx = with(density) { DragEdgeThresholdDp.dp.toPx() }
    val maxScrollPerTickPx = with(density) { DragMaxScrollPerTickDp.dp.toPx() }
    return remember(isEnabled, listState, edgeThresholdPx, maxScrollPerTickPx) {
        FeedSourceDragState(
            isEnabled = isEnabled,
            listState = listState,
            coroutineScope = coroutineScope,
            edgeThresholdPx = edgeThresholdPx,
            maxScrollPerTickPx = maxScrollPerTickPx,
        )
    }
}

internal fun Modifier.dropTargetModifier(
    dragState: FeedSourceDragState,
    category: FeedSourceCategory?,
    isDropTargetActive: Boolean,
    highlightColor: Color,
): Modifier {
    if (!dragState.isEnabled) return this
    val highlightModifier = if (isDropTargetActive) {
        Modifier.border(
            width = 1.dp,
            color = highlightColor.copy(alpha = 0.6f),
            shape = CircleShape,
        )
    } else {
        Modifier
    }

    return this
        .onGloballyPositioned { dragState.updateDropTarget(category, it) }
        .then(highlightModifier)
}

internal fun Modifier.feedSourceDragSource(
    dragState: FeedSourceDragState,
    feedSource: FeedSource,
    selectedFeedSources: () -> List<FeedSource>,
    onMoveFeedSourcesToCategory: (List<FeedSource>, FeedSourceCategory?) -> Unit,
): Modifier = composed {
    if (!dragState.isEnabled) return@composed this

    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val updatedFeedSource by rememberUpdatedState(feedSource)
    val updatedSelectedFeedSources by rememberUpdatedState(selectedFeedSources)
    val updatedOnMove by rememberUpdatedState(onMoveFeedSourcesToCategory)
    val isDragging = dragState.isDragging(feedSource)

    return@composed this
        .onGloballyPositioned { coordinates = it }
        .alpha(if (isDragging) DraggingAlpha else 1f)
        .pointerInput(feedSource.id) {
            detectDragGestures(
                onDragStart = { offset ->
                    val layoutCoordinates = coordinates ?: return@detectDragGestures
                    val selectedSources = updatedSelectedFeedSources()
                    val dragSources = if (selectedSources.any { it.id == updatedFeedSource.id }) {
                        selectedSources
                    } else {
                        listOf(updatedFeedSource)
                    }
                    dragState.startDrag(dragSources, layoutCoordinates.localToWindow(offset))
                },
                onDragEnd = {
                    val dropTargetKey = dragState.dropTargetKeyAtCurrentPosition()
                    val draggedFeedSources = dragState.draggedFeedSources
                    if (dropTargetKey != null && draggedFeedSources.isNotEmpty()) {
                        val targetCategory = dragState.dropTargetCategoryForKey(dropTargetKey)
                        val sourcesToMove = draggedFeedSources.filter {
                            it.category?.id != targetCategory?.id
                        }
                        if (sourcesToMove.isNotEmpty()) {
                            updatedOnMove(sourcesToMove, targetCategory)
                        }
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
internal fun DragGhost(
    dragState: FeedSourceDragState,
    drawerCoordinates: LayoutCoordinates?,
) {
    if (!dragState.isEnabled) return

    val feedSource = dragState.draggedFeedSources.firstOrNull() ?: return
    val containerCoordinates = drawerCoordinates ?: return
    val offset = dragState.dragOffsetInContainer(containerCoordinates) ?: return
    val dragCount = dragState.dragCount()
    val density = LocalDensity.current
    val pointerOffsetPx = with(density) { DragGhostPointerOffsetDp.dp.toPx() }
    val offsetX = (offset.x + pointerOffsetPx).roundToInt()
    val offsetY = (offset.y + pointerOffsetPx).roundToInt()

    Surface(
        modifier = Modifier
            .zIndex(1f)
            .offset { IntOffset(offsetX, offsetY) },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = Spacing.regular, vertical = Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val imageUrl = feedSource.logoUrl
            if (imageUrl != null) {
                FeedSourceLogoImage(
                    size = 20.dp,
                    imageUrl = imageUrl,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                )
            }

            Spacer(Modifier.width(Spacing.small))

            Text(
                text = feedSource.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (dragCount > 1) {
                Spacer(Modifier.width(Spacing.small))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = Spacing.small, vertical = 2.dp),
                        text = dragCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
internal fun FeedSourceDropTargetCleanup(
    dragState: FeedSourceDragState,
    category: FeedSourceCategory?,
) {
    if (!dragState.isEnabled) return
    DisposableEffect(category) {
        onDispose { dragState.removeDropTarget(category) }
    }
}
