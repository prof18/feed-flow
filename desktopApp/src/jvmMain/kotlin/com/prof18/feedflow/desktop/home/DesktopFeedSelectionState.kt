package com.prof18.feedflow.desktop.home

internal data class DesktopFeedSelectionState(
    val selectedIds: Set<String> = emptySet(),
    val anchorId: String? = null,
) {
    fun select(
        id: String,
        visibleIds: List<String>,
        toggle: Boolean = false,
        range: Boolean = false,
        activeId: String? = null,
    ): DesktopFeedSelectionState {
        if (id !in visibleIds) return this
        if (range) {
            val anchor = anchorId?.takeIf { it in visibleIds } ?: activeId?.takeIf { it in visibleIds } ?: id
            val anchorIndex = visibleIds.indexOf(anchor)
            val targetIndex = visibleIds.indexOf(id)
            val rangeIds = visibleIds.subList(minOf(anchorIndex, targetIndex), maxOf(anchorIndex, targetIndex) + 1)
            return copy(selectedIds = if (toggle) selectedIds + rangeIds else rangeIds.toSet(), anchorId = anchor)
        }
        if (toggle) {
            val previous = selectedIds.ifEmpty { setOfNotNull(activeId) }
            return copy(
                selectedIds = if (id in previous) previous - id else previous + id,
                anchorId = id,
            )
        }
        return copy(selectedIds = emptySet(), anchorId = id)
    }

    fun contextTarget(id: String): DesktopFeedSelectionState =
        if (id in selectedIds) this else copy(selectedIds = setOf(id), anchorId = id)

    fun reconcile(presentIds: Set<String>): DesktopFeedSelectionState = copy(
        selectedIds = selectedIds intersect presentIds,
        anchorId = anchorId?.takeIf { it in presentIds },
    )
}
