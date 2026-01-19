package com.prof18.feedflow.shared.di

import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.NSLogWriter
import co.touchlab.kermit.crashlytics.CrashlyticsLogWriter
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.core.utils.FEEDFLOW_USER_AGENT
import com.prof18.feedflow.database.createDatabaseDriver
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceIos
import com.prof18.feedflow.feedsync.googledrive.GoogleDrivePlatformClientIos
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.i18n.feedFlowStrings
import com.prof18.feedflow.shared.data.KeychainSettingsWrapper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.contentprefetch.ContentPrefetchRepository
import com.prof18.feedflow.shared.domain.contentprefetch.ContentPrefetchRepositoryIosDesktop
import com.prof18.feedflow.shared.domain.feed.SerialFeedFetcherRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncIosWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedbinHistorySyncScheduler
import com.prof18.feedflow.shared.domain.feedsync.FeedbinHistorySyncSchedulerIosDesktop
import com.prof18.feedflow.shared.domain.model.CurrentOS
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.domain.parser.FeedItemContentFileHandlerIos
import com.prof18.feedflow.shared.presentation.AboutAndSupportSettingsViewModel
import com.prof18.feedflow.shared.presentation.AccountsViewModel
import com.prof18.feedflow.shared.presentation.AddFeedViewModel
import com.prof18.feedflow.shared.presentation.BazquxSyncViewModel
import com.prof18.feedflow.shared.presentation.BlockedWordsViewModel
import com.prof18.feedflow.shared.presentation.ChangeFeedCategoryViewModel
import com.prof18.feedflow.shared.presentation.DeeplinkFeedViewModel
import com.prof18.feedflow.shared.presentation.DropboxSyncViewModel
import com.prof18.feedflow.shared.presentation.EditFeedViewModel
import com.prof18.feedflow.shared.presentation.FeedListSettingsViewModel
import com.prof18.feedflow.shared.presentation.FeedSourceListViewModel
import com.prof18.feedflow.shared.presentation.FeedSuggestionsViewModel
import com.prof18.feedflow.shared.presentation.FeedbinSyncViewModel
import com.prof18.feedflow.shared.presentation.FreshRssSyncViewModel
import com.prof18.feedflow.shared.presentation.GoogleDriveSyncViewModel
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.ICloudSyncViewModel
import com.prof18.feedflow.shared.presentation.ImportExportViewModel
import com.prof18.feedflow.shared.presentation.MainSettingsViewModel
import com.prof18.feedflow.shared.presentation.MinifluxSyncViewModel
import com.prof18.feedflow.shared.presentation.ReaderModeViewModel
import com.prof18.feedflow.shared.presentation.ReadingBehaviorSettingsViewModel
import com.prof18.feedflow.shared.presentation.ReviewViewModel
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.presentation.SyncAndStorageSettingsViewModel
import com.prof18.feedflow.shared.utils.Telemetry
import com.prof18.feedflow.shared.utils.UserFeedbackReporter
import com.prof18.rssparser.RssParserBuilder
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind
import org.koin.dsl.module
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.UIKit.UIDevice

fun initKoinIos(
    htmlParser: HtmlParser,
    appEnvironment: AppEnvironment,
    languageCode: String?,
    regionCode: String?,
    dropboxDataSource: DropboxDataSource,
    googleDrivePlatformClient: GoogleDrivePlatformClientIos,
    appVersion: String,
    telemetry: Telemetry,
    feedItemParserWorker: FeedItemParserWorker,
): KoinApplication = initKoin(
    appConfig = AppConfig(
        appEnvironment = appEnvironment,
        isLoggingEnabled = true,
        isDropboxSyncEnabled = true,
        isGoogleDriveSyncEnabled = true,
        isIcloudSyncEnabled = true,
        appVersion = appVersion,
        platformName = UIDevice.currentDevice.systemName(),
        platformVersion = UIDevice.currentDevice.systemVersion,
    ),
    crashReportingLogWriter = CrashlyticsLogWriter(),
    modules = listOf(
        module {
            factory { htmlParser }
            single { dropboxDataSource }
            single { googleDrivePlatformClient }
            single { telemetry }
            single { feedItemParserWorker }
            single<FeedFlowStrings> {
                when {
                    languageCode == null -> EnFeedFlowStrings
                    regionCode == null -> feedFlowStrings[languageCode] ?: EnFeedFlowStrings
                    else -> {
                        val locale = "${languageCode}_$regionCode"
                        feedFlowStrings[locale] ?: feedFlowStrings[languageCode] ?: EnFeedFlowStrings
                    }
                }
            }
        },
    ),
)

