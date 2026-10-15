package com.prof18.feedflow.desktop.testing

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.NavDrawerState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import org.w3c.dom.Element
import java.io.InputStream
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory

internal object DesktopLibraryFixture {
    fun load(): DesktopLibrary = read(
        requireNotNull(javaClass.classLoader.getResourceAsStream("fixtures/library.opml")),
    )

    fun read(input: InputStream): DesktopLibrary {
        val parser = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }.newDocumentBuilder()
        val document = input.use(parser::parse)
        val body = document.getElementsByTagName("body").item(0) as? Element
        requireNotNull(body) { "Fixture must contain an OPML body" }
        val categories = mutableListOf<FeedSourceCategory>()
        val sources = mutableListOf<DrawerItem.DrawerFeedSource>()
        body.outlines().forEach { outline ->
            if (outline.hasAttribute("xmlUrl")) {
                sources.add(outline.source(category = null))
            } else {
                val category = FeedSourceCategory(
                    id = outline.fixtureId(),
                    title = outline.getAttribute("text"),
                    position = categories.size + 1,
                )
                categories.add(category)
                sources.addAll(outline.outlines().map { it.source(category) })
            }
        }
        require(categories.map { it.id }.distinct().size == categories.size) { "Duplicate category identity" }
        require(sources.map { it.feedSource.id }.distinct().size == sources.size) { "Duplicate source identity" }
        return DesktopLibrary(categories, sources)
    }

    private fun Element.outlines(): List<Element> = (0 until childNodes.length)
        .mapNotNull { childNodes.item(it) as? Element }
        .filter { it.tagName == "outline" }

    private fun Element.fixtureId(): String = getAttribute("fixtureId").also {
        require(it.isNotBlank()) { "Missing stable fixture identity" }
    }

    private fun Element.source(category: FeedSourceCategory?): DrawerItem.DrawerFeedSource {
        val url = getAttribute("xmlUrl")
        require(URI(url).host in setOf("127.0.0.1", "localhost")) { "Fixture sources must be loopback-only" }
        val unread = getAttribute("unread").ifBlank { "0" }.toLong()
        require(unread >= 0) { "Unread counts must be nonnegative" }
        return DrawerItem.DrawerFeedSource(
            feedSource = FeedSource(
                id = fixtureId(),
                url = url,
                title = getAttribute("text"),
                category = category,
                lastSyncTimestamp = null,
                logoUrl = null,
                websiteUrl = null,
                fetchFailed = false,
                articleOpenMode = ArticleOpenMode.DEFAULT,
                isHiddenFromTimeline = false,
                isPinned = getAttribute("pinned").toBoolean(),
                isNotificationEnabled = false,
                isHideImagesEnabled = false,
            ),
            unreadCount = unread,
        )
    }
}

internal data class DesktopLibrary(
    val categories: List<FeedSourceCategory>,
    val sources: List<DrawerItem.DrawerFeedSource>,
) {
    fun drawerState(): NavDrawerState = NavDrawerState(
        timeline = listOf(DrawerItem.Timeline(sources.sumOf { it.unreadCount })).toImmutableList(),
        categories = categories.map { category ->
            DrawerItem.DrawerCategory(
                category,
                sources.filter { it.feedSource.category == category }.sumOf { it.unreadCount },
            )
        }.toImmutableList(),
        pinnedFeedSources = sources.filter { it.feedSource.isPinned }.toImmutableList(),
        feedSourcesWithoutCategory = sources.filter { it.feedSource.category == null }.toImmutableList(),
        feedSourcesByCategory = sources.filter { it.feedSource.category != null }
            .groupBy { DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper(it.feedSource.category) }
            .toImmutableMap(),
    )
}
