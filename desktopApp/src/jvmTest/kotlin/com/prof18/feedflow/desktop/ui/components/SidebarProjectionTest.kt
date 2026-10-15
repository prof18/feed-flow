package com.prof18.feedflow.desktop.ui.components

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.NavDrawerState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals

class SidebarProjectionTest {

    @Test
    fun `mixed category map null wrapper projects uncategorized feed group`() {
        val uncategorized = feed("loose", title = "Loose Feed")
        val category = FeedSourceCategory("news", "News", position = 1)
        val categorized = feed("news-feed", title = "News Feed", category = category)
        val rows = project(
            NavDrawerState(
                categories = persistentListOf(DrawerItem.DrawerCategory(category, unreadCount = 2)),
                feedSourcesByCategory = persistentMapOf(
                    FeedSourceCategoryWrapper(null) to listOf(
                        drawerFeed(uncategorized, unreadCount = 3),
                    ),
                    FeedSourceCategoryWrapper(category) to listOf(
                        drawerFeed(categorized, unreadCount = 4),
                    ),
                ),
                feedSourcesWithoutCategory = persistentListOf(),
            ),
        )

        val group = rows.single { it.entry.key == SidebarKey.Uncategorized }
        val child = rows.single { it.entry.key == SidebarKey.Feed("loose") }
        assertEquals(1, group.entry.childCount)
        assertEquals("Uncategorized (3)", group.entry.label)
        assertEquals("uncategorized", child.parentSection)
        assertEquals(FeedFilter.Uncategorized, group.filter)
    }

    @Test
    fun `uncategorized group respects configured position among categories`() {
        val first = FeedSourceCategory("first", "First", position = 1)
        val second = FeedSourceCategory("second", "Second", position = 3)
        val drawer = NavDrawerState(
            categories = persistentListOf(
                DrawerItem.DrawerCategory(second, unreadCount = 0),
                DrawerItem.DrawerCategory(first, unreadCount = 0),
            ),
            feedSourcesWithoutCategory = persistentListOf(drawerFeed(feed("loose"), 1)),
            uncategorizedPosition = 2,
        )

        val topLevelKeys = project(drawer)
            .filter { it.parentSection == null }
            .map { it.entry.key }
            .filter { it is SidebarKey.Category || it == SidebarKey.Uncategorized }

        assertEquals(
            listOf(
                SidebarKey.Category("first"),
                SidebarKey.Uncategorized,
                SidebarKey.Category("second"),
            ),
            topLevelKeys,
        )
    }

    @Test
    fun `empty categories remain represented`() {
        val category = FeedSourceCategory("empty", "Empty", position = 1)
        val rows = project(
            NavDrawerState(
                categories = persistentListOf(DrawerItem.DrawerCategory(category, unreadCount = 0)),
            ),
        )

        val row = rows.single { it.entry.key == SidebarKey.Category("empty") }
        assertEquals(0, row.entry.childCount)
        assertEquals(FeedFilter.Category(category), row.filter)
    }

    @Test
    fun `pinned feed appears once without changing category child or unread counts`() {
        val category = FeedSourceCategory("news", "News", position = 1)
        val pinned = feed("pinned", title = "Pinned News", category = category, isPinned = true)
        val rows = project(
            NavDrawerState(
                categories = persistentListOf(DrawerItem.DrawerCategory(category, unreadCount = 9)),
                pinnedFeedSources = persistentListOf(drawerFeed(pinned, unreadCount = 5)),
                feedSourcesByCategory = persistentMapOf(
                    FeedSourceCategoryWrapper(category) to listOf(drawerFeed(pinned, unreadCount = 5)),
                ),
            ),
        )

        assertEquals(1, rows.count { it.entry.key == SidebarKey.Feed("pinned") })
        val categoryRow = rows.single { it.entry.key == SidebarKey.Category("news") }
        assertEquals(1, categoryRow.entry.childCount)
        assertEquals("News (9)", categoryRow.entry.label)
    }

    @Test
    fun `timeline unread label comes from drawer timeline count`() {
        val rows = project(
            NavDrawerState(timeline = persistentListOf(DrawerItem.Timeline(unreadCount = 17))),
        )

        assertEquals("Timeline (17)", rows.single { it.entry.key == SidebarKey.Timeline }.entry.label)
    }

    private fun project(navDrawerState: NavDrawerState): List<SidebarRow> =
        buildSidebarRows(
            navDrawerState = navDrawerState,
            isUnreadCountHidden = false,
            timelineTitle = "Timeline",
            readTitle = "Read",
            bookmarksTitle = "Bookmarks",
            uncategorizedTitle = "Uncategorized",
        )

    private fun drawerFeed(feedSource: FeedSource, unreadCount: Long): DrawerItem.DrawerFeedSource =
        DrawerItem.DrawerFeedSource(feedSource, unreadCount)

    private fun feed(
        id: String,
        title: String = id,
        category: FeedSourceCategory? = null,
        isPinned: Boolean = false,
    ): FeedSource = FeedSource(
        id = id,
        url = "https://$id.example/feed.xml",
        title = title,
        category = category,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = null,
        fetchFailed = false,
        articleOpenMode = ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = isPinned,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
    )
}
