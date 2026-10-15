package com.prof18.feedflow.shared

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.model.FeedItemImportData
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchIndexImportTest : KoinTestBase() {
    private val databaseHelper: DatabaseHelper by inject()
    private val sqlDriver: SqlDriver by inject()

    @Test
    fun `importing the same feed item twice keeps one searchable index row and imported flags`() =
        runTest(testDispatcher) {
            val item = FeedItemGenerator.feedItem(id = "imported-item", isRead = true, isBookmarked = true)
            insertSource(item.feedSource.id, item.feedSource.title)
            val importedItem = item.toImportData()

            databaseHelper.importFeedItemsFromCsv(listOf(importedItem), setOf(item.feedSource.id))
            databaseHelper.importFeedItemsFromCsv(listOf(importedItem), setOf(item.feedSource.id))

            val searchResults = databaseHelper.search("Feed item").first()
            assertEquals(listOf("imported-item"), searchResults.map { it.url_hash })
            assertEquals(1L, feedSearchRowCount())
            assertEquals(true, searchResults.single().is_read)
            assertEquals(true, searchResults.single().is_bookmarked)
        }

    @Test
    fun `reimporting changed feed item title replaces its searchable text`() = runTest(testDispatcher) {
        val item = FeedItemGenerator.feedItem(id = "renamed-item", title = "EarlierImportToken")
        insertSource(item.feedSource.id, item.feedSource.title)

        databaseHelper.importFeedItemsFromCsv(listOf(item.toImportData()), setOf(item.feedSource.id))
        databaseHelper.importFeedItemsFromCsv(
            listOf(item.copy(title = "CurrentImportToken").toImportData()),
            setOf(item.feedSource.id),
        )

        assertEquals(emptyList(), databaseHelper.search("EarlierImportToken").first())
        assertEquals(listOf("renamed-item"), databaseHelper.search("CurrentImportToken").first().map { it.url_hash })
        assertEquals(1L, feedSearchRowCount())
    }

    @Test
    fun `search ignores orphan index documents with a nonmatching docid`() = runTest(testDispatcher) {
        val item = FeedItemGenerator.feedItem(id = "orphan-item", title = "SharedSearchToken CurrentOnlyToken")
        insertSource(item.feedSource.id, item.feedSource.title)
        sqlDriver.execute(
            identifier = null,
            sql = """
                INSERT INTO feed_search(docid, url_hash, title, subtitle)
                VALUES (999, 'orphan-item', 'SharedSearchToken OrphanOnlyToken', NULL)
            """.trimIndent(),
            parameters = 0,
            binders = null,
        ).value

        databaseHelper.insertFeedItems(listOf(item), lastSyncTimestamp = 0)

        assertEquals(listOf("orphan-item"), databaseHelper.search("SharedSearchToken").first().map { it.url_hash })
        assertEquals(emptyList(), databaseHelper.search("OrphanOnlyToken").first())
    }

    @Test
    fun `reimport repairs an orphan docid that collides with a reused feed item rowid`() =
        runTest(testDispatcher) {
            val firstItem = FeedItemGenerator.feedItem(id = "first-row", title = "First row")
            val item = FeedItemGenerator.feedItem(
                id = "reused-rowid-item",
                title = "EarlierImportToken",
                feedSource = firstItem.feedSource,
            )
            insertSource(item.feedSource.id, item.feedSource.title)
            databaseHelper.insertFeedItems(listOf(firstItem, item), lastSyncTimestamp = 0)
            sqlDriver.execute(
                identifier = null,
                sql = "DELETE FROM feed_item WHERE url_hash = 'first-row'",
                parameters = 0,
                binders = null,
            ).value
            sqlDriver.execute(
                identifier = null,
                sql = """
                    INSERT INTO feed_search(docid, url_hash, title, subtitle)
                    VALUES (1, 'reused-rowid-item', 'EarlierImportToken', NULL)
                """.trimIndent(),
                parameters = 0,
                binders = null,
            ).value

            databaseHelper.importFeedItemsFromCsv(
                listOf(item.copy(title = "CurrentImportToken").toImportData()),
                setOf(item.feedSource.id),
            )

            assertEquals(
                listOf("reused-rowid-item"),
                databaseHelper.search("CurrentImportToken").first().map { it.url_hash },
            )
            assertEquals(emptyList(), databaseHelper.search("EarlierImportToken").first())
            assertEquals(1L, feedSearchRowCount())
            val indexedRowIds = sqlDriver.executeQuery(
                identifier = null,
                sql = """
                    SELECT feed_item.rowid, feed_search.docid
                    FROM feed_item
                    INNER JOIN feed_search
                      ON feed_search.docid = feed_item.rowid
                     AND feed_search.url_hash = feed_item.url_hash
                    WHERE feed_item.url_hash = 'reused-rowid-item'
                      AND feed_search.title = 'CurrentImportToken'
                """.trimIndent(),
                mapper = { cursor ->
                    val rowIds = buildList {
                        while (cursor.next().value) {
                            add((cursor.getLong(0) ?: 0L) to (cursor.getLong(1) ?: 0L))
                        }
                    }
                    QueryResult.Value(rowIds)
                },
                parameters = 0,
                binders = null,
            ).value
            val (rowId, docId) = indexedRowIds.single()
            assertEquals(rowId, docId)
        }

    private suspend fun insertSource(id: String, title: String) {
        databaseHelper.insertFeedSource(
            listOf(
                ParsedFeedSource(
                    id = id,
                    url = "https://example.com/$id.xml",
                    title = title,
                    category = null,
                    logoUrl = null,
                    websiteUrl = null,
                ),
            ),
        )
    }

    private fun com.prof18.feedflow.core.model.FeedItem.toImportData() = FeedItemImportData(
        urlHash = id,
        url = url,
        title = title,
        subtitle = subtitle,
        imageUrl = imageUrl,
        feedSourceId = feedSource.id,
        isRead = isRead,
        isBookmarked = isBookmarked,
        pubDateMillis = pubDateMillis,
        commentsUrl = commentsUrl,
        notificationSent = false,
        isBlocked = false,
        contentFetched = false,
    )

    private fun feedSearchRowCount(): Long = sqlDriver.executeQuery(
        identifier = null,
        sql = "SELECT COUNT(*) FROM feed_search",
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0) ?: 0L)
        },
        parameters = 0,
        binders = null,
    ).value
}
