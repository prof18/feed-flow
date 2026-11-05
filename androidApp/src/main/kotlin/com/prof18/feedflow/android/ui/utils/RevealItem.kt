package com.prof18.feedflow.android.ui.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.snapshotFlow
import com.prof18.feedflow.shared.presentation.model.isFullyVisibleInViewport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first

suspend fun LazyListState.revealItemConditionally(index: Int, reduceMotionEnabled: Boolean) {
    snapshotFlow { layoutInfo.totalItemsCount > 0 }.first { it }
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index }
    val visible = item != null && isFullyVisibleInViewport(
        itemStart = item.offset,
        itemSize = item.size,
        viewportEndOffset = info.viewportEndOffset,
        afterContentPadding = info.afterContentPadding,
    )
    if (visible) return
    revealInterruptibly { scrollToItemConditionally(index, reduceMotionEnabled = reduceMotionEnabled) }
}

suspend fun LazyStaggeredGridState.revealItemConditionally(index: Int, reduceMotionEnabled: Boolean) {
    snapshotFlow { layoutInfo.totalItemsCount > 0 }.first { it }
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index }
    val visible = item != null && isFullyVisibleInViewport(
        itemStart = item.offset.y,
        itemSize = item.size.height,
        viewportEndOffset = info.viewportEndOffset,
        afterContentPadding = info.afterContentPadding,
    )
    if (visible) return
    revealInterruptibly { scrollToItemConditionally(index, reduceMotionEnabled = reduceMotionEnabled) }
}

suspend fun LazyGridState.revealItemConditionally(index: Int, reduceMotionEnabled: Boolean) {
    snapshotFlow { layoutInfo.totalItemsCount > 0 }.first { it }
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index }
    val visible = item != null && isFullyVisibleInViewport(
        itemStart = item.offset.y,
        itemSize = item.size.height,
        viewportEndOffset = info.viewportEndOffset,
        afterContentPadding = info.afterContentPadding,
    )
    if (visible) return
    revealInterruptibly { scrollToItemConditionally(index, reduceMotionEnabled = reduceMotionEnabled) }
}

@Suppress("SwallowedException")
private suspend fun revealInterruptibly(scroll: suspend () -> Unit) {
    try {
        scroll()
    } catch (e: CancellationException) {
        // A user scroll interrupts the animation; that still counts as revealed.
        // When the caller itself is cancelled (the list left composition), keep propagating.
        currentCoroutineContext().ensureActive()
    }
}
