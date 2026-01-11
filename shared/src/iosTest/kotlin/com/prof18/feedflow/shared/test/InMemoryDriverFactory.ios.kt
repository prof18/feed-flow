package com.prof18.feedflow.shared.test

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import com.prof18.feedflow.db.FeedFlowDB

actual fun createInMemoryDriver(): SqlDriver = inMemoryDriver(FeedFlowDB.Schema)
