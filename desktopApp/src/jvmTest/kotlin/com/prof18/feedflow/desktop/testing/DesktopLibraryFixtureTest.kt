package com.prof18.feedflow.desktop.testing

import com.prof18.feedflow.desktop.ui.components.SidebarKey
import com.prof18.feedflow.desktop.ui.components.buildSidebarRows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DesktopLibraryFixtureTest {
    @Test
    fun `representative library preserves source and category identity across loads`() {
        val first = DesktopLibraryFixture.load()
        val second = DesktopLibraryFixture.load()

        assertEquals(first, second)
        assertEquals(expected = 5, actual = first.categories.size)
        assertEquals(
            (1..FIXTURE_SOURCE_COUNT).map { "fixture-source-$it" }.toSet(),
            first.sources.map { it.feedSource.id }.toSet(),
        )
        assertEquals(1, first.sources.count { it.feedSource.category == null })
        assertTrue(first.sources.any { it.feedSource.title.length > LONG_LABEL_LENGTH })
    }

    @Test
    fun `pinned and zero unread sources stay reachable without duplicated rows`() {
        val library = DesktopLibraryFixture.load()
        val rows = buildSidebarRows(
            navDrawerState = library.drawerState(),
            isUnreadCountHidden = false,
            timelineTitle = "Timeline",
            readTitle = "Read",
            bookmarksTitle = "Bookmarks",
            uncategorizedTitle = "Uncategorized",
        )
        val feedRows = rows.filter { it.entry.key is SidebarKey.Feed }

        assertEquals(library.sources.size, feedRows.size)
        assertEquals(library.sources.size, feedRows.map { it.entry.key }.distinct().size)
        val pinned = library.sources.single { it.feedSource.isPinned }
        assertTrue(feedRows.single { it.entry.key == SidebarKey.Feed(pinned.feedSource.id) }.entry.isPinned)
        assertTrue(library.sources.any { it.unreadCount == 0L })
        library.sources.filter { it.unreadCount == 0L }.forEach { source ->
            assertTrue(feedRows.any { it.entry.key == SidebarKey.Feed(source.feedSource.id) })
        }
        library.categories.forEach { category ->
            val group = rows.single { it.entry.key == SidebarKey.Category(category.id) }
            val children = library.sources.filter { it.feedSource.category == category }
            assertEquals(children.size, group.entry.childCount)
            assertEquals(children.sumOf { it.unreadCount }, group.unreadCount)
        }
        val timeline = rows.single { it.entry.key == SidebarKey.Timeline }
        assertEquals(library.sources.sumOf { it.unreadCount }, timeline.unreadCount)
    }

    @Test
    fun `fixture rejects duplicate identities instead of hiding a source`() {
        val opml = """
            <opml><body>
              <outline text="One" xmlUrl="http://127.0.0.1:18765/one" fixtureId="duplicate" />
              <outline text="Two" xmlUrl="http://127.0.0.1:18765/two" fixtureId="duplicate" />
            </body></opml>
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> { DesktopLibraryFixture.read(opml.byteInputStream()) }
    }

    @Test
    fun `fixture rejects an invalid unread count`() {
        val opml = """
            <opml><body>
              <outline text="One" xmlUrl="http://127.0.0.1:18765/one" fixtureId="one" unread="-1" />
            </body></opml>
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> { DesktopLibraryFixture.read(opml.byteInputStream()) }
    }
}

private const val FIXTURE_SOURCE_COUNT = 16
private const val LONG_LABEL_LENGTH = 100
