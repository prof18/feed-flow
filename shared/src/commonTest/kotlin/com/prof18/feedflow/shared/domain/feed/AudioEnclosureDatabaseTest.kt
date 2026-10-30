package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.ArticleExportFilter
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemImportData
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.shared.test.DatabaseTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudioEnclosureDatabaseTest : DatabaseTestBase() {

    private val source = FeedSourceGenerator.feedSource(id = "audio-source")
    private val otherSource = FeedSourceGenerator.feedSource(id = "other-source")

    @Test
    fun `audio artwork prefers episode image then validated feed logo`() = runTest(testDispatcher) {
        val logoSource = source.copy(logoUrl = "https://example.com/show.jpg")
        database.insertFeedSourceWithCategory(logoSource)
        val artwork = "https://example.com/episode.jpg?token=a&part=1"
        database.insertFeedItems(
            listOf(
                buildFeedItem("with-artwork", "Episode", 10L, logoSource).copy(imageUrl = artwork),
                buildFeedItem("without-artwork", "Episode", 10L, logoSource).copy(imageUrl = null),
                buildFeedItem("unsafe-artwork", "Episode", 10L, logoSource).copy(imageUrl = "file:///art.jpg"),
            ),
            lastSyncTimestamp = 0,
        )
        assertEquals(artwork, database.getFeedItemAudioArtwork("with-artwork"))
        assertEquals(logoSource.logoUrl, database.getFeedItemAudioArtwork("without-artwork"))
        assertEquals(logoSource.logoUrl, database.getFeedItemAudioArtwork("unsafe-artwork"))
        assertNull(database.getFeedItemAudioArtwork("missing"))
    }

    @Test
    fun `duplicate enrichment rotates audio without replacing article fields or flags`() = runTest(testDispatcher) {
        database.insertFeedSourceWithCategory(source)
        val original = buildFeedItem("episode", "Original title", 10L, source)
            .copy(content = "Original content")
        database.insertFeedItems(listOf(original), lastSyncTimestamp = 0)
        database.updateReadStatus(FeedItemId(original.id), isRead = true)
        database.updateBookmarkStatus(FeedItemId(original.id), isBookmarked = true)
        database.insertFeedItems(
            listOf(
                original.copy(
                    title = "Changed",
                    content = "Changed",
                    url = "https://example.com/changed",
                    audioUrl = SIGNED_AUDIO,
                ),
            ),
            lastSyncTimestamp = 1,
        )
        assertEquals(SIGNED_AUDIO, database.getFeedItemAudioUrl(original.id))
        val row = database.getFeedItems(FeedFilter.Timeline, 10, true, FeedOrder.NEWEST_FIRST).single()
        assertEquals(original.title, row.title)
        assertEquals(original.url, row.url)
        assertEquals(original.pubDateMillis, row.pub_date)
        assertTrue(row.is_read)
        assertTrue(row.is_bookmarked)
        assertEquals(original.content, database.getFeedItemContent(original.id))

        val rotated = "https://example.com/episode.mp3?token=rotated"
        database.insertFeedItems(listOf(original.copy(audioUrl = rotated)), lastSyncTimestamp = 2)
        assertEquals(rotated, database.getFeedItemAudioUrl(original.id))
        database.insertFeedItems(listOf(original), lastSyncTimestamp = 3)
        database.insertFeedItems(listOf(original.copy(audioUrl = "file:///episode.mp3")), lastSyncTimestamp = 4)
        assertEquals(rotated, database.getFeedItemAudioUrl(original.id))
        assertEquals(1, database.getFeedItemsForExport(ArticleExportFilter.All).size)
        assertNull(database.getFeedItemAudioUrl("missing"))
    }

    @Test
    fun `source collision and deleted items cannot gain audio`() = runTest(testDispatcher) {
        database.insertFeedSourceWithCategory(source)
        database.insertFeedSourceWithCategory(otherSource)
        val item = buildFeedItem("episode", "Episode", 10L, source)
        database.insertFeedItems(listOf(item), lastSyncTimestamp = 0)
        database.insertFeedItems(listOf(item.copy(feedSource = otherSource, audioUrl = SIGNED_AUDIO)), 1)
        assertNull(database.getFeedItemAudioUrl(item.id))
        database.deleteOldFeedItems(timeThreshold = 20L, feedFilter = FeedFilter.Timeline)
        database.insertFeedItems(listOf(item.copy(audioUrl = SIGNED_AUDIO)), 2)
        assertNull(database.getFeedItemAudioUrl(item.id))
        assertTrue(database.getFeedItemsForExport(ArticleExportFilter.All).isEmpty())
    }

    @Test
    fun `CSV replacement preserves only same source local audio`() = runTest(testDispatcher) {
        database.insertFeedSourceWithCategory(source)
        database.insertFeedSourceWithCategory(otherSource)
        val item = buildFeedItem("episode", "Episode", 10L, source).copy(audioUrl = SIGNED_AUDIO)
        database.insertFeedItems(listOf(item), 0)
        val exported = database.getFeedItemsForExport(ArticleExportFilter.All).single()
        val imported = FeedItemImportData(
            urlHash = exported.url_hash,
            url = exported.url,
            title = "CSV updated title",
            subtitle = exported.subtitle,
            imageUrl = exported.image_url,
            feedSourceId = exported.feed_source_id,
            isRead = true,
            isBookmarked = true,
            pubDateMillis = exported.pub_date,
            commentsUrl = exported.comments_url,
            notificationSent = true,
            isBlocked = false,
            contentFetched = false,
        )
        val sources = setOf(source.id, otherSource.id)
        database.importFeedItemsFromCsv(listOf(imported), sources)
        assertEquals(SIGNED_AUDIO, database.getFeedItemAudioUrl(item.id))
        val updated = database.getFeedItemsForExport(ArticleExportFilter.All).single()
        assertEquals(imported.title, updated.title)
        assertTrue(updated.is_read)
        assertTrue(updated.is_bookmarked)
        database.importFeedItemsFromCsv(listOf(imported.copy(urlHash = "fresh")), sources)
        assertNull(database.getFeedItemAudioUrl("fresh"))
        database.importFeedItemsFromCsv(listOf(imported.copy(feedSourceId = otherSource.id)), sources)
        assertNull(database.getFeedItemAudioUrl(item.id))
    }

    private companion object {
        const val SIGNED_AUDIO = "https://example.com/episode.mp3?token=a%2Bb&part=1#track"
    }
}
