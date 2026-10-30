package com.prof18.feedflow.desktop.ui.components

sealed interface SidebarKey {
    data object Timeline : SidebarKey
    data object Read : SidebarKey
    data object Bookmarks : SidebarKey
    data object Uncategorized : SidebarKey
    data class Category(val id: String) : SidebarKey
    data class Feed(val id: String) : SidebarKey
}

data class SidebarEntry(
    val key: SidebarKey,
    val childCount: Int = 0,
    val label: String? = null,
    val isPinned: Boolean = false,
) {
    init {
        require(childCount >= 0)
    }

    companion object {
        fun feed(id: String, title: String, isPinned: Boolean = false): SidebarEntry {
            require(id.isNotBlank())
            return SidebarEntry(SidebarKey.Feed(id), label = title, isPinned = isPinned)
        }

        fun category(id: String, title: String, childCount: Int): SidebarEntry {
            require(id.isNotBlank())
            return SidebarEntry(SidebarKey.Category(id), childCount, label = title)
        }
    }
}

data class SidebarState(
    val entries: List<SidebarEntry>,
    val activeKey: SidebarKey?,
    val focusedKey: SidebarKey? = activeKey,
    val selectedFeedIds: Set<String> = emptySet(),
    val activation: SidebarKey? = null,
) {
    fun moveFocus(delta: Int): SidebarState {
        if (entries.isEmpty() || delta == 0) return copy(activation = null)

        val currentIndex = entries.indexOfFirst { it.key == focusedKey }
            .takeIf { it >= 0 }
            ?: entries.indexOfFirst { it.key == activeKey }.coerceAtLeast(0)
        val nextIndex = (currentIndex + delta).coerceIn(entries.indices)
        return copy(focusedKey = entries[nextIndex].key, activation = null)
    }

    fun activateFocused(isMultiSelect: Boolean = false): SidebarState =
        focusedKey?.let { activate(it, isMultiSelect) } ?: copy(activation = null)

    fun activatePointer(key: SidebarKey, isMultiSelect: Boolean = false): SidebarState =
        activate(key, isMultiSelect)

    private fun activate(key: SidebarKey, isMultiSelect: Boolean): SidebarState {
        if (entries.none { it.key == key }) return copy(activation = null)
        if (isMultiSelect) {
            val updatedSelection = if (key is SidebarKey.Feed) {
                if (key.id in selectedFeedIds) selectedFeedIds - key.id else selectedFeedIds + key.id
            } else {
                selectedFeedIds
            }
            return copy(focusedKey = key, selectedFeedIds = updatedSelection, activation = null)
        }

        return copy(
            activeKey = key,
            focusedKey = key,
            selectedFeedIds = emptySet(),
            activation = key,
        )
    }

    companion object {
        fun reconcile(
            entries: List<SidebarEntry>,
            activeKey: SidebarKey?,
            focusedKey: SidebarKey? = activeKey,
            selectedFeedIds: Set<String> = emptySet(),
        ): SidebarState {
            val keys = entries.mapTo(mutableSetOf()) { it.key }
            val timelineKey = SidebarKey.Timeline.takeIf { it in keys }
            val reconciledActiveKey = activeKey?.takeIf { it in keys } ?: timelineKey
            val reconciledFocusKey = focusedKey?.takeIf { it in keys } ?: reconciledActiveKey
            val presentFeedIds = entries.mapNotNullTo(mutableSetOf()) { entry ->
                (entry.key as? SidebarKey.Feed)?.id
            }

            return SidebarState(
                entries = entries,
                activeKey = reconciledActiveKey,
                focusedKey = reconciledFocusKey,
                selectedFeedIds = selectedFeedIds intersect presentFeedIds,
            )
        }
    }
}
