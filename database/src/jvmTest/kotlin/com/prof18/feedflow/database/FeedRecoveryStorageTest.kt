package com.prof18.feedflow.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedRecoveryProfile
import com.prof18.feedflow.core.model.FeedRecoveryRecord
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCacheInfo
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.RecoveredFeedOperation
import com.prof18.feedflow.core.model.RecoveryMode
import com.prof18.feedflow.db.FeedFlowDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedRecoveryStorageTest {

    @Test
    fun `migration to the recovery schema keeps feeds and creates empty tables`() {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            FeedFlowDB.Schema.create(driver)
            driver.execute(null, "DROP TABLE feed_recovery_record;", 0)
            driver.execute(null, "DROP TABLE feed_recovery_profile;", 0)
            driver.execute(
                null,
                "INSERT INTO feed_source(url_hash, url, title) VALUES ('kept', 'https://fixture.test/rss', 'Kept');",
                0,
            )

            FeedFlowDB.Schema.migrate(
                driver,
                oldVersion = PRE_RECOVERY_VERSION,
                newVersion = FeedFlowDB.Schema.version,
            )

            assertEquals(1L, count(driver, "feed_source"))
            assertEquals(0L, count(driver, "feed_recovery_record"))
            assertEquals(0L, count(driver, "feed_recovery_profile"))
        }
    }

    @Test
    fun `upserted record is returned for its epoch`() = withHelper { helper ->
        helper.insertFeedSource(listOf(parsedSource()))
        helper.upsertRecoveryRecord(record(), commit = { it() })

        val records = helper.recoveryRecords(EPOCH)

        assertEquals(1, records.size)
        assertEquals(RecoveryMode.SessionApproved, records.single().mode)
        assertEquals(record(), helper.recoveryRecord(EPOCH, SOURCE_ID))
        assertTrue(helper.recoveryRecords("other").isEmpty())
    }

    @Test
    fun `changing the source url removes its record`() = withHelper { helper ->
        helper.insertFeedSource(listOf(parsedSource()))
        helper.upsertRecoveryRecord(record(), commit = { it() })

        helper.updateFeedSource(
            FeedSource(
                id = SOURCE_ID,
                url = "https://fixture.test/other-rss",
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
            ),
        )

        assertNull(helper.recoveryRecord(EPOCH, SOURCE_ID))
    }

    @Test
    fun `deleting the source removes its record`() = withHelper { helper ->
        helper.insertFeedSource(listOf(parsedSource()))
        helper.upsertRecoveryRecord(record(), commit = { it() })

        helper.deleteFeedSource(SOURCE_ID)

        assertNull(helper.recoveryRecord(EPOCH, SOURCE_ID))
    }

    @Test
    fun `retireOrphanRecoveryProfile retires only profiles without records or reader retention`() =
        withHelper { helper ->
            helper.insertFeedSource(listOf(parsedSource()))
            helper.insertRecoveryProfile(profile(id = "orphan"), commit = { it() })
            helper.insertRecoveryProfile(profile(id = PROFILE_ID), commit = { it() })
            helper.upsertRecoveryRecord(record(), commit = { it() })

            helper.retireOrphanRecoveryProfile("orphan")
            helper.retireOrphanRecoveryProfile(PROFILE_ID)

            val profiles = helper.recoveryProfiles().associateBy { it.id }
            assertTrue(profiles.getValue("orphan").retired)
            assertFalse(profiles.getValue(PROFILE_ID).retired)
            assertTrue(helper.hasRecoveryProfiles())
        }

    @Test
    fun `recovered add writes source preferences items record and cloud change`() = withHelper { helper ->
        helper.insertRecoveryProfile(profile(id = PROFILE_ID), commit = { it() })
        helper.ensureCloudSyncState(CLOUD_SESSION)
        val source = source(isPinned = true)

        helper.commitRecoveredFeed(
            source = source,
            items = listOf(item(source)),
            operation = RecoveredFeedOperation.Add,
            expectedUrl = null,
            record = record(),
            cloudSessionId = CLOUD_SESSION,
            commit = { it() },
        )

        val stored = assertNotNull(helper.getFeedSource(SOURCE_ID))
        assertTrue(stored.isPinned)
        assertTrue(helper.hasFeedItems())
        assertEquals(RecoveryMode.SessionApproved, helper.recoveryRecord(EPOCH, SOURCE_ID)?.mode)
        assertTrue(helper.getCloudPendingFeedAndCategoryChanges(CLOUD_SESSION).isNotEmpty())
    }

    @Test
    fun `recovered add of an existing url writes nothing`() = withHelper { helper ->
        helper.insertRecoveryProfile(profile(id = PROFILE_ID), commit = { it() })
        helper.insertFeedSource(listOf(parsedSource().copy(id = "other-id")))

        assertFailsWith<IllegalStateException> {
            helper.commitRecoveredFeed(
                source = source(),
                items = listOf(item(source())),
                operation = RecoveredFeedOperation.Add,
                expectedUrl = null,
                record = record(),
                cloudSessionId = null,
                commit = { it() },
            )
        }

        assertNull(helper.getFeedSource(SOURCE_ID))
        assertFalse(helper.hasFeedItems())
        assertNull(helper.recoveryRecord(EPOCH, SOURCE_ID))
    }

    @Test
    fun `recovered refresh with a retired profile writes nothing`() = withHelper { helper ->
        helper.insertFeedSource(listOf(parsedSource()))
        helper.insertRecoveryProfile(profile(id = PROFILE_ID), commit = { it() })
        helper.retireRecoveryProfile(PROFILE_ID)

        assertFailsWith<IllegalStateException> {
            helper.commitRecoveredFeed(
                source = source(),
                items = listOf(item(source())),
                operation = RecoveredFeedOperation.Refresh,
                expectedUrl = SOURCE_URL,
                record = record(),
                cloudSessionId = null,
                commit = { it() },
            )
        }

        assertFalse(helper.hasFeedItems())
        assertNull(helper.recoveryRecord(EPOCH, SOURCE_ID))
    }

    @Test
    fun `a failing commit guard rolls back the recovered feed`() = withHelper { helper ->
        helper.insertRecoveryProfile(profile(id = PROFILE_ID), commit = { it() })

        assertFailsWith<IllegalStateException> {
            helper.commitRecoveredFeed(
                source = source(),
                items = listOf(item(source())),
                operation = RecoveredFeedOperation.Add,
                expectedUrl = null,
                record = record(),
                cloudSessionId = null,
                commit = { error("Recovery owner changed") },
            )
        }

        assertNull(helper.getFeedSource(SOURCE_ID))
        assertNull(helper.recoveryRecord(EPOCH, SOURCE_ID))
    }

    @Test
    fun `a recovered refresh approves the other waiting feeds of the same site`() = withHelper { helper ->
        helper.insertFeedSource(
            listOf(
                parsedSource(),
                parsedSource().copy(id = "same-a", url = "https://fixture.test/a"),
                parsedSource().copy(id = "same-b", url = "https://fixture.test/b"),
                parsedSource().copy(id = "other", url = "https://other.test/rss"),
            ),
        )
        helper.insertRecoveryProfile(profile(id = PROFILE_ID), commit = { it() })
        val waiting = record().copy(mode = RecoveryMode.VerificationRequired, profileId = null)
        helper.upsertRecoveryRecord(
            waiting.copy(
                sourceId = "same-a",
                subscriptionUrl = "https://fixture.test/a",
                effectiveUrl = "https://fixture.test/a?x",
            ),
        ) { it() }
        helper.upsertRecoveryRecord(
            waiting.copy(
                sourceId = "same-b",
                subscriptionUrl = "https://fixture.test/b",
                effectiveUrl = "https://fixture.test/b",
            ),
        ) { it() }
        helper.upsertRecoveryRecord(
            waiting.copy(
                sourceId = "other",
                subscriptionUrl = "https://other.test/rss",
                effectiveUrl = "https://other.test/rss",
                origin = "https://other.test:443",
            ),
        ) { it() }

        helper.commitRecoveredFeed(
            source = source(),
            items = listOf(item(source())),
            operation = RecoveredFeedOperation.Refresh,
            expectedUrl = SOURCE_URL,
            record = record(),
            cloudSessionId = null,
            commit = { it() },
        )

        val sameA = assertNotNull(helper.recoveryRecord(EPOCH, "same-a"))
        assertEquals(RecoveryMode.SessionApproved, sameA.mode)
        assertEquals(PROFILE_ID, sameA.profileId)
        assertEquals("https://fixture.test/a?x", sameA.effectiveUrl)
        assertEquals(RecoveryMode.SessionApproved, helper.recoveryRecord(EPOCH, "same-b")?.mode)
        val other = assertNotNull(helper.recoveryRecord(EPOCH, "other"))
        assertEquals(RecoveryMode.VerificationRequired, other.mode)
        assertNull(other.profileId)
    }

    @Test
    fun `a recovered edit to a new url drops the old cache info`() = withHelper { helper ->
        helper.insertFeedSource(listOf(parsedSource()))
        helper.updateFeedSourcesCacheInfo(
            listOf(FeedSourceCacheInfo(SOURCE_ID, "etag", null, null, null, null)),
        )
        helper.insertRecoveryProfile(profile(id = PROFILE_ID), commit = { it() })
        val moved = source().copy(url = "https://fixture.test/moved")

        helper.commitRecoveredFeed(
            source = moved,
            items = emptyList(),
            operation = RecoveredFeedOperation.Edit,
            expectedUrl = SOURCE_URL,
            record = record().copy(subscriptionUrl = moved.url),
            cloudSessionId = null,
            commit = { it() },
        )

        assertEquals("https://fixture.test/moved", helper.getFeedSource(SOURCE_ID)?.url)
        assertTrue(helper.getFeedSourcesCacheInfo().isEmpty())
    }

    private fun source(isPinned: Boolean = false) = FeedSource(
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
        isPinned = isPinned,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
    )

    private fun item(source: FeedSource) = FeedItem(
        id = "item-1",
        url = "/post/1",
        title = "Item",
        subtitle = null,
        content = null,
        imageUrl = null,
        feedSource = source,
        pubDateMillis = null,
        isRead = false,
        dateString = null,
        commentsUrl = null,
        isBookmarked = false,
    )

    private fun withHelper(block: suspend (DatabaseHelper) -> Unit) {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            FeedFlowDB.Schema.create(driver)
            val helper = DatabaseHelper(driver, Dispatchers.Unconfined, Logger)
            runBlocking { block(helper) }
        }
    }

    private fun count(driver: JdbcSqliteDriver, table: String): Long? = driver.executeQuery(
        null,
        "SELECT count(*) FROM $table;",
        { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0))
        },
        0,
    ).value

    private fun parsedSource() = ParsedFeedSource(
        id = SOURCE_ID,
        url = SOURCE_URL,
        title = "Title",
        category = null,
        logoUrl = null,
        websiteUrl = null,
    )

    private fun record() = FeedRecoveryRecord(
        accountEpoch = EPOCH,
        sourceId = SOURCE_ID,
        subscriptionUrl = SOURCE_URL,
        effectiveUrl = SOURCE_URL,
        origin = "https://fixture.test:443",
        mode = RecoveryMode.SessionApproved,
        profileId = PROFILE_ID,
        statusCode = 403,
        retryAfter = null,
    )

    private fun profile(id: String) = FeedRecoveryProfile(
        id = id,
        accountEpoch = EPOCH,
        origin = "https://fixture.test:443",
        userAgent = "ua",
        engineVersion = "engine",
        readerRetained = false,
        retired = false,
    )

    private companion object {
        const val EPOCH = "epoch"
        const val SOURCE_ID = "source"
        const val SOURCE_URL = "https://fixture.test/rss"
        const val PROFILE_ID = "feedflow-recovery-1"
        const val CLOUD_SESSION = "cloud-session"
        val PRE_RECOVERY_VERSION = FeedFlowDB.Schema.version - 1
    }
}
