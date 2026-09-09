package com.prof18.feedflow.shared.domain.feedsync

import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.model.CloudFeedOrCategoryEntity
import com.prof18.feedflow.core.model.CloudFeedOrCategoryField
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CloudPendingFeedAndCategoryChangesDatabaseTest : KoinTestBase() {

    private val database: DatabaseHelper by inject()
    private val sqlDriver: SqlDriver by inject()

    @Test
    fun `stored feed and category strings decode as enums and can be acknowledged`() = runTest(testDispatcher) {
        database.ensureCloudSyncState(SESSION_ID)
        sqlDriver.execute(
            identifier = null,
            sql = """
                INSERT INTO cloud_pending_feed_or_category_change(session_id, entity, id, field, value, revision)
                VALUES ('$SESSION_ID', 'SOURCE', 'source', 'EXISTS', '1', 1),
                       ('$SESSION_ID', 'SOURCE', 'source', 'URL', 'https://example.com/source', 1),
                       ('$SESSION_ID', 'SOURCE', 'source', 'TITLE', 'Source', 1),
                       ('$SESSION_ID', 'SOURCE', 'source', 'CATEGORY', NULL, 1),
                       ('$SESSION_ID', 'SOURCE', 'source', 'LOGO', NULL, 1),
                       ('$SESSION_ID', 'CATEGORY', 'category', 'EXISTS', '1', 1),
                       ('$SESSION_ID', 'CATEGORY', 'category', 'TITLE', 'Tech', 1)
            """.trimIndent(),
            parameters = 0,
            binders = null,
        ).value

        val pending = database.getCloudPendingFeedAndCategoryChanges(SESSION_ID)
        assertEquals(CloudFeedOrCategoryEntity.entries.toSet(), pending.map { it.entity }.toSet())
        assertEquals(
            CloudFeedOrCategoryField.entries.toSet(),
            pending.filter { it.entity == CloudFeedOrCategoryEntity.SOURCE }.map { it.field }.toSet(),
        )
        assertEquals(null, pending.single { it.field == CloudFeedOrCategoryField.CATEGORY }.value)
        database.acknowledgeCloudPendingFeedAndCategoryChanges(SESSION_ID, pending)
        assertTrue(database.getCloudPendingFeedAndCategoryChanges(SESSION_ID).isEmpty())
    }

    @Test
    fun `enum writes preserve the existing feed and category storage values`() = runTest(testDispatcher) {
        database.ensureCloudSyncState(SESSION_ID)
        val category = category("category", "Tech")
        database.insertCategories(listOf(category), SESSION_ID)
        database.insertFeedSource(listOf(source(category = category, logoUrl = "logo")), SESSION_ID)

        val storedFields = sqlDriver.executeQuery(
            identifier = null,
            sql = """
                SELECT entity, field FROM cloud_pending_feed_or_category_change
                WHERE session_id = '$SESSION_ID'
            """.trimIndent(),
            mapper = { cursor ->
                val fields = buildSet {
                    while (cursor.next().value) {
                        add(requireNotNull(cursor.getString(0)) to requireNotNull(cursor.getString(1)))
                    }
                }
                app.cash.sqldelight.db.QueryResult.Value(fields)
            },
            parameters = 0,
            binders = null,
        ).value
        assertEquals(
            setOf(
                "SOURCE" to "EXISTS",
                "SOURCE" to "URL",
                "SOURCE" to "TITLE",
                "SOURCE" to "CATEGORY",
                "SOURCE" to "LOGO",
                "CATEGORY" to "EXISTS",
                "CATEGORY" to "TITLE",
            ),
            storedFields,
        )
    }

    @Test
    fun `pending feed and category fields are readable and coalesce independently`() = runTest(testDispatcher) {
        val category = category("category", "Tech")
        database.insertCategories(listOf(category))
        database.insertFeedSource(listOf(source(category = category)))
        database.ensureCloudSyncState(SESSION_ID)

        database.updateFeedSourceName("source", "Renamed", SESSION_ID)
        database.updateFeedSource(
            feedSource = requireNotNull(database.getFeedSource("source")).copy(category = null),
            cloudSessionId = SESSION_ID,
        )

        val pending = database.getCloudPendingFeedAndCategoryChanges(SESSION_ID)
        assertEquals(
            setOf(CloudFeedOrCategoryField.TITLE, CloudFeedOrCategoryField.CATEGORY),
            pending.filter { it.entity == CloudFeedOrCategoryEntity.SOURCE && it.id == "source" }
                .map { it.field }
                .toSet(),
        )
        assertEquals("Renamed", pending.single { it.field == CloudFeedOrCategoryField.TITLE }.value)
        assertEquals(null, pending.single { it.field == CloudFeedOrCategoryField.CATEGORY }.value)
    }

    @Test
    fun `acknowledging old feed and category changes retains newer field edits`() = runTest(testDispatcher) {
        database.insertFeedSource(listOf(source()))
        database.ensureCloudSyncState(SESSION_ID)

        database.updateFeedSourceName("source", "First", SESSION_ID)
        val firstCapture = database.getCloudPendingFeedAndCategoryChanges(SESSION_ID)
        database.updateFeedSourceName("source", "Second", SESSION_ID)

        database.acknowledgeCloudPendingFeedAndCategoryChanges(SESSION_ID, firstCapture)
        val newerCapture = database.getCloudPendingFeedAndCategoryChanges(SESSION_ID)
        assertEquals("Second", newerCapture.single().value)
        assertTrue(newerCapture.single().revision > firstCapture.single().revision)

        database.acknowledgeCloudPendingFeedAndCategoryChanges(SESSION_ID, newerCapture)
        assertEquals(emptyList(), database.getCloudPendingFeedAndCategoryChanges(SESSION_ID))
    }

    @Test
    fun `stale embedded category metadata does not rename an existing category`() = runTest(testDispatcher) {
        val canonicalCategory = category("category", "Canonical")
        database.insertCategories(listOf(canonicalCategory))
        database.ensureCloudSyncState(SESSION_ID)

        database.insertFeedSource(
            listOf(source(category = category("category", "Stale embedded title"))),
            cloudSessionId = SESSION_ID,
        )

        assertEquals(canonicalCategory, database.getFeedSourceCategory(canonicalCategory.id))
        assertEquals(
            emptyList(),
            database.getCloudPendingFeedAndCategoryChanges(SESSION_ID).filter {
                it.entity == CloudFeedOrCategoryEntity.CATEGORY && it.id == canonicalCategory.id
            },
        )
    }

    @Test
    fun `feed and category change journal failure rolls back the visible mutation`() = runTest(testDispatcher) {
        database.ensureCloudSyncState(SESSION_ID)
        createAbortTrigger()

        assertFailsWith<Exception> {
            database.insertCategories(listOf(category("category", "Tech")), SESSION_ID)
        }

        assertEquals(emptyList(), database.getFeedSourceCategories())
        assertEquals(emptyList(), database.getCloudPendingFeedAndCategoryChanges(SESSION_ID))
    }

    @Test
    fun `duplicate source insert records metadata updates instead of creation`() = runTest(testDispatcher) {
        database.insertFeedSource(listOf(source(title = "Old")))
        database.ensureCloudSyncState(SESSION_ID)

        database.insertFeedSource(listOf(source(title = "New", logoUrl = "logo")), SESSION_ID)

        val pending = database.getCloudPendingFeedAndCategoryChanges(SESSION_ID)
        assertEquals(emptyList(), pending.filter { it.field == CloudFeedOrCategoryField.EXISTS })
        assertEquals(
            setOf(CloudFeedOrCategoryField.TITLE, CloudFeedOrCategoryField.LOGO),
            pending.map { it.field }.toSet(),
        )
    }

    @Test
    fun `explicit delete and readd coalesce to the recreated source`() = runTest(testDispatcher) {
        database.insertFeedSource(listOf(source(title = "Old")))
        database.ensureCloudSyncState(SESSION_ID)

        database.deleteFeedSource("source", SESSION_ID)
        database.insertFeedSource(listOf(source(title = "Recreated")), SESSION_ID)

        val pending = database.getCloudPendingFeedAndCategoryChanges(SESSION_ID)
        assertEquals("1", pending.single { it.field == CloudFeedOrCategoryField.EXISTS }.value)
        assertEquals("Recreated", pending.single { it.field == CloudFeedOrCategoryField.TITLE }.value)
    }

    @Test
    fun `delete all keeps session and journals every feed and category deletion`() = runTest(testDispatcher) {
        val category = category("category", "Tech")
        database.insertCategories(listOf(category))
        database.insertFeedSource(listOf(source(category = category)))
        database.ensureCloudSyncState(SESSION_ID)

        database.deleteAllCloudSubscriptions(SESSION_ID)

        val pending = database.getCloudPendingFeedAndCategoryChanges(SESSION_ID)
        assertEquals(
            setOf(CloudFeedOrCategoryEntity.SOURCE, CloudFeedOrCategoryEntity.CATEGORY),
            pending.map { it.entity }.toSet(),
        )
        assertEquals(
            "0",
            pending.single {
                it.entity == CloudFeedOrCategoryEntity.SOURCE && it.field == CloudFeedOrCategoryField.EXISTS
            }.value,
        )
        database.ensureCloudSyncState(SESSION_ID)
        assertEquals(pending, database.getCloudPendingFeedAndCategoryChanges(SESSION_ID))
    }

    @Test
    fun `session guard failure rolls back both visible edit and journal`() = runTest(testDispatcher) {
        database.ensureCloudSyncState(SESSION_ID)
        assertFailsWith<IllegalStateException> {
            database.insertCategories(
                listOf(category("category", "Tech")),
                cloudSessionId = SESSION_ID,
                withCurrentSession = { edit ->
                    edit()
                    error("Account changed")
                },
            )
        }
        assertTrue(database.getFeedSourceCategories().isEmpty())
        assertTrue(database.getCloudPendingFeedAndCategoryChanges(SESSION_ID).isEmpty())
    }

    private fun createAbortTrigger() {
        sqlDriver.execute(
            identifier = null,
            sql = """
                CREATE TRIGGER abort_cloud_pending_feed_or_category_change_insert
                BEFORE INSERT ON cloud_pending_feed_or_category_change
                BEGIN
                    SELECT RAISE(ABORT, 'pending feed or category change insert failed');
                END
            """.trimIndent(),
            parameters = 0,
            binders = null,
        ).value
    }

    private fun source(
        title: String = "Source",
        category: FeedSourceCategory? = null,
        logoUrl: String? = null,
    ) = ParsedFeedSource(
        id = "source",
        url = "https://example.com/source",
        title = title,
        category = category,
        logoUrl = logoUrl,
        websiteUrl = null,
    )

    private fun category(id: String, title: String) = FeedSourceCategory(id = id, title = title)

    private companion object {
        const val SESSION_ID = "session-1"
    }
}
