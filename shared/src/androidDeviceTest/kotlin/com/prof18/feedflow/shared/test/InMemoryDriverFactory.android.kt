package com.prof18.feedflow.shared.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.prof18.feedflow.db.FeedFlowDB
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import java.util.UUID

actual fun createInMemoryDriver(): SqlDriver = AndroidSqliteDriver(
    schema = FeedFlowDB.Schema,
    context = ApplicationProvider.getApplicationContext<Context>(),
    name = "feedflow-test-main-${UUID.randomUUID()}.db",
)

actual fun createInMemorySyncDriver(): SqlDriver = AndroidSqliteDriver(
    schema = FeedFlowFeedSyncDB.Schema,
    context = ApplicationProvider.getApplicationContext<Context>(),
    name = "feedflow-test-sync-${UUID.randomUUID()}.db",
)