internal actual fun getPlatformModule(appEnvironment: AppEnvironment): Module = module {
    single {
        RssParserBuilder(
            nsUrlSession = NSURLSession.sessionWithConfiguration(
                NSURLSessionConfiguration.defaultSessionConfiguration().apply {
                    HTTPAdditionalHeaders = mapOf(
                        "User-Agent" to FEEDFLOW_USER_AGENT,
                    )
                },
            ),
        ).build()
    }

    single<SqlDriver> {
        createDatabaseDriver(appEnvironment)
    }

    single<DispatcherProvider> {
        object : DispatcherProvider {
            override val main: CoroutineDispatcher = Dispatchers.Main
            override val default: CoroutineDispatcher = Dispatchers.Default
            override val io: CoroutineDispatcher = Dispatchers.IO
        }
    }

    single<FeedbinHistorySyncScheduler> {
        FeedbinHistorySyncSchedulerIosDesktop(
            feedbinRepository = get(),
            dispatcherProvider = get(),
            logger = getWith("FeedbinHistorySyncSchedulerIosDesktop"),
        )
    }

    factory {
        OpmlFeedHandler(
            dispatcherProvider = get(),
        )
    }

    single<Settings> {
        KeychainSettingsWrapper.settings
    }

    single {
        GoogleDriveDataSourceIos(
            platformClient = get(),
            googleDriveSettings = get(),
            logger = getWith("GoogleDriveDataSourceIos"),
            dispatcherProvider = get(),
        )
    }

    factory<FeedSyncWorker> {
        FeedSyncIosWorker(
            dispatcherProvider = get(),
            feedSyncMessageQueue = get(),
            dropboxDataSource = get(),
            googleDriveDataSource = get(),
            logger = getWith("FeedSyncIosWorker"),
            feedSyncer = get(),
            appEnvironment = appEnvironment,
            dropboxSettings = get(),
            googleDriveSettings = get(),
            settingsRepository = get(),
            accountsRepository = get(),
            iCloudSettings = get(),
            telemetry = get(),
        )
    }

    viewModel {
        DropboxSyncViewModel(
            logger = getWith("DropboxSyncViewModel"),
            dropboxSettings = get(),
            dropboxDataSource = get(),
            feedSyncRepository = get(),
            dateFormatter = get(),
            feedFetcherRepository = get(),
            feedSyncMessageQueue = get(),
            accountsRepository = get(),
        )
    }

    viewModel {
        GoogleDriveSyncViewModel(
            logger = getWith("GoogleDriveSyncViewModel"),
            googleDriveSettings = get(),
            googleDriveDataSource = get(),
            feedSyncRepository = get(),
            dateFormatter = get(),
            feedFetcherRepository = get(),
            feedSyncMessageQueue = get(),
            accountsRepository = get(),
        )
    }

    factory<CurrentOS> { CurrentOS.Ios }

    single {
        FeedItemContentFileHandlerIos(
            dispatcherProvider = get(),
            logger = getWith("FeedItemContentFileHandlerIos"),
        )
    } bind FeedItemContentFileHandler::class

    factory {
        SerialFeedFetcherRepository(
            dispatcherProvider = get(),
            feedStateRepository = get(),
            gReaderRepository = get(),
            feedbinRepository = get(),
            databaseHelper = get(),
            feedSyncRepository = get(),
            logger = getWith("SerialFeedFetcherRepository"),
            rssParser = get(),
            rssChannelMapper = get(),
            dateFormatter = get(),
        )
    }

    viewModel {
        ICloudSyncViewModel(
            iCloudSettings = get(),
            dateFormatter = get(),
            accountsRepository = get(),
            feedSyncRepository = get(),
            feedFetcherRepository = get(),
            feedSyncMessageQueue = get(),
            logger = getWith("ICloudSyncViewModel"),
        )
    }

    single<ContentPrefetchRepository> {
        ContentPrefetchRepositoryIosDesktop(
            dispatcherProvider = get(),
            settingsRepository = get(),
            databaseHelper = get(),
            feedItemParserWorker = get(),
            feedItemContentFileHandler = get(),
            logger = getWith("ContentPrefetchRepositoryIosDesktop"),
        )
    }
}

