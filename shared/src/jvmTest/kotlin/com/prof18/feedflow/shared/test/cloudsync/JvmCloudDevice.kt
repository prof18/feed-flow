package com.prof18.feedflow.shared.test.cloudsync

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.db.FeedFlowDB
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import com.prof18.feedflow.feedsync.database.di.FEED_SYNC_SCOPE_NAME
import com.prof18.feedflow.feedsync.database.di.SYNC_DB_DRIVER
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceJvm
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncJvmWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.domain.feedsync.PendingCloudChangesManager
import com.prof18.feedflow.shared.test.TestDispatcherProvider
import com.prof18.feedflow.shared.test.koin.TestModules
import com.prof18.feedflow.shared.test.testLogger
import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.io.File
import java.nio.file.Files
import java.util.Properties

internal actual fun createCloudDevice(
    provider: CloudProvider,
    store: CloudStore,
    id: String,
    root: String?,
): CloudDevice {
    val directory = root?.let(::File) ?: Files.createTempDirectory("feedflow-cloud-$id-").toFile()
    val preferencesFile = File(directory, "settings.properties")
    val properties = Properties().apply {
        if (preferencesFile.exists()) preferencesFile.inputStream().use { load(it) }
    }
    val settings = PropertiesSettings(properties) { values ->
        preferencesFile.outputStream().use { values.store(it, null) }
    }
    if (root == null) selectCloudAccount(provider, settings)
    val syncFile = File(directory, "${SyncedDatabaseHelper.SYNC_DATABASE_NAME_DEBUG}.db")
    val workerScope = CoroutineScope(SupervisorJob() + TestDispatcherProvider.testDispatcher)
    val application = koinApplication {
        allowOverride(true)
        modules(
            TestModules.createTestModules() + module {
                single<Settings> { settings }
                single<SqlDriver> { mainDriver(File(directory, "main.db")) }
                single {
                    SyncedDatabaseHelper(
                        TestDispatcherProvider.testDispatcher,
                        koinContext = getKoin(),
                        clock = CloudHarnessClock,
                    )
                }
                scope(named(FEED_SYNC_SCOPE_NAME)) {
                    scoped<SqlDriver>(named(SYNC_DB_DRIVER)) { syncDriver(syncFile) }
                }
                single<DropboxDataSource> { JvmDropboxTransport(store, id) }
                single<GoogleDriveDataSourceJvm> { JvmDriveTransport(store, id, get()) }
                single<FeedSyncWorker> {
                    FeedSyncJvmWorker(
                        dropboxDataSource = get(),
                        googleDriveDataSource = get(),
                        appEnvironment = AppEnvironment.Debug,
                        logger = testLogger,
                        feedSyncer = get(),
                        pendingCloudChanges = get<PendingCloudChangesManager>(),
                        feedSyncMessageQueue = get(),
                        settingsRepository = get(),
                        dispatcherProvider = TestDispatcherProvider,
                        dropboxSettings = get(),
                        googleDriveSettings = get(),
                        accountsRepository = get(),
                        iCloudSettings = get(),
                        syncDirectory = directory,
                        scope = workerScope,
                        iCloudBridge = JvmICloudTransport(store, id, syncFile),
                    )
                }
            },
        )
    }
    return CloudDevice(application, directory.absolutePath) { workerScope.cancel() }
}

private fun mainDriver(file: File): SqlDriver {
    val exists = file.exists()
    return openDriver(file).also {
        if (!exists) {
            FeedFlowDB.Schema.create(it)
            it.execute(null, "PRAGMA user_version = ${FeedFlowDB.Schema.version}", 0)
        }
    }
}

private fun syncDriver(file: File): SqlDriver {
    val exists = file.exists()
    return openDriver(file).also {
        if (!exists) {
            FeedFlowFeedSyncDB.Schema.create(it)
            it.execute(null, "PRAGMA user_version = ${FeedFlowFeedSyncDB.Schema.version}", 0)
        }
    }
}

private fun openDriver(file: File): JdbcSqliteDriver = JdbcSqliteDriver(
    "jdbc:sqlite:${file.absolutePath}",
    Properties().apply {
        setProperty("journal_mode", "WAL")
        setProperty("synchronous", "NORMAL")
        setProperty("busy_timeout", "30000")
    },
)

internal fun selectCloudAccount(provider: CloudProvider, settings: Settings) {
    when (provider) {
        CloudProvider.DROPBOX -> DropboxSettings(settings).setDropboxData("fixture-credentials")
        CloudProvider.GOOGLE_DRIVE -> GoogleDriveSettings(settings).setGoogleDriveLinked(true)
        CloudProvider.ICLOUD -> ICloudSettings(settings).setUseICloud(true)
    }
}
