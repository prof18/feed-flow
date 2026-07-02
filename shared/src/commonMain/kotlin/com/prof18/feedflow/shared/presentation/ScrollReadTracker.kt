package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.VisibleFeedItem

internal class ScrollReadTracker {

    private var previousFirstVisibleId: String? = null

    // A visible snapshot is valid only for the list version and settings that produced it.
    // If either changes, indices may no longer describe the same visual list.
    private var observedFeedListVersion: Long? = null
    private var observedListShapeKey: ScrollReadListShapeKey? = null

    fun onVisibleItemsChanged(
        visibleItems: List<VisibleFeedItem>,
        feedItems: List<FeedItem>,
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

        // Empty snapshots can happen transiently while a virtualized list is relayouting.
        // Keep the previous boundary; explicit resets and list version changes handle real
        // teardown/reload cases.
        if (visibleItems.isEmpty()) {
            return emptySet()
        }

        val currentFirstVisibleId = visibleItems.first().id
        val previousFirstId = previousFirstVisibleId

        // Only forward scrolling can mark items. The first snapshot just seeds the tracker,
        // and backward/stationary movement must not mark anything.
        if (previousFirstId == null) {
            previousFirstVisibleId = currentFirstVisibleId
            return emptySet()
        }

        val previousFirstPosition = feedItems.indexOfFirst { it.id == previousFirstId }
        val currentFirstPosition = feedItems.indexOfFirst { it.id == currentFirstVisibleId }
        if (previousFirstPosition == -1 || currentFirstPosition == -1) {
            previousFirstVisibleId = currentFirstVisibleId
            return emptySet()
        }

        if (currentFirstPosition <= previousFirstPosition) {
            previousFirstVisibleId = currentFirstVisibleId
            return emptySet()
        }

        // Once the first visible article moves forward in the loaded list, every loaded
        // unread item before the current first visible article has been passed. The range
        // is resolved by stable article IDs, so snapshot row indices are not used as identity.
        val idsToMark = if (previousFirstPosition < currentFirstPosition) {
            feedItems
                .subList(previousFirstPosition, currentFirstPosition)
                .filter { !it.isRead }
                .map { FeedItemId(it.id) }
                .toSet()
        } else {
            emptySet()
        }

        previousFirstVisibleId = currentFirstVisibleId

        return idsToMark
    }

    fun reset() {
        resetVisibleItems()
        observedFeedListVersion = null
        observedListShapeKey = null
    }

    private fun resetVisibleItems() {
        previousFirstVisibleId = null
    }
}

internal data class ScrollReadListShapeKey(
    val showReadArticlesTimeline: Boolean,
    val hideReadItems: Boolean,
    val feedLayout: FeedLayout,
    val isGridLayoutEnabled: Boolean,
)
