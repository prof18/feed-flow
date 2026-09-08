package com.prof18.feedflow.shared.test.cloudsync

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.JournalMode
import com.prof18.feedflow.core.model.DropboxClientStatus
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.db.FeedFlowDB
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import com.prof18.feedflow.feedsync.database.di.FEED_SYNC_SCOPE_NAME
import com.prof18.feedflow.feedsync.database.di.SYNC_DB_DRIVER
import com.prof18.feedflow.feedsync.dropbox.DatabaseDestinationUrl
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.dropbox.DropboxDownloadParam
import com.prof18.feedflow.feedsync.dropbox.DropboxDownloadResult
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.dropbox.DropboxStringCredentials
import com.prof18.feedflow.feedsync.dropbox.DropboxUploadParam
import com.prof18.feedflow.feedsync.dropbox.DropboxUploadResult
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceIos
import com.prof18.feedflow.feedsync.googledrive.GoogleDrivePlatformClientIos
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.feedsync.icloud.ICloudDataSource
import com.prof18.feedflow.feedsync.icloud.ICloudDownloadResult
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.feedsync.icloud.ICloudUploadResult
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncIosWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.domain.feedsync.PendingCloudChangesManager
import com.prof18.feedflow.shared.test.TestDispatcherProvider
import com.prof18.feedflow.shared.test.koin.TestModules
import com.prof18.feedflow.shared.test.koin.getWith
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.getBytes
import platform.Foundation.writeToURL

private const val CLOUD_ACCOUNT = "fixture-account"

internal actual fun createCloudDevice(
    provider: CloudProvider,
    store: CloudStore,
    id: String,
    root: String?,
): CloudDevice {
    val isFresh = root == null
    val databaseRoot = root ?: "${NSTemporaryDirectoryPath()}/feedflow-cloudsync-$id-${NSUUID.UUID().UUIDString}"
    val downloadRoot = "$databaseRoot/downloads"
    createDirectory(databaseRoot)
    createDirectory(downloadRoot)

    val settings = NSUserDefaultsSettings(
        NSUserDefaults(suiteName = "com.prof18.feedflow.cloudsync.${databaseRoot.hashCode()}"),
    )
    if (isFresh) {
        selectCloudAccount(provider, settings)
    }
    val workerScope = CoroutineScope(SupervisorJob() + TestDispatcherProvider.testDispatcher)

    val appModules = TestModules.createTestModules() + cloudDeviceModule(
        store = store,
        deviceId = id,
        databaseRoot = databaseRoot,
        downloadRoot = downloadRoot,
        settings = settings,
        workerScope = workerScope,
    )

    val application = koinApplication {
        allowOverride(true)
        modules(appModules)
    }

    return CloudDevice(
        application = application,
        root = databaseRoot,
        closeResources = { workerScope.cancel() },
    )
}

private fun cloudDeviceModule(
    store: CloudStore,
    deviceId: String,
    databaseRoot: String,
    downloadRoot: String,
    settings: Settings,
    workerScope: CoroutineScope,
): Module = module {
    single<SqlDriver> {
        NativeSqliteDriver(
            schema = FeedFlowDB.Schema,
            onConfiguration = { configuration ->
                configuration.copy(
                    extendedConfig = configuration.extendedConfig.copy(basePath = databaseRoot),
                )
            },
            name = DatabaseHelper.APP_DATABASE_NAME_DEBUG,
        )
    }
    single {
        SyncedDatabaseHelper(
            backgroundDispatcher = TestDispatcherProvider.testDispatcher,
            koinContext = getKoin(),
            clock = CloudHarnessClock,
        )
    }
    scope(named(FEED_SYNC_SCOPE_NAME)) {
        scoped<SqlDriver>(named(SYNC_DB_DRIVER)) {
            NativeSqliteDriver(
                schema = FeedFlowFeedSyncDB.Schema,
                onConfiguration = { configuration ->
                    configuration.copy(
                        extendedConfig = configuration.extendedConfig.copy(
                            basePath = databaseRoot,
                            busyTimeout = 30000,
                        ),
                        journalMode = JournalMode.WAL,
                    )
                },
                name = SyncedDatabaseHelper.SYNC_DATABASE_NAME_DEBUG,
            )
        }
    }
    single<Settings> {
        settings
    }
    single<DropboxDataSource> {
        CloudDropboxDataSource(store = store, deviceId = deviceId, outputDirectory = downloadRoot)
    }
    single<GoogleDrivePlatformClientIos> {
        CloudGoogleDrivePlatformClient(store = store, deviceId = deviceId)
    }
    single {
        GoogleDriveDataSourceIos(
            platformClient = get(),
            googleDriveSettings = get(),
            logger = getWith("GoogleDriveDataSourceIos"),
            dispatcherProvider = get(),
            outputDirectory = downloadRoot,
        )
    }
    single<ICloudDataSource> {
        CloudICloudDataSource(
            store = store,
            deviceId = deviceId,
            root = downloadRoot,
        )
    }
    single<FeedSyncWorker> {
        FeedSyncIosWorker(
            dispatcherProvider = get(),
            feedSyncMessageQueue = get(),
            dropboxDataSource = get(),
            googleDriveDataSource = get(),
            iCloudDataSource = get(),
            logger = getWith("FeedSyncIosWorker"),
            feedSyncer = get(),
            pendingCloudChanges = get<PendingCloudChangesManager>(),
            appEnvironment = AppEnvironment.Debug,
            dropboxSettings = get(),
            googleDriveSettings = get(),
            settingsRepository = get(),
            accountsRepository = get(),
            iCloudSettings = get(),
            databaseDirectory = databaseRoot,
            scope = workerScope,
        )
    }
}

