package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.ArticleExportFilter
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.csv.CsvInput
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.createCsvInput
import com.prof18.feedflow.shared.test.createCsvOutput
import com.prof18.feedflow.shared.test.createFailingCsvOutput
import com.prof18.feedflow.shared.test.createOpmlInput
import com.prof18.feedflow.shared.test.createOpmlOutput
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ImportExportFileIntegrationJvmTest : KoinTestBase() {
    private val viewModel: ImportExportViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()

    @Test
    fun `OPML file import persists categorized and uncategorized feeds and export reparses them`() =
        runTest(testDispatcher) {
            val input = createOpmlInput(
                """<?xml version="1.0" encoding="UTF-8"?>
                    <opml version="1.0"><head><title>Fixture</title></head><body>
                    <outline text="Tech" title="Tech">
                      <outline type="rss" text="Alpha" title="Alpha" xmlUrl="https://example.com/alpha.xml"/>
                    </outline>
                    <outline type="rss" text="Beta" title="Beta" xmlUrl="https://example.com/beta.xml"/>
                    </body></opml>
                """.trimIndent(),
            )

            viewModel.importFeed(input)
            advanceUntilIdle()

            val success = assertIs<FeedImportExportState.ImportSuccess>(viewModel.importExportState.value)
            assertTrue(success.notValidFeedSources.isEmpty())
            assertTrue(success.feedSourceWithError.isEmpty())
            val sources = databaseHelper.getFeedSources().associateBy { it.url }
            assertEquals(setOf("https://example.com/alpha.xml", "https://example.com/beta.xml"), sources.keys)
            assertEquals("Alpha", sources.getValue("https://example.com/alpha.xml").title)
            assertEquals("Tech", sources.getValue("https://example.com/alpha.xml").category?.title)
            assertEquals("Beta", sources.getValue("https://example.com/beta.xml").title)
            assertEquals(null, sources.getValue("https://example.com/beta.xml").category)

            val output = createOpmlOutput()
            viewModel.exportFeed(output)
            advanceUntilIdle()
            assertEquals(FeedImportExportState.ExportSuccess, viewModel.importExportState.value)

            databaseHelper.deleteAll()
            viewModel.importFeed(OpmlInput(output.file))
            advanceUntilIdle()
            assertIs<FeedImportExportState.ImportSuccess>(viewModel.importExportState.value)
            val reparsed = databaseHelper.getFeedSources().associateBy { it.url }
            assertEquals(sources.keys, reparsed.keys)
            assertEquals("Alpha", reparsed.getValue("https://example.com/alpha.xml").title)
            assertEquals("Tech", reparsed.getValue("https://example.com/alpha.xml").category?.title)
            assertEquals("Beta", reparsed.getValue("https://example.com/beta.xml").title)
            assertEquals(null, reparsed.getValue("https://example.com/beta.xml").category)
        }

    @Test
    fun `malformed OPML file maps to invalid state without changing stored feeds`() = runTest(testDispatcher) {
        val existing = feedSource("existing", "Existing", "https://example.com/existing.xml")
        databaseHelper.insertFeedSourceWithCategory(existing)
        val before = databaseHelper.getFeedSources()

        viewModel.importFeed(createOpmlInput("<opml><body><outline"))
        advanceUntilIdle()

        assertEquals(FeedImportExportState.InvalidOpml, viewModel.importExportState.value)
        assertEquals(before, databaseHelper.getFeedSources())
    }

    @Test
    fun `CSV file round trip preserves exported article fields and filter subsets`() = runTest(testDispatcher) {
        val category = FeedSourceCategory(id = "cat-1", title = "Technology")
        val source = feedSource("source-1", "Feed", "https://example.com/feed.xml", category)
        databaseHelper.insertFeedSourceWithCategory(source)
        databaseHelper.addBlockedWord("Quoted")
        val fixture = createCsvInput(
            """url_hash,url,title,subtitle,image_url,feed_source_id,is_read,is_bookmarked,pub_date,comments_url,notification_sent,is_blocked
                |article-1,https://example.com/article/1,"Quoted, title
                |second line",,,source-1,true,true,1700000000123,https://example.com/comments?a=1&b=2,true,true
                |article-2,https://example.com/article/2,Unread unbookmarked,Subtitle,https://example.com/image.png,source-1,false,false,,,false,false
                |
            """.trimMargin(),
        )
        viewModel.importArticles(fixture)
        advanceUntilIdle()
        assertEquals(FeedImportExportState.ArticleImportSuccess, viewModel.importExportState.value)

        val allOutput = createCsvOutput()
        viewModel.exportArticles(allOutput, ArticleExportFilter.All)
        advanceUntilIdle()
        assertEquals(FeedImportExportState.ArticleExportSuccess, viewModel.importExportState.value)
        val csv = allOutput.file.readText(Charsets.UTF_8)
        assertEquals(
            "url_hash,url,title,subtitle,image_url,feed_source_id,is_read,is_bookmarked," +
                "pub_date,comments_url,notification_sent,is_blocked",
            csv.lineSequence().first(),
        )
        assertTrue(csv.contains("\"Quoted, title\nsecond line\""))

        databaseHelper.deleteAll()
        databaseHelper.insertFeedSourceWithCategory(source)
        viewModel.importArticles(CsvInput(allOutput.file))
        advanceUntilIdle()
        assertEquals(FeedImportExportState.ArticleImportSuccess, viewModel.importExportState.value)

        val imported = databaseHelper.getFeedItemsForExport(ArticleExportFilter.All).associateBy { it.url_hash }
        assertEquals(setOf("article-1", "article-2"), imported.keys)
        val quoted = imported.getValue("article-1")
        assertEquals("https://example.com/article/1", quoted.url)
        assertEquals("Quoted, title\nsecond line", quoted.title)
        assertEquals(null, quoted.subtitle)
        assertEquals(null, quoted.image_url)
        assertEquals("source-1", quoted.feed_source_id)
        assertEquals(true, quoted.is_read)
        assertEquals(true, quoted.is_bookmarked)
        assertEquals(1_700_000_000_123L, quoted.pub_date)
        assertEquals("https://example.com/comments?a=1&b=2", quoted.comments_url)
        assertEquals(true, quoted.notification_sent)
        assertEquals(true, quoted.is_blocked)
        val unread = imported.getValue("article-2")
        assertEquals("Subtitle", unread.subtitle)
        assertEquals("https://example.com/image.png", unread.image_url)
        assertEquals(false, unread.is_read)
        assertEquals(false, unread.is_bookmarked)
        assertEquals(null, unread.pub_date)
        assertEquals(null, unread.comments_url)
        assertEquals(false, unread.notification_sent)
        assertEquals(false, unread.is_blocked)
        assertEquals("https://example.com/article/2", unread.url)
        assertEquals("Unread unbookmarked", unread.title)
        assertEquals("source-1", unread.feed_source_id)

        val readOutput = createCsvOutput()
        viewModel.exportArticles(readOutput, ArticleExportFilter.Read)
        advanceUntilIdle()
        assertEquals(FeedImportExportState.ArticleExportSuccess, viewModel.importExportState.value)
        assertTrue(readOutput.file.readText(Charsets.UTF_8).contains("article-1"))
        assertFalse(readOutput.file.readText(Charsets.UTF_8).contains("article-2"))

        val unreadOutput = createCsvOutput()
        viewModel.exportArticles(unreadOutput, ArticleExportFilter.Unread)
        advanceUntilIdle()
        assertEquals(FeedImportExportState.ArticleExportSuccess, viewModel.importExportState.value)
        assertFalse(unreadOutput.file.readText(Charsets.UTF_8).contains("article-1"))
        assertTrue(unreadOutput.file.readText(Charsets.UTF_8).contains("article-2"))

        val bookmarkedOutput = createCsvOutput()
        viewModel.exportArticles(bookmarkedOutput, ArticleExportFilter.Bookmarked)
        advanceUntilIdle()
        assertEquals(FeedImportExportState.ArticleExportSuccess, viewModel.importExportState.value)
        assertTrue(bookmarkedOutput.file.readText(Charsets.UTF_8).contains("article-1"))
        assertFalse(bookmarkedOutput.file.readText(Charsets.UTF_8).contains("article-2"))
    }

    @Test
    fun `invalid CSV and failed export report errors without changing existing articles`() = runTest(testDispatcher) {
        val source = feedSource("source-1", "Feed", "https://example.com/feed.xml")
        databaseHelper.insertFeedSourceWithCategory(source)
        databaseHelper.insertFeedItems(
            listOf(buildFeedItem("existing", "Existing", 1L, source)),
            lastSyncTimestamp = 0,
        )
        val before = databaseHelper.getFeedItemsForExport(ArticleExportFilter.All)

        viewModel.importArticles(createCsvInput("url_hash,title\nother,Missing required columns"))
        advanceUntilIdle()
        assertEquals(FeedImportExportState.Error, viewModel.importExportState.value)
        assertEquals(before, databaseHelper.getFeedItemsForExport(ArticleExportFilter.All))

        viewModel.exportArticles(createFailingCsvOutput(), ArticleExportFilter.All)
        advanceUntilIdle()
        assertEquals(FeedImportExportState.Error, viewModel.importExportState.value)
        assertEquals(before, databaseHelper.getFeedItemsForExport(ArticleExportFilter.All))
    }

    private fun feedSource(
        id: String,
        title: String,
        url: String,
        category: FeedSourceCategory? = null,
    ) = FeedSourceGenerator.feedSource(
        id = id,
        url = url,
        title = title,
        category = category,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = null,
    )
}
