package com.prof18.feedflow.shared.test

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import com.prof18.feedflow.db.FeedFlowDB
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB

actual fun createInMemoryDriver(): SqlDriver = inMemoryDriver(FeedFlowDB.Schema)

actual fun createInMemorySyncDriver(): SqlDriver = inMemoryDriver(FeedFlowFeedSyncDB.Schema)
