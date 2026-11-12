package com.prof18.feedflow.feedsync.database.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.JournalMode
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class SyncDatabaseFilesTest {

    private val fileManager = NSFileManager.defaultManager
    private val root = "${NSTemporaryDirectory()}sync-db-files-${NSUUID().UUIDString}"

    @AfterTest
    fun cleanUp() {
        fileManager.removeItemAtPath(root, null)
    }

    @Test
    fun `replacing only the main file replays the stale WAL onto the downloaded database`() {
        val installDir = createKilledSessionDatabase()

        replaceMainFile(installDir, createDownloadedDatabase())

        val error = assertFails { readFeedSourceTitles(installDir) }
        assertContains(error.message.orEmpty(), "integrity check failed")
    }

    @Test
    fun `removing the journals before replacing installs the downloaded database`() {
        val installDir = createKilledSessionDatabase()

        removeSyncDatabaseJournals(NSURL.fileURLWithPath("$installDir/$DATABASE_NAME"))
        assertFalse(fileManager.fileExistsAtPath("$installDir/$DATABASE_NAME-wal"))
        assertFalse(fileManager.fileExistsAtPath("$installDir/$DATABASE_NAME-shm"))
        replaceMainFile(installDir, createDownloadedDatabase())

        assertEquals(listOf("Remote"), readFeedSourceTitles(installDir))
    }

    @Test
    fun `removing the journals of a cleanly closed database keeps its data`() {
        val directory = createDirectory("clean")
        val driver = walDriver(directory)
        driver.execute(
            null,
            "INSERT INTO synced_feed_source(url_hash, url, title, category_id) " +
                "VALUES ('local', 'https://local.example', 'Local', NULL)",
            0,
        )
        driver.close()

        removeSyncDatabaseJournals(NSURL.fileURLWithPath("$directory/$DATABASE_NAME"))

        assertTrue(fileManager.fileExistsAtPath("$directory/$DATABASE_NAME"))
        assertEquals(listOf("Local"), readFeedSourceTitles(directory))
    }

    private fun replaceMainFile(directory: String, staged: String) {
        val database = "$directory/$DATABASE_NAME"
        check(fileManager.removeItemAtPath(database, null))
        check(fileManager.moveItemAtPath(staged, database, null))
    }

    private fun createKilledSessionDatabase(): String {
        val liveDir = createDirectory("live")
        val killedDir = createDirectory("killed")
        val driver = walDriver(liveDir)
        repeat(LOCAL_SOURCES) { index ->
            driver.execute(
                null,
                "INSERT INTO synced_feed_source_category(id, title) VALUES ('category-$index', 'Category $index')",
                0,
            )
            driver.execute(
                null,
                "INSERT INTO synced_feed_source(url_hash, url, title, category_id) " +
                    "VALUES ('local-$index', 'https://local.example/$index', 'Local $index', 'category-$index')",
                0,
            )
        }
        // A process killed with this connection open leaves its uncheckpointed WAL behind.
        listOf("", "-wal", "-shm").forEach { suffix ->
            val fileName = "$DATABASE_NAME$suffix"
            check(fileManager.copyItemAtPath("$liveDir/$fileName", "$killedDir/$fileName", null))
        }
        driver.close()
        return killedDir
    }

    private fun createDownloadedDatabase(): String {
        val downloadDir = createDirectory("download")
        val driver = NativeSqliteDriver(
            schema = FeedFlowFeedSyncDB.Schema,
            name = DATABASE_NAME,
            onConfiguration = { configuration ->
                configuration.copy(
                    extendedConfig = configuration.extendedConfig.copy(basePath = downloadDir),
                    journalMode = JournalMode.DELETE,
                )
            },
        )
        driver.execute(
            null,
            "INSERT INTO synced_feed_source(url_hash, url, title, category_id) " +
                "VALUES ('remote', 'https://remote.example', 'Remote', NULL)",
            0,
        )
        driver.close()
        val staged = "$root/staged.db"
        check(fileManager.moveItemAtPath("$downloadDir/$DATABASE_NAME", staged, null))
        return staged
    }

    private fun readFeedSourceTitles(directory: String): List<String> {
        val driver = walDriver(directory)
        try {
            validateSyncDatabase(driver)
            return driver.executeQuery(null, "SELECT title FROM synced_feed_source ORDER BY title", { cursor ->
                val titles = mutableListOf<String>()
                while (cursor.next().value) titles.add(requireNotNull(cursor.getString(0)))
                QueryResult.Value(titles)
            }, 0).value
        } finally {
            driver.close()
        }
    }

    private fun walDriver(directory: String): SqlDriver = NativeSqliteDriver(
        schema = FeedFlowFeedSyncDB.Schema,
        name = DATABASE_NAME,
        onConfiguration = { configuration ->
            configuration.copy(
                extendedConfig = configuration.extendedConfig.copy(basePath = directory),
                journalMode = JournalMode.WAL,
            )
        },
    )

    private fun createDirectory(name: String): String {
        val path = "$root/$name"
        check(fileManager.createDirectoryAtPath(path, true, null, null))
        return path
    }

    private companion object {
        const val DATABASE_NAME = "FeedFlowFeedSyncDB-test"
        const val LOCAL_SOURCES = 200
    }
}
