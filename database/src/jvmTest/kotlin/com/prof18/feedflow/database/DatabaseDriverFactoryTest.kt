package com.prof18.feedflow.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.DesktopDatabaseErrorState
import com.prof18.feedflow.db.FeedFlowDB
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.createTempDirectory
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class DatabaseDriverFactoryTest {

    private lateinit var directory: File
    private lateinit var databasePath: File
    private val logger = Logger.withTag("DatabaseDriverFactoryTest")
    private val openDrivers = mutableListOf<SqlDriver>()

    @BeforeTest
    fun setUp() {
        directory = createTempDirectory("feedflow-db-test").toFile()
        databasePath = File(directory, "FeedFlow.db")
        DesktopDatabaseErrorState.clear()
    }

    @AfterTest
    fun tearDown() {
        openDrivers.forEach { runCatching { it.close() } }
        DesktopDatabaseErrorState.clear()
        directory.deleteRecursively()
    }

    @Test
    fun `validateRequiredSchemaObjects accepts complete schema`() {
        val driver = createCompleteSchemaDriver()

        validateRequiredSchemaObjects(driver)
    }

    @Test
    fun `validateRequiredSchemaObjects rejects missing startup view`() {
        val driver = createCompleteSchemaDriver()
        driver.execute(null, "DROP VIEW feed_source_unread_count;", 0, null)

        assertFailsWith<InvalidDatabaseSchemaException> {
            validateRequiredSchemaObjects(driver)
        }
    }

    @Test
    fun `validateRequiredSchemaObjects rejects current version database missing core tables`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "CREATE TABLE unrelated_table(id TEXT);", 0, null)
        setVersion(driver, FeedFlowDB.Schema.version)

        assertFailsWith<InvalidDatabaseSchemaException> {
            validateRequiredSchemaObjects(driver)
        }
    }

    @Test
    fun `a fresh database is created at the current version`() {
        val driver = open()

        assertEquals(FeedFlowDB.Schema.version, driver.userVersion())
        assertNull(DesktopDatabaseErrorState.recoveredFileName.value)
    }

    @Test
    fun `a locked database fails as Locked without touching any file`() {
        rawConnection().use { lockHolder ->
            lockHolder.createStatement().use {
                it.execute("PRAGMA journal_mode=WAL;")
                it.execute("BEGIN IMMEDIATE;")
            }

            val error = assertFailsWith<DesktopDatabaseStartupException> { open() }

            assertEquals(DesktopDatabaseFailureKind.Locked, error.failure.kind)
            assertTrue(databasePath.exists())
            assertTrue(recoveryFiles().isEmpty())
            assertFalse(markerFile().exists())

            lockHolder.createStatement().use { it.execute("ROLLBACK;") }
        }

        rawConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("PRAGMA user_version;").use { assertEquals(0, it.getInt(1)) }
                statement.executeQuery(
                    "SELECT count(*) FROM sqlite_master WHERE type = 'table';",
                ).use { assertEquals(0, it.getInt(1)) }
            }
        }
    }

    @Test
    fun `a locked healthy database still opens because WAL readers are not blocked`() {
        open().close()
        openDrivers.clear()

        rawConnection().use { lockHolder ->
            lockHolder.createStatement().use { it.execute("BEGIN IMMEDIATE;") }

            val driver = open()

            assertEquals(FeedFlowDB.Schema.version, driver.userVersion())
            lockHolder.createStatement().use { it.execute("ROLLBACK;") }
        }
    }

    @Test
    fun `a file of random bytes is quarantined and replaced`() {
        databasePath.writeBytes(Random(42).nextBytes(8192))

        val driver = open()

        assertEquals(FeedFlowDB.Schema.version, driver.userVersion())
        val recovered = recoveryFiles()
        assertTrue(recovered.isNotEmpty() && recovered.size <= 3, "recovery files: $recovered")
        assertEquals("FeedFlow.db.recovery-20261027T140000Z", DesktopDatabaseErrorState.recoveredFileName.value)
        assertFalse(markerFile().exists())
    }

    @Test
    fun `a database missing a required table is quarantined, not wiped in place`() {
        open().apply { execute(null, "DROP TABLE blocked_word;", 0, null) }.close()
        openDrivers.clear()

        val driver = open()

        assertEquals(FeedFlowDB.Schema.version, driver.userVersion())
        assertTrue(File(directory, "FeedFlow.db.recovery-20261027T140000Z").exists())
    }

    @Test
    fun `tables with user_version 0 are quarantined`() {
        rawConnection().use { connection ->
            connection.createStatement().use { it.execute("CREATE TABLE leftover(id TEXT);") }
        }

        open()

        assertTrue(File(directory, "FeedFlow.db.recovery-20261027T140000Z").exists())
    }

    @Test
    fun `an occupied recovery name gets a numeric suffix`() {
        val occupied = File(directory, "FeedFlow.db.recovery-20261027T140000Z").apply { writeText("older copy") }
        databasePath.writeBytes(Random(7).nextBytes(8192))

        open()

        assertEquals("older copy", occupied.readText())
        assertTrue(File(directory, "FeedFlow.db.recovery-20261027T140000Z-1").exists())
        assertEquals("FeedFlow.db.recovery-20261027T140000Z-1", DesktopDatabaseErrorState.recoveredFileName.value)
    }

    @Test
    fun `a quarantine interrupted after the first move is finished on the next launch`() {
        assertInterruptedQuarantineIsFinished(successfulMoves = 1)
    }

    @Test
    fun `a quarantine interrupted after the second move is finished on the next launch`() {
        assertInterruptedQuarantineIsFinished(successfulMoves = 2)
    }

    @Test
    fun `an unreadable quarantine marker stops startup before SQLite opens`() {
        markerFile().writeText("garbage")

        val error = assertFailsWith<DesktopDatabaseStartupException> { open() }

        assertEquals(DesktopDatabaseFailureKind.StorageUnavailable, error.failure.kind)
        assertFalse(databasePath.exists())
    }

    @Test
    fun `a marker that cannot be deleted stops startup before SQLite opens`() {
        markerFile().writeText("20261027T140000Z")

        val error = assertFailsWith<DesktopDatabaseStartupException> {
            open(deleteFile = { throw IOException("read-only folder") })
        }

        assertEquals(DesktopDatabaseFailureKind.StorageUnavailable, error.failure.kind)
        assertFalse(databasePath.exists())
    }

    @Test
    fun `an interrupted schema creation leaves no tables behind`() {
        val error = assertFailsWith<DesktopDatabaseStartupException> {
            open(wrapDriver = { FailingNthExecuteDriver(it, failingCall = 3) })
        }
        assertEquals(DesktopDatabaseFailureKind.StorageUnavailable, error.failure.kind)

        rawConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT count(*) FROM sqlite_master WHERE type = 'table';",
                ).use { assertEquals(0, it.getInt(1)) }
            }
        }

        assertEquals(FeedFlowDB.Schema.version, open().userVersion())
    }

    private fun assertInterruptedQuarantineIsFinished(successfulMoves: Int) {
        // Driven through DatabaseFileSet: a failed SQLite open deletes stray sidecars on close,
        // so a full open could not leave all three files in place for the injected failure.
        databasePath.writeBytes(Random(successfulMoves).nextBytes(8192))
        File(directory, "FeedFlow.db-wal").writeBytes(ByteArray(64))
        File(directory, "FeedFlow.db-shm").writeBytes(ByteArray(64))

        var moves = 0
        val interrupted = DatabaseFileSet(
            databasePath = databasePath,
            moveFile = { from, to ->
                moves++
                if (moves > successfulMoves) throw IOException("disk went away")
                Files.move(from, to)
            },
            deleteFile = { Files.delete(it) },
            logger = logger,
        )
        val error = assertFailsWith<DesktopDatabaseStartupException> {
            interrupted.quarantine(Instant.parse("2026-10-27T14:00:00Z"))
        }
        assertEquals(DesktopDatabaseFailureKind.StorageUnavailable, error.failure.kind)
        assertTrue(markerFile().exists())
        assertEquals(successfulMoves, recoveryFiles().size)

        val driver = open(now = { Instant.parse("2026-10-28T09:00:00Z") })

        assertEquals(FeedFlowDB.Schema.version, driver.userVersion())
        assertFalse(markerFile().exists())
        assertEquals(
            setOf(
                "FeedFlow.db.recovery-20261027T140000Z",
                "FeedFlow.db-wal.recovery-20261027T140000Z",
                "FeedFlow.db-shm.recovery-20261027T140000Z",
            ),
            recoveryFiles().toSet(),
        )
        assertEquals("FeedFlow.db.recovery-20261027T140000Z", DesktopDatabaseErrorState.recoveredFileName.value)
    }

    private fun open(
        moveFile: (Path, Path) -> Unit = { from, to -> Files.move(from, to) },
        deleteFile: (Path) -> Unit = { Files.delete(it) },
        now: () -> Instant = { Instant.parse("2026-10-27T14:00:00Z") },
        wrapDriver: (SqlDriver) -> SqlDriver = { it },
    ): SqlDriver = openDesktopDatabase(
        databasePath = databasePath,
        logger = logger,
        properties = defaultDriverProperties().apply { setProperty("busy_timeout", TEST_BUSY_TIMEOUT_MS) },
        moveFile = moveFile,
        deleteFile = deleteFile,
        now = now,
        wrapDriver = wrapDriver,
    ).also { openDrivers += it }

    private fun rawConnection(): Connection = DriverManager.getConnection("jdbc:sqlite:${databasePath.absolutePath}")

    private fun markerFile() = File(directory, "FeedFlow.db.quarantine-pending")

    private fun recoveryFiles(): List<String> =
        directory.listFiles().orEmpty().map { it.name }.filter { ".recovery-" in it }

    private fun SqlDriver.userVersion(): Long =
        executeQuery(null, "PRAGMA user_version;", { QueryResult.Value(it.getLong(0)) }, 0, null).value ?: -1L

    private fun createCompleteSchemaDriver(): JdbcSqliteDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FeedFlowDB.Schema.create(driver)
        setVersion(driver, FeedFlowDB.Schema.version)
        return driver
    }

    private class FailingNthExecuteDriver(
        private val delegate: SqlDriver,
        private val failingCall: Int,
    ) : SqlDriver by delegate {
        private var calls = 0

        override fun execute(
            identifier: Int?,
            sql: String,
            parameters: Int,
            binders: (SqlPreparedStatement.() -> Unit)?,
        ): QueryResult<Long> {
            calls++
            if (calls == failingCall) throw SQLiteException("full", SQLiteErrorCode.SQLITE_FULL)
            return delegate.execute(identifier, sql, parameters, binders)
        }
    }

    private companion object {
        const val TEST_BUSY_TIMEOUT_MS = "500"
    }
}
