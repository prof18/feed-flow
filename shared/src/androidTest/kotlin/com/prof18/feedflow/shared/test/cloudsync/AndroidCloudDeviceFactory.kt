package com.prof18.feedflow.shared.test.cloudsync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.database.di.FEED_SYNC_SCOPE_NAME
import com.prof18.feedflow.feedsync.database.di.SYNC_DB_DRIVER
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceAndroid
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncAndroidWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.domain.feedsync.PendingCloudChangesManager
import com.prof18.feedflow.shared.domain.feedsync.SyncDatabaseFileProvider
import com.prof18.feedflow.shared.test.TestDispatcherProvider
import com.prof18.feedflow.shared.test.koin.TestModules
import com.prof18.feedflow.shared.test.testLogger
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.io.File
import java.util.UUID

internal fun createAndroidCloudDevice(
    provider: CloudProvider,
    store: CloudStore,
    id: String,
    root: String?,
    driverFactory: (File, Boolean) -> SqlDriver,
    useFixtureDatabaseDirectory: Boolean,
    seedAccount: Boolean,
): CloudDevice {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val directory = root?.let(::File)
        ?: context.cacheDir.resolve("feedflow-android-cloud-$id-${UUID.randomUUID()}")
    directory.mkdirs()
    val token = directory.name
    val settings = SharedPreferencesSettings(
        context.getSharedPreferences("cloud-sync-$token-settings", Context.MODE_PRIVATE),
    )
    if (seedAccount) selectCloudAccount(provider, settings)

    val mainFile = File(directory, "$token-main.db")
    val syncFile = File(directory, "$token-sync.db")
    val application = koinApplication {
        allowOverride(true)
        modules(
            TestModules.createTestModules() + module {
                single<Context> { context }
                single<Settings> { settings }
                single<SqlDriver> { driverFactory(mainFile, false) }
                single {
                    SyncedDatabaseHelper(
                        TestDispatcherProvider.testDispatcher,
                        koinContext = getKoin(),
                        clock = CloudHarnessClock,
                    )
                }
                scope(named(FEED_SYNC_SCOPE_NAME)) {
                    scoped<SqlDriver>(named(SYNC_DB_DRIVER)) { driverFactory(syncFile, true) }
                }
                single<DropboxDataSource> { AndroidDropboxTransport(store, id) }
                single<GoogleDriveDataSourceAndroid> { AndroidDriveTransport(store, id, get()) }
                single<FeedSyncAndroidWorker> {
                    FeedSyncAndroidWorker(
                        context = get(),
                        dropboxDataSource = get(),
                        googleDriveDataSource = get(),
                        syncDatabaseFileProvider = object : SyncDatabaseFileProvider {
                            override val databaseFile = if (useFixtureDatabaseDirectory) {
                                syncFile
                            } else {
                                context.getDatabasePath(syncFile.name)
                            }
                            override val remoteFileName = "${SyncedDatabaseHelper.SYNC_DATABASE_NAME_DEBUG}.db"
                        },
                        logger = testLogger,
                        feedSyncer = get(),
                        pendingCloudChanges = get<PendingCloudChangesManager>(),
                        feedSyncMessageQueue = get(),
                        dispatcherProvider = TestDispatcherProvider,
                        dropboxSettings = get(),
                        googleDriveSettings = get(),
                        settingsRepository = get(),
                        accountsRepository = get(),
                    )
                }
                single<FeedSyncWorker> { get<FeedSyncAndroidWorker>() }
            },
        )
    }
    return CloudDevice(application, directory.absolutePath)
}

private fun selectCloudAccount(provider: CloudProvider, settings: Settings) {
    when (provider) {
        CloudProvider.DROPBOX -> DropboxSettings(settings).setDropboxData("fixture-credentials")
        CloudProvider.GOOGLE_DRIVE -> GoogleDriveSettings(settings).setGoogleDriveLinked(true)
        CloudProvider.ICLOUD -> error("iCloud is unsupported by Android")
    }
}