internal actual fun platformLogWriters(): List<LogWriter> {
    return listOf(
        NSLogWriter(),
    )
}

@Suppress("unused") // Called from Swift
object Deps : KoinComponent {
    fun getHomeViewModel() = getKoin().get<HomeViewModel>()
    fun getFeedSourceListViewModel() = getKoin().get<FeedSourceListViewModel>()
    fun getAddFeedViewModel() = getKoin().get<AddFeedViewModel>()
    fun getLogger(tag: String? = null) = getKoin().get<Logger> { parametersOf(tag) }
    fun getImportExportViewModel() = getKoin().get<ImportExportViewModel>()
    fun getMainSettingsViewModel() = getKoin().get<MainSettingsViewModel>()
    fun getFeedListSettingsViewModel() = getKoin().get<FeedListSettingsViewModel>()
    fun getReadingBehaviorSettingsViewModel() = getKoin().get<ReadingBehaviorSettingsViewModel>()
    fun getSyncAndStorageSettingsViewModel() = getKoin().get<SyncAndStorageSettingsViewModel>()
    fun getAboutAndSupportSettingsViewModel() = getKoin().get<AboutAndSupportSettingsViewModel>()
    fun getFeedFlowStrings() = getKoin().get<FeedFlowStrings>()
    fun getStrings() = getKoin().get<FeedFlowStrings>()
    fun getSettingsRepository() = getKoin().get<SettingsRepository>()
    fun getSearchViewModel() = getKoin().get<SearchViewModel>()
    fun getAccountsViewModel() = getKoin().get<AccountsViewModel>()
    fun getDropboxDataSource() = getKoin().get<DropboxDataSource>()
    fun getDropboxSyncViewModel() = getKoin().get<DropboxSyncViewModel>()
    fun getGoogleDriveSyncViewModel() = getKoin().get<GoogleDriveSyncViewModel>()
    fun getFeedSyncRepository() = getKoin().get<FeedSyncRepository>()
    fun getICloudSyncViewModel() = getKoin().get<ICloudSyncViewModel>()
    fun getReaderModeViewModel() = getKoin().get<ReaderModeViewModel>()
    fun getEditFeedViewModel() = getKoin().get<EditFeedViewModel>()
    fun getFreshRssSyncViewModel() = getKoin().get<FreshRssSyncViewModel>()
    fun getMinifluxSyncViewModel() = getKoin().get<MinifluxSyncViewModel>()
    fun getBazquxSyncViewModel() = getKoin().get<BazquxSyncViewModel>()
    fun getDeeplinkFeedViewModel() = getKoin().get<DeeplinkFeedViewModel>()
    fun getReviewViewModel() = getKoin().get<ReviewViewModel>()
    fun getSerialFeedFetcherRepository() = getKoin().get<SerialFeedFetcherRepository>()
    fun getBlockedWordsViewModel() = getKoin().get<BlockedWordsViewModel>()
    fun getHtmlRetriever() = getKoin().get<HtmlRetriever>()
    fun getContentPrefetchManager() = getKoin().get<ContentPrefetchRepository>()
    fun getUserFeedbackReporter() = getKoin().get<UserFeedbackReporter>()
    fun getChangeFeedCategoryViewModel() = getKoin().get<ChangeFeedCategoryViewModel>()
    fun getFeedSuggestionsViewModel() = getKoin().get<FeedSuggestionsViewModel>()
    fun getFeedbinSyncViewModel() = getKoin().get<FeedbinSyncViewModel>()
}
