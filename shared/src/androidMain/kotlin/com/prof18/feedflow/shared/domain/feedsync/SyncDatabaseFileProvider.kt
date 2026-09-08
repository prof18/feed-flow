package com.prof18.feedflow.shared.domain.feedsync

import android.content.Context
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import java.io.File

internal interface SyncDatabaseFileProvider {
    val databaseFile: File
    val remoteFileName: String
}

internal class AndroidSyncDatabaseFileProvider(
    context: Context,
    appEnvironment: AppEnvironment,
) : SyncDatabaseFileProvider {
    private val databaseName = if (appEnvironment.isDebug()) {
        SyncedDatabaseHelper.SYNC_DATABASE_NAME_DEBUG
    } else {
        SyncedDatabaseHelper.SYNC_DATABASE_NAME_PROD
    }

    override val databaseFile: File = context.getDatabasePath(databaseName)
    override val remoteFileName: String = "$databaseName.db"
}
