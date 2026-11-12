package com.prof18.feedflow.shared.test.cloudsync

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.JournalMode
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosCloudStaleJournalTest : KoinTestBase() {

    @Test
    fun `a WAL left by a killed session does not corrupt the downloaded sync database`() = runTest(testDispatcher) {
        val store = CloudStore()
        val device1 = createCloudDevice(CloudProvider.ICLOUD, store, "journal-device1")
        val device2 = createCloudDevice(CloudProvider.ICLOUD, store, "journal-device2")
        val outcomes = mutableListOf<SyncResult>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            device2.application.koin.get<FeedSyncMessageQueue>().messageQueue.collect { outcomes.add(it) }
        }
        try {
            device1.seed()
            device1.backup()
            store.propagate("journal-device1")
            device2.refresh()

            device2.syncDatabase.closeScope()
            leaveStaleWal(device2.root)

            device1.read("article-one", true)
            device1.bookmark("article-two", true)
            device1.backup()
            store.propagate("journal-device1")
            outcomes.clear()
            device2.refresh()

            assertTrue(outcomes.isNotEmpty())
            assertTrue(outcomes.none { it.isError() }, "Sync failed after the database was replaced: $outcomes")
            assertEquals(device1.syncDatabase.getAllFeedSources(), device2.syncDatabase.getAllFeedSources())
            assertEquals(device1.syncDatabase.getAllFeedItems(), device2.syncDatabase.getAllFeedItems())
            assertEquals(device1.flags(), device2.flags())
        } finally {
            collector.cancel()
            try {
                device1.close()
            } finally {
                device2.close()
            }
        }
    }

    // Simulates iOS killing the app while the sync database connection still had uncheckpointed WAL frames.
    private fun leaveStaleWal(root: String) {
        val fileManager = NSFileManager.defaultManager
        val name = SyncedDatabaseHelper.SYNC_DATABASE_NAME_DEBUG
        check(fileManager.fileExistsAtPath("$root/$name")) { "Missing sync database in $root" }
        val sessionDir = "$root/killed-session"
        check(fileManager.createDirectoryAtPath(sessionDir, true, null, null))
        check(fileManager.copyItemAtPath("$root/$name", "$sessionDir/$name", null))

        val driver = NativeSqliteDriver(
            schema = FeedFlowFeedSyncDB.Schema,
            name = name,
            onConfiguration = { configuration ->
                configuration.copy(
                    extendedConfig = configuration.extendedConfig.copy(basePath = sessionDir),
                    journalMode = JournalMode.WAL,
                )
            },
        )
        repeat(STALE_SOURCES) { index ->
            driver.execute(
                null,
                "INSERT INTO synced_feed_source(url_hash, url, title, category_id) " +
                    "VALUES ('stale-$index', 'https://stale.example/$index', 'Stale $index', NULL)",
                0,
            )
        }
        listOf("-wal", "-shm").forEach { suffix ->
            fileManager.removeItemAtPath("$root/$name$suffix", null)
            check(fileManager.copyItemAtPath("$sessionDir/$name$suffix", "$root/$name$suffix", null))
        }
        driver.close()
        fileManager.removeItemAtPath(sessionDir, null)
    }

    private companion object {
        const val STALE_SOURCES = 200
    }
}
