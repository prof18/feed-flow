package com.prof18.feedflow.database

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.AppDataPathBuilder
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DesktopDatabaseErrorState
import com.prof18.feedflow.db.FeedFlowDB
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.time.Clock
import kotlin.time.Instant

fun createDatabaseDriver(
    appEnvironment: AppEnvironment,
    logger: Logger,
): SqlDriver {
    val appPath = AppDataPathBuilder.getAppDataPath(appEnvironment)
    return openDesktopDatabase(
        databasePath = File(appPath, DatabaseHelper.DB_FILE_NAME_WITH_EXTENSION),
        logger = logger,
    )
}

/**
 * Opens the desktop database without ever deleting user data. Only confirmed corruption replaces the
 * file, and the old files are first moved aside under a recovery name. Every other failure surfaces as
 * a [DesktopDatabaseStartupException] with the files untouched.
 */
internal fun openDesktopDatabase(
    databasePath: File,
    logger: Logger,
    properties: Properties = defaultDriverProperties(),
    moveFile: (from: Path, to: Path) -> Unit = { from, to -> Files.move(from, to) },
    deleteFile: (Path) -> Unit = { Files.delete(it) },
    now: () -> Instant = { Clock.System.now() },
    wrapDriver: (SqlDriver) -> SqlDriver = { it },
): SqlDriver {
    val files = DatabaseFileSet(databasePath, moveFile, deleteFile, logger)
    files.finishInterruptedQuarantine()?.let { DesktopDatabaseErrorState.setRecovered(it) }

    val firstError = try {
        return openAndInitialize(databasePath, properties, logger, wrapDriver)
    } catch (e: Throwable) {
        e
    }

    val failure = firstError.classifyDesktopDatabaseFailure() ?: throw firstError
    if (failure.kind != DesktopDatabaseFailureKind.Corrupt) {
        logger.e(firstError) { "Database unavailable: ${failure.kind} ${failure.sqliteResultCode}" }
        throw DesktopDatabaseStartupException(failure, firstError)
    }

    logger.w { "Database corrupt (${failure.sqliteResultCode}), moving it aside" }
    val recoveredName = files.quarantine(now())
    val driver = openFreshDatabase(databasePath, properties, logger, wrapDriver)
    DesktopDatabaseErrorState.setRecovered(recoveredName)
    return driver
}

private fun openFreshDatabase(
    databasePath: File,
    properties: Properties,
    logger: Logger,
    wrapDriver: (SqlDriver) -> SqlDriver,
): SqlDriver = try {
    openAndInitialize(databasePath, properties, logger, wrapDriver)
} catch (e: Throwable) {
    val failure = e.classifyDesktopDatabaseFailure()
        ?: DesktopDatabaseFailure(DesktopDatabaseFailureKind.Unexpected, sqliteResultCode = null)
    throw DesktopDatabaseStartupException(failure, e)
}

internal fun defaultDriverProperties(): Properties = Properties().apply {
    setProperty("journal_mode", "WAL")
    setProperty("synchronous", "NORMAL")
    setProperty("busy_timeout", "30000")
}

private fun openAndInitialize(
    databasePath: File,
    properties: Properties,
    logger: Logger,
    wrapDriver: (SqlDriver) -> SqlDriver,
): SqlDriver {
    databasePath.parentFile?.mkdirs()
    val driver = wrapDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY + databasePath.absolutePath, properties))
    return try {
        initDatabase(driver, logger)
    } catch (e: Throwable) {
        try {
            driver.close()
        } catch (closeError: Throwable) {
            e.addSuppressed(closeError)
        }
        throw e
    }
}

private fun initDatabase(driver: SqlDriver, logger: Logger): SqlDriver {
    val schemaVersion = FeedFlowDB.Schema.version

    val sqlCursor = driver.executeQuery(
        null,
        "PRAGMA user_version;",
        {
            QueryResult.Value(it.getLong(0))
        },
        0,
        null,
    )
    val currentVer: Long = sqlCursor.value ?: -1L

    if (currentVer == 0L) {
        if (hasAnyUserTables(driver)) {
            throw InvalidDatabaseSchemaException("Tables present with user_version 0")
        }
        runSchemaChange(driver) {
            FeedFlowDB.Schema.create(driver)
            setVersion(driver, schemaVersion)
        }
        logger.d("init: created tables, setVersion to $schemaVersion")
    } else {
        if (schemaVersion > currentVer) {
            runSchemaChange(driver) {
                FeedFlowDB.Schema.migrate(driver, oldVersion = currentVer, newVersion = schemaVersion)
                setVersion(driver, schemaVersion)
            }
            logger.d("init: migrated from $currentVer to $schemaVersion")
        } else {
            logger.d("init with existing database")
        }
    }
    validateRequiredSchemaObjects(driver)
    return driver
}

// A schema change interrupted halfway (e.g. disk full) must not leave new tables behind with an old
// user_version: the next launch would fail on "table already exists" forever.
private fun runSchemaChange(driver: SqlDriver, change: () -> Unit) {
    object : TransacterImpl(driver) {}.transaction { change() }
}

fun setVersion(driver: SqlDriver, version: Long) {
    driver.execute(null, "PRAGMA user_version = $version;", 0, null)
}

private fun hasAnyUserTables(driver: SqlDriver): Boolean {
    val cursor = driver.executeQuery(
        null,
        "SELECT count(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%';",
        {
            QueryResult.Value(it.getLong(0))
        },
        0,
        null,
    )
    return (cursor.value ?: 0L) > 0L
}

internal fun validateRequiredSchemaObjects(driver: SqlDriver) {
    val missingObjects = buildList {
        requiredTables.filterNot { driver.hasSchemaObject(type = "table", name = it) }
            .mapTo(this) { "table:$it" }
        requiredViews.filterNot { driver.hasSchemaObject(type = "view", name = it) }
            .mapTo(this) { "view:$it" }
    }

    if (missingObjects.isNotEmpty()) {
        throw InvalidDatabaseSchemaException(
            "Missing required database schema objects: ${missingObjects.joinToString()}",
        )
    }
}

private fun SqlDriver.hasSchemaObject(
    type: String,
    name: String,
): Boolean {
    val cursor = executeQuery(
        null,
        "SELECT count(*) FROM sqlite_master WHERE type = ? AND name = ?;",
        { QueryResult.Value(it.getLong(0)) },
        2,
        {
            bindString(0, type)
            bindString(1, name)
        },
    )
    return (cursor.value ?: 0L) > 0L
}

private val requiredTables = listOf(
    "blocked_word",
    "content_prefetch_queue",
    "deleted_feed_items",
    "feed_item_ids_for_checks",
    "feed_item_status",
    "feed_source",
    "feed_source_cache_info",
    "feed_item",
    "feed_search",
    "feed_source_category",
    "feed_source_preferences",
    "read_status_pending_action",
    "sync_metadata",
)

private val requiredViews = listOf(
    "feed_source_unread_count",
    "category_with_unread",
)
