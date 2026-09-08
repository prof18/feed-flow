package com.prof18.feedflow.shared.domain.feedsync

import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.SyncedFeedItem
import com.prof18.feedflow.database.CloudArticleFlag
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloudPendingArticleFlagsDatabaseTest : KoinTestBase() {

    private val databaseHelper: DatabaseHelper by inject()
    private val sqlDriver: SqlDriver by inject()

    @Test
    fun `read and bookmark false values remain independent pending article flags`() = runTest(testDispatcher) {
        seedItems("item-1")
        databaseHelper.ensureCloudSyncState(SESSION_ID)

        databaseHelper.updateReadStatus(FeedItemId("item-1"), isRead = true, cloudSessionId = SESSION_ID)
        databaseHelper.updateBookmarkStatus(FeedItemId("item-1"), isBookmarked = true, cloudSessionId = SESSION_ID)
        databaseHelper.updateReadStatus(FeedItemId("item-1"), isRead = false, cloudSessionId = SESSION_ID)
        databaseHelper.updateBookmarkStatus(FeedItemId("item-1"), isBookmarked = false, cloudSessionId = SESSION_ID)

        val pendingByField = databaseHelper.getCloudPendingArticleFlags(SESSION_ID).associateBy { it.field }
        assertEquals(setOf(CloudArticleFlag.READ, CloudArticleFlag.BOOKMARK), pendingByField.keys)
        assertFalse(pendingByField.getValue(CloudArticleFlag.READ).value)
        assertFalse(pendingByField.getValue(CloudArticleFlag.BOOKMARK).value)
        assertTrue(
            pendingByField.getValue(CloudArticleFlag.READ).revision <
                pendingByField.getValue(CloudArticleFlag.BOOKMARK).revision,
        )
    }

    @Test
    fun `acknowledging a captured revision leaves a later edit pending`() = runTest(testDispatcher) {
        seedItems("item-1")
        databaseHelper.ensureCloudSyncState(SESSION_ID)
        databaseHelper.updateReadStatus(FeedItemId("item-1"), isRead = true, cloudSessionId = SESSION_ID)
        val captured = databaseHelper.getCloudPendingArticleFlags(SESSION_ID)

        databaseHelper.updateReadStatus(FeedItemId("item-1"), isRead = false, cloudSessionId = SESSION_ID)
        databaseHelper.acknowledgeCloudPendingArticleFlags(SESSION_ID, captured)

        val remaining = databaseHelper.getCloudPendingArticleFlags(SESSION_ID).single()
        assertFalse(remaining.value)
        assertTrue(remaining.revision > captured.single().revision)
    }

    @Test
    fun `pending article flags are isolated by session`() = runTest(testDispatcher) {
        seedItems("item-1")
        databaseHelper.ensureCloudSyncState(SESSION_ID)
        databaseHelper.ensureCloudSyncState(SECOND_SESSION_ID)

        databaseHelper.updateReadStatus(FeedItemId("item-1"), isRead = true, cloudSessionId = SESSION_ID)
        databaseHelper.updateBookmarkStatus(
            FeedItemId("item-1"),
            isBookmarked = true,
            cloudSessionId = SECOND_SESSION_ID,
        )

        assertEquals(
            listOf(CloudArticleFlag.READ),
            databaseHelper.getCloudPendingArticleFlags(SESSION_ID).map { it.field },
        )
        assertEquals(
            listOf(CloudArticleFlag.BOOKMARK),
            databaseHelper.getCloudPendingArticleFlags(SECOND_SESSION_ID).map { it.field },
        )
    }

    @Test
    fun `bulk mark read captures only unread items in the selected feed`() = runTest(testDispatcher) {
        val selectedSource = FeedSourceGenerator.feedSource(id = "selected-source")
        val otherSource = FeedSourceGenerator.feedSource(id = "other-source")
        databaseHelper.insertFeedSourceWithCategory(selectedSource)
        databaseHelper.insertFeedSourceWithCategory(otherSource)
        databaseHelper.insertFeedItems(
            listOf(
                buildFeedItem("selected-unread", "Selected unread", 3L, selectedSource),
                buildFeedItem("selected-read", "Selected read", 2L, selectedSource),
                buildFeedItem("other-unread", "Other unread", 1L, otherSource),
            ),
            lastSyncTimestamp = 0,
        )
        databaseHelper.updateReadStatus(FeedItemId("selected-read"), isRead = true)
        databaseHelper.ensureCloudSyncState(SESSION_ID)

        databaseHelper.markAllFeedAsRead(FeedFilter.Source(selectedSource), cloudSessionId = SESSION_ID)

        assertEquals(
            listOf("selected-unread"),
            databaseHelper.getCloudPendingArticleFlags(SESSION_ID).map { it.itemId },
        )
    }

    @Test
    fun `cloud state initialization is idempotent and preserves revision sequence`() = runTest(testDispatcher) {
        seedItems("item-1")
        databaseHelper.ensureCloudSyncState(SESSION_ID)
        databaseHelper.updateReadStatus(FeedItemId("item-1"), isRead = true, cloudSessionId = SESSION_ID)
        val firstRevision = databaseHelper.getCloudPendingArticleFlags(SESSION_ID).single().revision

        databaseHelper.ensureCloudSyncState(SESSION_ID)
        databaseHelper.updateBookmarkStatus(FeedItemId("item-1"), isBookmarked = true, cloudSessionId = SESSION_ID)

        val revisions = databaseHelper.getCloudPendingArticleFlags(SESSION_ID).map { it.revision }
        assertEquals(listOf(firstRevision, firstRevision + 1), revisions)
    }

    @Test
    fun `remote update reapplies and retains current pending article flags`() = runTest(testDispatcher) {
        seedItems("item-1", "absent-item")
        databaseHelper.ensureCloudSyncState(SESSION_ID)
        databaseHelper.updateReadStatus(FeedItemId("item-1"), isRead = true, cloudSessionId = SESSION_ID)
        databaseHelper.updateBookmarkStatus(FeedItemId("item-1"), isBookmarked = false, cloudSessionId = SESSION_ID)
        databaseHelper.updateReadStatus(FeedItemId("absent-item"), isRead = true)
        databaseHelper.updateBookmarkStatus(FeedItemId("absent-item"), isBookmarked = true)
        val captured = databaseHelper.getCloudPendingArticleFlags(SESSION_ID)

        databaseHelper.updateFeedItemReadAndBookmarked(
            syncedFeedItems = listOf(SyncedFeedItem("item-1", isRead = false, isBookmarked = true)),
            cloudSessionId = SESSION_ID,
            replaceAll = true,
        )

        val item = feedItem("item-1")
        assertTrue(item.is_read)
        assertFalse(item.is_bookmarked)
        val absentItem = feedItem("absent-item")
        assertFalse(absentItem.is_read)
        assertFalse(absentItem.is_bookmarked)
        assertEquals(captured, databaseHelper.getCloudPendingArticleFlags(SESSION_ID))
    }

    @Test
    fun `cloud article flag export includes false false items`() = runTest(testDispatcher) {
        seedItems("false-item")

        assertEquals(
            listOf(SyncedFeedItem("false-item", isRead = false, isBookmarked = false)),
            databaseHelper.getAllFeedItemFlagsForCloud(),
        )
    }

    @Test
    fun `raw cloud article flag values remain compatible with enum reads and writes`() = runTest(testDispatcher) {
        seedItems("item-1")
        databaseHelper.ensureCloudSyncState(SESSION_ID)
        sqlDriver.execute(
            identifier = null,
            sql = """
                INSERT INTO cloud_pending_article_flag(session_id, item_id, field, value, revision)
                VALUES ('$SESSION_ID', 'item-1', 'READ', 1, 1),
                       ('$SESSION_ID', 'item-1', 'BOOKMARK', 0, 2)
            """.trimIndent(),
            parameters = 0,
            binders = null,
        ).value

        assertEquals(
            setOf(CloudArticleFlag.READ, CloudArticleFlag.BOOKMARK),
            databaseHelper.getCloudPendingArticleFlags(SESSION_ID).map { it.field }.toSet(),
        )

        databaseHelper.updateReadStatus(FeedItemId("item-1"), isRead = false, cloudSessionId = SESSION_ID)

        databaseHelper.updateBookmarkStatus(FeedItemId("item-1"), isBookmarked = true, cloudSessionId = SESSION_ID)

        val storedFields = sqlDriver.executeQuery(
            identifier = null,
            sql = """
                SELECT field FROM cloud_pending_article_flag
                WHERE session_id = '$SESSION_ID' AND item_id = 'item-1'
                ORDER BY field
            """.trimIndent(),
            mapper = { cursor ->
                val fields = buildList {
                    while (cursor.next().value) add(requireNotNull(cursor.getString(0)))
                }
                app.cash.sqldelight.db.QueryResult.Value(fields)
            },
            parameters = 0,
            binders = null,
        ).value
        assertEquals(listOf("BOOKMARK", "READ"), storedFields)
    }

    @Test
    fun `pending article flag survives article cache deletion until acknowledgement`() = runTest(testDispatcher) {
        seedItems("item-1")
        databaseHelper.ensureCloudSyncState(SESSION_ID)
        databaseHelper.updateReadStatus(FeedItemId("item-1"), isRead = true, cloudSessionId = SESSION_ID)
        val captured = databaseHelper.getCloudPendingArticleFlags(SESSION_ID)

        databaseHelper.deleteOldFeedItems(timeThreshold = 2, feedFilter = FeedFilter.Timeline)

        assertEquals(captured, databaseHelper.getCloudPendingArticleFlags(SESSION_ID))
        databaseHelper.acknowledgeCloudPendingArticleFlags(SESSION_ID, captured)
        assertEquals(emptyList(), databaseHelper.getCloudPendingArticleFlags(SESSION_ID))
    }

    @Test
    fun `bulk capture and remote overlay preserve one thousand pending edits`() = runTest(testDispatcher) {
        val source = FeedSourceGenerator.feedSource(id = "bulk-source")
        databaseHelper.insertFeedSourceWithCategory(source)
        val items = List(BULK_ITEM_COUNT) { index ->
            buildFeedItem(
                id = "bulk-${index.toString().padStart(4, '0')}",
                title = "Bulk $index",
                pubDateMillis = index.toLong(),
                source = source,
            )
        }
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)
        databaseHelper.ensureCloudSyncState(SESSION_ID)

        databaseHelper.markAllFeedAsRead(FeedFilter.Timeline, cloudSessionId = SESSION_ID)
        val pending = databaseHelper.getCloudPendingArticleFlags(SESSION_ID)
        assertEquals(BULK_ITEM_COUNT, pending.size)

        databaseHelper.updateFeedItemReadAndBookmarked(
            syncedFeedItems = items.map { item ->
                SyncedFeedItem(item.id, isRead = false, isBookmarked = false)
            },
            cloudSessionId = SESSION_ID,
        )

        assertTrue(databaseHelper.getAllFeedItemFlagsForCloud().all { it.isRead })
        assertEquals(pending, databaseHelper.getCloudPendingArticleFlags(SESSION_ID))
    }

    @Test
    fun `pending insert failure rolls back the visible edit`() = runTest(testDispatcher) {
        seedItems("item-1")
        databaseHelper.ensureCloudSyncState(SESSION_ID)
        sqlDriver.execute(
            identifier = null,
            sql = """
                CREATE TRIGGER abort_cloud_pending_insert
                BEFORE INSERT ON cloud_pending_article_flag
                BEGIN
                    SELECT RAISE(ABORT, 'pending insert failed');
                END
            """.trimIndent(),
            parameters = 0,
            binders = null,
        ).value

        assertFailsWith<Exception> {
            databaseHelper.updateReadStatus(FeedItemId("item-1"), isRead = true, cloudSessionId = SESSION_ID)
        }

        assertFalse(feedItem("item-1").is_read)
        assertEquals(emptyList(), databaseHelper.getCloudPendingArticleFlags(SESSION_ID))
    }

    private suspend fun seedItems(vararg itemIds: String) {
        val source = FeedSourceGenerator.feedSource(id = "source")
        databaseHelper.insertFeedSourceWithCategory(source)
        databaseHelper.insertFeedItems(
            itemIds.mapIndexed { index, itemId ->
                buildFeedItem(itemId, itemId, index.toLong(), source)
            },
            lastSyncTimestamp = 0,
        )
    }

    private suspend fun feedItem(itemId: String) = databaseHelper.getFeedItems(
        feedFilter = FeedFilter.Timeline,
        pageSize = 100,
        showReadItems = true,
        sortOrder = FeedOrder.NEWEST_FIRST,
    ).single { it.url_hash == itemId }

    private companion object {
        const val SESSION_ID = "session-1"
        const val SECOND_SESSION_ID = "session-2"
        const val BULK_ITEM_COUNT = 1_000
    }
}
