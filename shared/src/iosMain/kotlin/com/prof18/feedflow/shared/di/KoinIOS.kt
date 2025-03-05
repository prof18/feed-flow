package com.prof18.feedflow.shared.di

import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.crashlytics.CrashlyticsLogWriter
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DatabaseFileMigration
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.database.createDatabaseDriver
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.i18n.feedFlowStrings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.browser.BrowserSettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncIosWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.domain.model.CurrentOS
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.presentation.AccountsViewModel
import com.prof18.feedflow.shared.presentation.AddFeedViewModel
import com.prof18.feedflow.shared.presentation.DeeplinkFeedViewModel
import com.prof18.feedflow.shared.presentation.DropboxSyncViewModel
import com.prof18.feedflow.shared.presentation.EditFeedViewModel
import com.prof18.feedflow.shared.presentation.FeedSourceListViewModel
import com.prof18.feedflow.shared.presentation.FreshRssSyncViewModel
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.ICloudSyncViewModel
import com.prof18.feedflow.shared.presentation.ImportExportViewModel
import com.prof18.feedflow.shared.presentation.ReaderModeViewModel
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.presentation.SettingsViewModel
import com.prof18.rssparser.RssParserBuilder
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration

@OptIn(ExperimentalKermitApi::class)
fun initKoinIos(
    htmlParser: HtmlParser,
    appEnvironment: AppEnvironment,
    languageCode: String?,
    regionCode: String?,
    dropboxDataSource: DropboxDataSource,
): KoinApplication = initKoin(
    appConfig = AppConfig(
        appEnvironment = appEnvironment,
        isLoggingEnabled = true,
        isDropboxSyncEnabled = true,
        isIcloudSyncEnabled = true,
    ),
    crashReportingLogWriter = CrashlyticsLogWriter(),
    modules = listOf(
        module {
            factory { htmlParser }
            single { dropboxDataSource }
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

@OptIn(ExperimentalSettingsImplementation::class)
internal actual fun getPlatformModule(appEnvironment: AppEnvironment): Module = module {
    single {
        RssParserBuilder(
            nsUrlSession = NSURLSession.sessionWithConfiguration(
                NSURLSessionConfiguration.defaultSessionConfiguration().apply {
                    HTTPAdditionalHeaders = mapOf(
                        "User-Agent" to "FeedFlow (RSS Reader; +https://feedflow.dev)",
                    )
                },
            ),
        ).build()
    }

    single<SqlDriver> {
        DatabaseFileMigration(
            databaseName = if (appEnvironment.isDebug()) {
                DatabaseHelper.APP_DATABASE_NAME_DEBUG
            } else {
                DatabaseHelper.APP_DATABASE_NAME_PROD
            },
        ).migrate()
        createDatabaseDriver(appEnvironment)
    }

    single<DispatcherProvider> {
        object : DispatcherProvider {
            override val main: CoroutineDispatcher = Dispatchers.Main
            override val default: CoroutineDispatcher = Dispatchers.Default
            override val io: CoroutineDispatcher = Dispatchers.IO
        }
    }

    factory {
        OpmlFeedHandler(
            dispatcherProvider = get(),
        )
    }

    single<Settings> {
        KeychainSettings(service = "FeedFlow")
    }

    factory<FeedSyncWorker> {
        FeedSyncIosWorker(
            dispatcherProvider = get(),
            feedSyncMessageQueue = get(),
            dropboxDataSource = get(),
            logger = getWith("FeedSyncIosWorker"),
            feedSyncer = get(),
            appEnvironment = appEnvironment,
            dropboxSettings = get(),
            settingsRepository = get(),
            accountsRepository = get(),
            iCloudSettings = get(),
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

    factory<CurrentOS> { CurrentOS.Ios }

    viewModel {
        ReaderModeViewModel(
            settingsRepository = get(),
            feedActionsRepository = get(),
        )
    }

    viewModel {
        DeeplinkFeedViewModel(
            widgetRepository = get(),
            feedActionsRepository = get(),
        )
    }
}

@Suppress("unused") // Called from Swift
object Deps : KoinComponent {
    fun getHomeViewModel() = getKoin().get<HomeViewModel>()
    fun getFeedSourceListViewModel() = getKoin().get<FeedSourceListViewModel>()
    fun getAddFeedViewModel() = getKoin().get<AddFeedViewModel>()
    fun getBrowserSettingsRepository() = getKoin().get<BrowserSettingsRepository>()
    fun getLogger(tag: String? = null) = getKoin().get<Logger> { parametersOf(tag) }
    fun getImportExportViewModel() = getKoin().get<ImportExportViewModel>()
    fun getSettingsViewModel() = getKoin().get<SettingsViewModel>()
    fun getFeedFlowStrings() = getKoin().get<FeedFlowStrings>()
    fun getSettingsRepository() = getKoin().get<SettingsRepository>()
    fun getSearchViewModel() = getKoin().get<SearchViewModel>()
    fun getAccountsViewModel() = getKoin().get<AccountsViewModel>()
    fun getDropboxDataSource() = getKoin().get<DropboxDataSource>()
    fun getDropboxSyncViewModel() = getKoin().get<DropboxSyncViewModel>()
    fun getFeedSyncRepository() = getKoin().get<FeedSyncRepository>()
    fun getICloudSyncViewModel() = getKoin().get<ICloudSyncViewModel>()
    fun getReaderModeViewModel() = getKoin().get<ReaderModeViewModel>()
    fun getEditFeedViewModel() = getKoin().get<EditFeedViewModel>()
    fun getFreshRssSyncViewModel() = getKoin().get<FreshRssSyncViewModel>()
    fun getFeedWidgetRepository() = getKoin().get<FeedWidgetRepository>()
    fun getDeeplinkFeedViewModel() = getKoin().get<DeeplinkFeedViewModel>()
}