private class CloudDropboxDataSource(
    private val store: CloudStore,
    private val deviceId: String,
    private val outputDirectory: String,
) : DropboxDataSource {
    private var linked = false

    override fun setup(apiKey: String) = Unit

    override fun startAuthorization(platformAuthHandler: () -> Unit) = platformAuthHandler()

    override fun handleOAuthResponse(platformOAuthResponseHandler: () -> Unit) = platformOAuthResponseHandler()

    override fun restoreAuth(stringCredentials: DropboxStringCredentials): DropboxClientStatus {
        linked = true
        return DropboxClientStatus.LINKED
    }

    override fun saveAuth(stringCredentials: DropboxStringCredentials) {
        linked = true
    }

    override suspend fun revokeAccess() {
        linked = false
    }

    override fun isClientSet(): Boolean = linked

    override suspend fun performUpload(uploadParam: DropboxUploadParam): DropboxUploadResult {
        store.beforeUploadRead()
        val bytes = requireNotNull(NSData.create(contentsOfURL = uploadParam.url)).toByteArray()
        val fileId = store.upload(
            provider = CloudProvider.DROPBOX,
            account = CLOUD_ACCOUNT,
            name = uploadParam.path.substringAfterLast('/'),
            bytes = bytes,
            deviceId = deviceId,
        )
        return DropboxUploadResult(fileId, 0L, bytes.size.toLong(), null)
    }

    override suspend fun performDownload(downloadParam: DropboxDownloadParam): DropboxDownloadResult {
        val name = downloadParam.path.substringAfterLast('/')
        val bytes = store.download(CloudProvider.DROPBOX, CLOUD_ACCOUNT, name)
        val destination = requireNotNull(
            NSURL.fileURLWithPath(outputDirectory).URLByAppendingPathComponent(downloadParam.outputName),
        )
        destination.writeData(bytes)
        return DropboxDownloadResult(
            id = name,
            sizeInByte = bytes.size.toLong(),
            contentHash = null,
            destinationUrl = DatabaseDestinationUrl(destination),
        )
    }
}

private class CloudGoogleDrivePlatformClient(
    private val store: CloudStore,
    private val deviceId: String,
) : GoogleDrivePlatformClientIos {
    override fun authenticate(onResult: (Boolean) -> Unit) = onResult(true)

    override fun restorePreviousSignIn(onResult: (Boolean) -> Unit) = onResult(true)

    override fun isAuthorized(): Boolean = true

    override fun isServiceSet(): Boolean = true

    override fun signOut() = Unit

    override fun uploadFile(
        data: NSData,
        fileName: String,
        existingFileId: String?,
        completionHandler: (String?, Throwable?) -> Unit,
    ) {
        val fileId = store.upload(
            provider = CloudProvider.GOOGLE_DRIVE,
            account = CLOUD_ACCOUNT,
            name = fileName,
            bytes = data.toByteArray(),
            deviceId = deviceId,
        )
        completionHandler(fileId, null)
    }

    override fun downloadFile(
        fileName: String,
        existingFileId: String?,
        completionHandler: (NSData?, String?, Throwable?) -> Unit,
    ) {
        runCatching {
            store.download(CloudProvider.GOOGLE_DRIVE, CLOUD_ACCOUNT, fileName).toNSData()
        }.fold(
            onSuccess = { completionHandler(it, store.snapshot().single().fileId, null) },
            onFailure = { completionHandler(null, null, it) },
        )
    }
}

private class CloudICloudDataSource(
    private val store: CloudStore,
    private val deviceId: String,
    private val root: String,
) : ICloudDataSource {
    override suspend fun performUpload(databasePath: NSURL, databaseName: String): ICloudUploadResult {
        store.beforeUploadRead()
        store.uploadFailure?.let { return ICloudUploadResult.Error.UploadFailed(it.toString()) }
        val bytes = requireNotNull(NSData.create(contentsOfURL = databasePath)).toByteArray()
        store.upload(CloudProvider.ICLOUD, CLOUD_ACCOUNT, databaseName, bytes, deviceId)
        return ICloudUploadResult.Success
    }

    override suspend fun performDownload(databaseName: String): ICloudDownloadResult {
        store.downloadFailure?.let { return ICloudDownloadResult.Error.DownloadFailed(it.toString()) }
        val bytes = runCatching { store.download(CloudProvider.ICLOUD, CLOUD_ACCOUNT, databaseName) }
            .getOrElse { return ICloudDownloadResult.Error.FileNotFound }
        val destination = requireNotNull(
            NSURL.fileURLWithPath(root).URLByAppendingPathComponent(databaseName),
        )
        destination.writeData(bytes)
        return ICloudDownloadResult.Success(destination)
    }

    override suspend fun getICloudBaseFolderURL(timeoutSeconds: Int, initialPollIntervalMs: Long): NSURL =
        NSURL.fileURLWithPath(root)
}

private fun createDirectory(path: String) {
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = path,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
}

private fun selectCloudAccount(provider: CloudProvider, settings: Settings) {
    when (provider) {
        CloudProvider.DROPBOX -> DropboxSettings(settings).setDropboxData("fixture-credentials")
        CloudProvider.GOOGLE_DRIVE -> GoogleDriveSettings(settings).setGoogleDriveLinked(true)
        CloudProvider.ICLOUD -> ICloudSettings(settings).setUseICloud(true)
    }
}

private fun NSTemporaryDirectoryPath(): String = platform.Foundation.NSTemporaryDirectory().trimEnd('/')

private fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isNotEmpty()) {
        result.usePinned { pinned -> getBytes(pinned.addressOf(0), length) }
    }
    return result
}

private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData()
    } else {
        usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }

private fun NSURL.writeData(bytes: ByteArray) {
    bytes.toNSData().writeToURL(this, atomically = true)
}
