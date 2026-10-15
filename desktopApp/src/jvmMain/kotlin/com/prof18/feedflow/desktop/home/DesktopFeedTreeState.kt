package com.prof18.feedflow.desktop.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper
import com.prof18.feedflow.core.model.NavDrawerState

internal fun projectDesktopFeedTree(drawer: NavDrawerState): DesktopFeedTreeProjection {
    val pinned = drawer.pinnedFeedSources.filterIsInstance<DrawerItem.DrawerFeedSource>()
        .distinctBy { it.feedSource.id }
    val pinnedIds = pinned.mapTo(mutableSetOf()) { it.feedSource.id }
    val groupedFeeds = drawer.feedSourcesByCategory.values.flatten()
        .filterIsInstance<DrawerItem.DrawerFeedSource>()
    val sources = (
        groupedFeeds + drawer.feedSourcesWithoutCategory.filterIsInstance<DrawerItem.DrawerFeedSource>() + pinned
        ).distinctBy { it.feedSource.id }
    val declaredCategories = drawer.categories.filterIsInstance<DrawerItem.DrawerCategory>()
    val categories = (
        declaredCategories.map { it.category } +
            drawer.feedSourcesByCategory.keys.mapNotNull { it.feedSourceCategory } +
            sources.mapNotNull { it.feedSource.category }
        ).distinctBy { it.id }
    val categoryBySource = drawer.feedSourcesByCategory.entries.flatMap { (wrapper, items) ->
        items.filterIsInstance<DrawerItem.DrawerFeedSource>().map {
            it.feedSource.id to wrapper.feedSourceCategory
        }
    }.toMap()
    val sections = buildList {
        val loose = sources.filter { (it.feedSource.category ?: categoryBySource[it.feedSource.id]) == null }
        if (loose.isNotEmpty()) {
            add(
                DesktopFeedTreeSection(
                    id = "uncategorized",
                    categoryWrapper = FeedSourceCategoryWrapper(null),
                    members = loose,
                    visibleFeeds = loose.filterNot { it.feedSource.id in pinnedIds },
                    unreadCount = loose.sumOf { it.unreadCount },
                    position = drawer.uncategorizedPosition,
                ),
            )
        }
        categories.forEach { category ->
            val members = sources.filter {
                (it.feedSource.category ?: categoryBySource[it.feedSource.id])?.id == category.id
            }
            add(
                DesktopFeedTreeSection(
                    id = "category:${category.id}",
                    categoryWrapper = FeedSourceCategoryWrapper(category),
                    members = members,
                    visibleFeeds = members.filterNot { it.feedSource.id in pinnedIds },
                    unreadCount = declaredCategories.firstOrNull { it.category.id == category.id }?.unreadCount
                        ?: members.sumOf { it.unreadCount },
                    position = category.position,
                ),
            )
        }
    }.sortedBy { it.position }
    return DesktopFeedTreeProjection(pinned, sections)
}

// Navigation and management windows share application-scoped UI preferences.
@Suppress("CompositionLocalAllowlist")
internal val LocalDesktopFeedTreeState = compositionLocalOf<DesktopFeedTreeState?> { null }

@Composable
internal fun rememberDesktopFeedTreeState(): DesktopFeedTreeState =
    LocalDesktopFeedTreeState.current ?: remember { DesktopFeedTreeState() }

internal class DesktopFeedTreeState(
    collapsedSections: Set<String> = emptySet(),
    private val persistCollapsedSections: (Set<String>) -> Unit = {},
) {
    var collapsedSectionIds by mutableStateOf(collapsedSections)
        private set
    var focusedKey by mutableStateOf<String?>(null)
    private var hasReceivedContent = false

    fun isExpanded(sectionId: String): Boolean = sectionId !in collapsedSectionIds

    fun setExpanded(sectionId: String, expanded: Boolean) {
        val updated = if (expanded) collapsedSectionIds - sectionId else collapsedSectionIds + sectionId
        if (updated != collapsedSectionIds) {
            collapsedSectionIds = updated
            persistCollapsedSections(updated)
        }
    }

    fun reconcile(projection: DesktopFeedTreeProjection) {
        // The viewmodel emits an empty initial drawer before the database flows load.
        if (!hasReceivedContent && projection.sections.isEmpty() && projection.pinned.isEmpty()) return
        hasReceivedContent = true
        val sectionIds = projection.sections.mapTo(mutableSetOf()) { it.id }
        val remaining = collapsedSectionIds intersect sectionIds
        if (remaining != collapsedSectionIds) {
            collapsedSectionIds = remaining
            persistCollapsedSections(remaining)
        }
        val validKeys = sectionIds + projection.sourceIds.map { "feed:$it" } + setOf("timeline", "read", "bookmarks")
        if (focusedKey !in validKeys) focusedKey = null
    }
}

internal data class DesktopFeedTreeProjection(
    val pinned: List<DrawerItem.DrawerFeedSource>,
    val sections: List<DesktopFeedTreeSection>,
) {
    val sourceIds: Set<String>
        get() = (pinned + sections.flatMap { it.members }).mapTo(mutableSetOf()) { it.feedSource.id }
}

internal data class DesktopFeedTreeSection(
    val id: String,
    val categoryWrapper: FeedSourceCategoryWrapper,
    val members: List<DrawerItem.DrawerFeedSource>,
    val visibleFeeds: List<DrawerItem.DrawerFeedSource>,
    val unreadCount: Long,
    val position: Int,
)

internal fun DesktopFeedTreeSection.reorderVisibleFeeds(
    fromIndex: Int,
    insertionIndex: Int,
): List<DrawerItem.DrawerFeedSource> {
    if (fromIndex !in visibleFeeds.indices || insertionIndex !in 0..visibleFeeds.size) return members
    val reordered = visibleFeeds.toMutableList()
    val moved = reordered.removeAt(fromIndex)
    reordered.add(if (fromIndex < insertionIndex) insertionIndex - 1 else insertionIndex, moved)
    val iterator = reordered.iterator()
    val visibleIds = visibleFeeds.mapTo(mutableSetOf()) { it.feedSource.id }
    return members.map { if (it.feedSource.id in visibleIds) iterator.next() else it }
}
