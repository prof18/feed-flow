package com.prof18.feedflow.feedsync.database.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import java.io.File

fun prepareSyncDatabaseFile(context: Context, file: File) {
    AndroidSqliteDriver(
        schema = FeedFlowFeedSyncDB.Schema,
        context = context,
        name = file.absolutePath,
        callback = object : SupportSQLiteOpenHelper.Callback(FeedFlowFeedSyncDB.Schema.version.toInt()) {
            override fun onCreate(db: SupportSQLiteDatabase) = Unit
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
                error("Unsupported sync database version: $oldVersion")
            override fun onCorruption(db: SupportSQLiteDatabase) = error("Invalid sync database download")
        },
    ).use { prepareSyncDatabaseSnapshot(it) }
}
