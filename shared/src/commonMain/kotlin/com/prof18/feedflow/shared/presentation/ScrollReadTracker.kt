package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.VisibleFeedItem

internal class ScrollReadTracker {

    private var previousVisibleItems: Map<String, VisibleFeedItem> = emptyMap()
    private var previousFirstVisibleIndex: Int? = null

    // A visible snapshot is valid only for the list version and settings that produced it.
    // If either changes, indices may no longer describe the same visual list.
    private var observedFeedListVersion: Long? = null
    private var observedListShapeKey: ScrollReadListShapeKey? = null

    fun onVisibleItemsChanged(
        visibleItems: List<VisibleFeedItem>,
        feedListVersion: Long,
        listShapeKey: ScrollReadListShapeKey,
    ): Set<FeedItemId> {
        if (
            observedFeedListVersion != feedListVersion ||
            observedListShapeKey != listShapeKey
        ) {
            resetVisibleItems()
            observedFeedListVersion = feedListVersion
            observedListShapeKey = listShapeKey
        }

        // Empty snapshots happen during list teardown/reload. Keep them from becoming a
        // synthetic scroll event that marks the previous viewport as read.
        if (visibleItems.isEmpty()) {
            resetVisibleItems()
            return emptySet()
        }

        val currentVisibleItems = visibleItems.associateBy { it.id }
        val currentFirstVisibleIndex = visibleItems.minOf { it.index }
        val previousFirstIndex = previousFirstVisibleIndex

        // Only forward scrolling can mark items. The first snapshot just seeds the tracker,
        // and backward/stationary movement must not mark anything.
        if (previousFirstIndex == null || currentFirstVisibleIndex <= previousFirstIndex) {
            previousVisibleItems = currentVisibleItems
            previousFirstVisibleIndex = currentFirstVisibleIndex
            return emptySet()
        }

        // Be conservative: mark only unread items that were actually visible before and are
        // now gone above the viewport. Fast flings never mark unobserved rows in the gap.
        val idsToMark = previousVisibleItems.values
            .filter { previousItem ->
                !previousItem.isRead &&
                    previousItem.id !in currentVisibleItems &&
                    previousItem.index < currentFirstVisibleIndex
            }
            .map { FeedItemId(it.id) }
            .toSet()

        previousVisibleItems = currentVisibleItems
        previousFirstVisibleIndex = currentFirstVisibleIndex

        return idsToMark
    }

    fun reset() {
        resetVisibleItems()
        observedFeedListVersion = null
        observedListShapeKey = null
    }

    private fun resetVisibleItems() {
        previousVisibleItems = emptyMap()
        previousFirstVisibleIndex = null
    }
}

internal data class ScrollReadListShapeKey(
    val showReadArticlesTimeline: Boolean,
    val hideReadItems: Boolean,
)
