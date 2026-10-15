package com.prof18.feedflow.desktop.home

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.desktop.testing.DesktopLibraryFixture
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopFeedTreeTest {
    @Test
    fun `all representative fixture identities occur once in both tree consumers`() {
        val library = DesktopLibraryFixture.load()
        val tree = projectDesktopFeedTree(library.drawerState())
        val displayed = tree.pinned + tree.sections.flatMap { it.visibleFeeds }
        assertEquals(library.sources.size, displayed.size)
        assertEquals(library.sources.map { it.feedSource.id }.toSet(), tree.sourceIds)
        library.categories.forEach { category ->
            val section = tree.sections.single { it.categoryWrapper.feedSourceCategory?.id == category.id }
            val members = library.sources.filter { it.feedSource.category?.id == category.id }
            assertEquals(members.size, section.members.size)
            assertEquals(members.sumOf { it.unreadCount }, section.unreadCount)
        }
    }

    @Test
    fun `sources occur once while pinned category membership and empty groups remain`() {
        val news = FeedSourceCategory("news", "News", position = 2)
        val empty = FeedSourceCategory("empty", "Empty", position = 3)
        val pinned = item("pinned", news)
        val failed = item("failed", news, failed = true)
        val loose = item("loose")
        val tree = projectDesktopFeedTree(
            NavDrawerState(
                pinnedFeedSources = persistentListOf(pinned, pinned),
                categories = persistentListOf(DrawerItem.DrawerCategory(empty, 0)),
                feedSourcesByCategory = persistentMapOf(
                    FeedSourceCategoryWrapper(news) to listOf(pinned, failed, failed),
                    FeedSourceCategoryWrapper(null) to listOf(loose),
                ),
                feedSourcesWithoutCategory = persistentListOf(loose),
            ),
        )
        val visibleIds = (tree.pinned + tree.sections.flatMap { it.visibleFeeds }).map { it.feedSource.id }
        assertEquals(setOf("pinned", "failed", "loose"), visibleIds.toSet())
        assertEquals(expected = 3, actual = visibleIds.size)
        assertEquals(2, tree.sections.single { it.categoryWrapper.feedSourceCategory?.id == "news" }.members.size)
        assertTrue(tree.sections.single { it.categoryWrapper.feedSourceCategory?.id == "empty" }.members.isEmpty())
    }

    @Test
    fun `reordering visible category feeds preserves pinned membership positions`() {
        val news = FeedSourceCategory("news", "News", position = 1)
        val first = item("first", news)
        val pinned = item("pinned", news)
        val last = item("last", news)
        val section = projectDesktopFeedTree(
            NavDrawerState(
                pinnedFeedSources = persistentListOf(pinned),
                feedSourcesByCategory = persistentMapOf(FeedSourceCategoryWrapper(news) to listOf(first, pinned, last)),
            ),
        ).sections.single()
        assertEquals(
            listOf("last", "pinned", "first"),
            section.reorderVisibleFeeds(0, 2).map { it.feedSource.id },
        )
        assertEquals(section.members, section.reorderVisibleFeeds(-1, 2))
    }

    @Test
    fun `new groups expand while an explicit collapse survives recreation`() {
        var saved = emptySet<String>()
        val state = DesktopFeedTreeState(persistCollapsedSections = { saved = it })
        assertTrue(state.isExpanded("category:news"))
        state.setExpanded("category:news", false)
        assertFalse(state.isExpanded("category:news"))
        val restored = DesktopFeedTreeState(saved)
        assertFalse(restored.isExpanded("category:news"))
        assertTrue(restored.isExpanded("category:new"))
        restored.setExpanded("category:news", true)
        assertTrue(restored.isExpanded("category:news"))
    }

    @Test
    fun `loaded tree prunes deleted collapse and focus without losing preference during startup`() {
        val news = FeedSourceCategory("news", "News", position = 1)
        val tree = projectDesktopFeedTree(
            NavDrawerState(
                categories = persistentListOf(DrawerItem.DrawerCategory(news, 0)),
            ),
        )
        var saved = setOf("category:news")
        val state = DesktopFeedTreeState(saved, persistCollapsedSections = { saved = it })
        state.reconcile(projectDesktopFeedTree(NavDrawerState()))
        assertEquals(setOf("category:news"), state.collapsedSectionIds)
        state.reconcile(tree)
        state.focusedKey = "category:news"
        state.reconcile(projectDesktopFeedTree(NavDrawerState()))
        assertTrue(state.collapsedSectionIds.isEmpty())
        assertTrue(saved.isEmpty())
        assertNull(state.focusedKey)
    }

    private fun item(id: String, category: FeedSourceCategory? = null, failed: Boolean = false) =
        DrawerItem.DrawerFeedSource(
            FeedSource(
                id = id,
                url = "https://$id.example/feed.xml",
                title = id,
                category = category,
                lastSyncTimestamp = null,
                logoUrl = null,
                websiteUrl = null,
                fetchFailed = failed,
                articleOpenMode = ArticleOpenMode.DEFAULT,
                isHiddenFromTimeline = false,
                isPinned = id == "pinned",
                isNotificationEnabled = false,
                isHideImagesEnabled = false,
            ),
            unreadCount = 0,
        )
}
