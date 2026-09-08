package com.prof18.feedflow.feedsync.database.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB

fun validateSyncDatabase(driver: SqlDriver): Long {
    val version = driver.executeQuery(null, "PRAGMA user_version", { cursor ->
        check(cursor.next().value) { "Missing sync database version" }
        QueryResult.Value(requireNotNull(cursor.getLong(0)))
    }, 0).value
    check(version in 0..FeedFlowFeedSyncDB.Schema.version) { "Unsupported sync database version: $version" }

    val integrity = driver.executeQuery(null, "PRAGMA integrity_check", { cursor ->
        val results = mutableListOf<String?>()
        while (cursor.next().value) results.add(cursor.getString(0))
        QueryResult.Value(results)
    }, 0).value
    check(integrity == listOf("ok")) { "Sync database integrity check failed" }

    requiredQueries.forEach { query ->
        driver.executeQuery(null, query, { cursor ->
            cursor.next()
            QueryResult.Unit
        }, 0).value
    }
    return version
}

fun prepareSyncDatabaseSnapshot(driver: SqlDriver) {
    val version = validateSyncDatabase(driver)
    if (version == 0L) {
        // Legacy desktop exports omitted user_version despite containing the complete schema.
        driver.execute(null, "PRAGMA user_version = ${FeedFlowFeedSyncDB.Schema.version}", 0)
    }
}

private val requiredQueries = listOf(
    "SELECT url_hash, is_read, is_bookmarked FROM synced_feed_item LIMIT 0",
    "SELECT url_hash, url, title, category_id, logo_url FROM synced_feed_source LIMIT 0",
    "SELECT id, title FROM synced_feed_source_category LIMIT 0",
    "SELECT table_name, last_change_timestamp FROM sync_metadata LIMIT 0",
)
