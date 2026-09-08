package com.prof18.feedflow.shared.test.cloudsync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.prof18.feedflow.db.FeedFlowDB
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import java.io.File
import java.util.UUID

internal actual fun createCloudDevice(
    provider: CloudProvider,
    store: CloudStore,
    id: String,
    root: String?,
): CloudDevice {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val directory = root?.let(::File)
        ?: context.cacheDir.resolve("feedflow-android-cloud-$id-${UUID.randomUUID()}")
    return createAndroidCloudDevice(
        provider = provider,
        store = store,
        id = id,
        root = directory.absolutePath,
        driverFactory = { file, isSyncDatabase ->
            AndroidSqliteDriver(
                schema = if (isSyncDatabase) FeedFlowFeedSyncDB.Schema else FeedFlowDB.Schema,
                context = context,
                name = file.name,
            )
        },
        useFixtureDatabaseDirectory = false,
        seedAccount = root == null,
    )
}
