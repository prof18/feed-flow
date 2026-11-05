package com.prof18.feedflow.desktop.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.snapshotFlow
import com.prof18.feedflow.shared.presentation.model.isFullyVisibleInViewport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first

// The list sits beside the reader, which lays out the new article in the same frames; an animated
// scroll gets starved and stutters, so the reveal snaps like a mail client keeping its selection in view.
suspend fun LazyListState.revealItem(index: Int) {
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
    revealInterruptibly { scrollToItem(index) }
}

suspend fun LazyStaggeredGridState.revealItem(index: Int) {
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
    revealInterruptibly { scrollToItem(index) }
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
