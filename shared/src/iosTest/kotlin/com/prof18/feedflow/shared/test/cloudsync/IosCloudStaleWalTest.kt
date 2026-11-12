package com.prof18.feedflow.shared.test.cloudsync

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.JournalMode
import co.touchlab.sqliter.NO_VERSION_CHECK
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.dataWithContentsOfFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IosCloudStaleWalTest : KoinTestBase() {

    @Test
    fun `installing a downloaded sync database discards the WAL a killed process left behind`() =
        runTest(testDispatcher) {
            val store = CloudStore()
            val device1 = createCloudDevice(CloudProvider.GOOGLE_DRIVE, store, "stale-wal-device1")
            val device2 = createCloudDevice(CloudProvider.GOOGLE_DRIVE, store, "stale-wal-device2")
            var relaunched: CloudDevice? = null
            try {
                device1.seed()
                device1.read("article-one", true)
                device1.backup()
                store.propagate("stale-wal-device1")
                device2.refresh()
                device2.close()
                val walPath = "${device2.root}/${SyncedDatabaseHelper.SYNC_DATABASE_NAME_DEBUG}-wal"
                val staleWal = assertNotNull(leaveStaleSyncWal(device2.root, walPath))

                device1.read("article-two", true)
                device1.backup()
                store.propagate("stale-wal-device1")

                // A new process, as on the next background launch: the backup is installed before anything
                // opens the old sync database. SQLite would replay the old WAL onto the new file.
                val device = createCloudDevice(CloudProvider.GOOGLE_DRIVE, store, "stale-wal-device2", device2.root)
                relaunched = device
                val outcomes = mutableListOf<SyncResult>()
                val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                    device.application.koin.get<FeedSyncMessageQueue>().messageQueue.collect { outcomes.add(it) }
                }
                try {
                    device.refresh()
                } finally {
                    collector.cancel()
                }

                assertFalse(
                    NSData.dataWithContentsOfFile(walPath)?.isEqual(staleWal) == true,
                    "The old database's WAL was kept next to the downloaded one",
                )
                assertTrue(outcomes.none { it.isError() }, "Sync failed after installing the backup: $outcomes")
                assertEquals(device1.flags(), device.flags())
            } finally {
                try {
                    device1.close()
                } finally {
                    try {
                        device2.close()
                    } finally {
                        relaunched?.close()
                    }
                }
            }
        }

    // Writes to the sync database without a checkpoint and keeps the -wal and -shm files, as when iOS
    // kills the app before the connection is closed.
    private fun leaveStaleSyncWal(root: String, walPath: String): NSData? {
        val name = SyncedDatabaseHelper.SYNC_DATABASE_NAME_DEBUG
        val databasePath = "$root/$name"
        val driver = NativeSqliteDriver(
            DatabaseConfiguration(
                name = name,
                version = NO_VERSION_CHECK,
                create = {},
                journalMode = JournalMode.WAL,
                extendedConfig = DatabaseConfiguration.Extended(basePath = root),
            ),
        )
        val fileManager = NSFileManager.defaultManager
        try {
            driver.executeQuery(null, "PRAGMA wal_autocheckpoint = 0", { cursor ->
                cursor.next()
                QueryResult.Unit
            }, 0)
            driver.execute(null, "CREATE TABLE stale_pages(value TEXT)", 0)
            repeat(STALE_ROWS) {
                driver.execute(null, "INSERT INTO stale_pages VALUES ('${"x".repeat(STALE_ROW_LENGTH)}')", 0)
            }
            listOf("-wal", "-shm").forEach { suffix ->
                assertTrue(fileManager.copyItemAtPath("$databasePath$suffix", "$databasePath$suffix.kept", null))
            }
        } finally {
            driver.close()
        }
        listOf("-wal", "-shm").forEach { suffix ->
            fileManager.removeItemAtPath("$databasePath$suffix", null)
            assertTrue(fileManager.moveItemAtPath("$databasePath$suffix.kept", "$databasePath$suffix", null))
        }
        return NSData.dataWithContentsOfFile(walPath)
    }

    private companion object {
        const val STALE_ROWS = 40
        const val STALE_ROW_LENGTH = 500
    }
}
