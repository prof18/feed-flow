package com.prof18.feedflow.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCacheInfo
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.db.FeedFlowDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabaseHelperRefreshPersistenceTest {

    @Test
    fun `a refresh saves items, timestamps and cache info together`() = withHelper { helper, _ ->
        helper.persistFeedRefresh(
            feedItems = listOf(item()),
            syncedFeedSourceIds = listOf(SOURCE_ID),
            lastSyncTimestamp = SYNC_TIMESTAMP,
            updatedCacheInfo = listOf(cacheInfo()),
            markFirstSyncItemsAsNotified = false,
        )

        assertTrue(helper.hasFeedItems())
        assertEquals(SYNC_TIMESTAMP, helper.getFeedSource(SOURCE_ID)?.lastSyncTimestamp)
        assertEquals("etag", helper.getFeedSourcesCacheInfo().single().etag)
    }

    @Test
    fun `a refresh with no items still writes timestamps and cache info and clears the failure flag`() =
        withHelper { helper, _ ->
            helper.setFeedFetchFailed(SOURCE_ID, true)

            helper.persistFeedRefresh(
                feedItems = emptyList(),
                syncedFeedSourceIds = listOf(SOURCE_ID),
                lastSyncTimestamp = SYNC_TIMESTAMP,
                updatedCacheInfo = listOf(cacheInfo()),
                markFirstSyncItemsAsNotified = false,
            )

            val source = helper.getFeedSource(SOURCE_ID)
            assertEquals(SYNC_TIMESTAMP, source?.lastSyncTimestamp)
            assertFalse(source?.fetchFailed ?: true)
            assertEquals(1, helper.getFeedSourcesCacheInfo().size)
        }

    @Test
    fun `a failed cache-info write rolls back items and timestamps`() = withHelper { helper, driver ->
        driver.failOn = { sql -> "feed_source_cache_info" in sql }

        val error = assertFailsWith<DatabaseTransactionException> {
            helper.persistFeedRefresh(
                feedItems = listOf(item()),
                syncedFeedSourceIds = listOf(SOURCE_ID),
                lastSyncTimestamp = SYNC_TIMESTAMP,
                updatedCacheInfo = listOf(cacheInfo()),
                markFirstSyncItemsAsNotified = false,
            )
        }
        driver.failOn = { false }

        assertIs<SQLiteException>(error.cause)
        assertFalse(helper.hasFeedItems())
        assertNull(helper.getFeedSource(SOURCE_ID)?.lastSyncTimestamp)
        assertTrue(helper.getFeedSourcesCacheInfo().isEmpty())
    }

    @Test
    fun `a failed transaction reports its label and elapsed time`() = withHelper { helper, driver ->
        driver.failOn = { true }

        val error = assertFailsWith<DatabaseTransactionException> {
            helper.updateLastSyncTimestamps(listOf(SOURCE_ID), SYNC_TIMESTAMP)
        }

        assertTrue(error.elapsedMs >= 0)
        assertEquals("unnamed", error.label)
        assertIs<SQLiteException>(error.cause)
    }

    private fun withHelper(block: suspend (DatabaseHelper, FailingDriver) -> Unit) {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { jdbc ->
            FeedFlowDB.Schema.create(jdbc)
            val driver = FailingDriver(jdbc)
            val helper = DatabaseHelper(driver, Dispatchers.Unconfined, Logger)
            runBlocking {
                helper.insertFeedSource(listOf(parsedSource()))
                block(helper, driver)
            }
        }
    }

    private class FailingDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
        var failOn: (String) -> Boolean = { false }

        override fun execute(
            identifier: Int?,
            sql: String,
            parameters: Int,
            binders: (SqlPreparedStatement.() -> Unit)?,
        ): QueryResult<Long> {
            if (failOn(sql)) throw SQLiteException("busy", SQLiteErrorCode.SQLITE_BUSY)
            return delegate.execute(identifier, sql, parameters, binders)
        }
    }

    private fun parsedSource() = ParsedFeedSource(
        id = SOURCE_ID,
        url = SOURCE_URL,
        title = "Title",
        category = null,
        logoUrl = null,
        websiteUrl = null,
    )

    private fun source() = FeedSource(
        id = SOURCE_ID,
        url = SOURCE_URL,
        title = "Title",
        category = null,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = null,
        fetchFailed = false,
        articleOpenMode = ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
    )

    private fun item() = FeedItem(
        id = "item-1",
        url = "https://fixture.test/post/1",
        title = "Item",
        subtitle = null,
        content = null,
        imageUrl = null,
        feedSource = source(),
        pubDateMillis = null,
        isRead = false,
        dateString = null,
        commentsUrl = null,
        isBookmarked = false,
    )

    private fun cacheInfo() = FeedSourceCacheInfo(SOURCE_ID, "etag", null, null, null, null)

    private companion object {
        const val SOURCE_ID = "source-1"
        const val SOURCE_URL = "https://fixture.test/rss"
        const val SYNC_TIMESTAMP = 1_000L
    }
}
