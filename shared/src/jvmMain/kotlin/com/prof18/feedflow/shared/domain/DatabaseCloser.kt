package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper

class DatabaseCloser(
    private val databaseHelper: DatabaseHelper,
    private val syncedDatabaseHelper: SyncedDatabaseHelper,
) {
    fun close() {
        databaseHelper.close()
        syncedDatabaseHelper.closeScope()
    }
}
