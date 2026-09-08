package com.prof18.feedflow.feedsync.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class DatabaseDriverFactoryTest {
    private val logger = Logger.withTag("DatabaseDriverFactoryTest")

    @Test
    fun `absent database is initialized and reopen preserves data`() = withTempDirectory { directory ->
        val databasePath = File(directory, "sync.db")

        openSyncDatabase(databasePath, logger).use { driver ->
            insertItem(driver, "item-1")
        }
        openSyncDatabase(databasePath, logger).use { driver ->
            assertEquals(1L, countRows(driver, "synced_feed_item"))
            assertEquals(FeedFlowFeedSyncDB.Schema.version.toLong(), userVersion(driver))
        }
    }

    @Test
    fun `legacy version zero schema preserves rows and upgrades to version one`() = withTempDirectory { directory ->
        val databasePath = File(directory, "legacy.db")
        createDatabase(databasePath) { driver ->
            createSyncTables(driver)
            insertItem(driver, "legacy-item")
            setVersion(driver, 0)
        }

        openSyncDatabase(databasePath, logger).use { driver ->
            assertEquals(1L, countRows(driver, "synced_feed_item"))
            assertEquals(FeedFlowFeedSyncDB.Schema.version.toLong(), userVersion(driver))
        }
    }

    @Test
    fun `future schema version is rejected without changing file`() = withTempDirectory { directory ->
        val databasePath = File(directory, "future.db")
        createDatabase(databasePath) { driver ->
            createSyncTables(driver)
            insertItem(driver, "future-item")
            setVersion(driver, FeedFlowFeedSyncDB.Schema.version.toLong() + 1)
        }
        val before = databasePath.readBytes()

        assertFails { openSyncDatabase(databasePath, logger) }

        assertContentEquals(before, databasePath.readBytes())
        createDatabase(databasePath) { driver ->
            assertEquals(1L, countRows(driver, "synced_feed_item"))
            assertEquals(FeedFlowFeedSyncDB.Schema.version.toLong() + 1, userVersion(driver))
        }
    }

    @Test
    fun `incomplete schema is rejected while existing rows remain`() = withTempDirectory { directory ->
        val databasePath = File(directory, "incomplete.db")
        createDatabase(databasePath) { driver ->
            driver.execute(null, "CREATE TABLE synced_feed_item (url_hash TEXT NOT NULL PRIMARY KEY);", 0, null)
            driver.execute(null, "INSERT INTO synced_feed_item(url_hash) VALUES ('incomplete-item')", 0)
            setVersion(driver, FeedFlowFeedSyncDB.Schema.version.toLong())
        }
        val before = databasePath.readBytes()

        assertFails { openSyncDatabase(databasePath, logger) }

        assertContentEquals(before, databasePath.readBytes())
        createDatabase(databasePath) { driver ->
            assertEquals(1L, countRows(driver, "synced_feed_item"))
        }
    }

    @Test
    fun `invalid sqlite file is rejected unchanged`() = withTempDirectory { directory ->
        val databasePath = File(directory, "invalid.db")
        val bytes = "not a sqlite database".encodeToByteArray()
        databasePath.writeBytes(bytes)

        assertFails { openSyncDatabase(databasePath, logger) }

        assertContentEquals(bytes, databasePath.readBytes())
    }

    @Test
    fun `directory database path is rejected without replacing directory`() = withTempDirectory { directory ->
        val databasePath = File(directory, "sync.db").apply { mkdir() }
        val marker = File(databasePath, "marker").apply { writeText("keep") }

        assertFails { openSyncDatabase(databasePath, logger) }

        assertTrue(databasePath.isDirectory)
        assertEquals("keep", marker.readText())
    }

    @Test
    fun `unwritable database parent is rejected without replacing parent`() = withTempDirectory { directory ->
        val parentFile = File(directory, "parent").apply { writeText("keep") }
        val databasePath = File(parentFile, "sync.db")

        assertFails { openSyncDatabase(databasePath, logger) }

        assertTrue(parentFile.isFile)
        assertEquals("keep", parentFile.readText())
    }

    private fun createDatabase(databasePath: File, block: (JdbcSqliteDriver) -> Unit) {
        JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}").use { driver -> block(driver) }
    }

    private fun createSyncTables(driver: SqlDriver) {
        FeedFlowFeedSyncDB.Schema.create(driver)
    }

    private fun insertItem(driver: SqlDriver, urlHash: String) {
        FeedFlowFeedSyncDB(driver).syncedFeedItemQueries.insertOrReplaceSyncedFeedItem(urlHash, false, false)
    }

    private fun countRows(driver: SqlDriver, table: String): Long =
        driver.executeQuery(null, "SELECT count(*) FROM $table", { QueryResult.Value(it.getLong(0)) }, 0, null).value
            ?: 0L

    private fun userVersion(driver: SqlDriver): Long =
        driver.executeQuery(null, "PRAGMA user_version", { QueryResult.Value(it.getLong(0)) }, 0, null).value
            ?: -1L

    private fun setVersion(driver: SqlDriver, version: Long) {
        driver.execute(null, "PRAGMA user_version = $version", 0, null)
    }

    private fun withTempDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("feedflow-database-test").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
