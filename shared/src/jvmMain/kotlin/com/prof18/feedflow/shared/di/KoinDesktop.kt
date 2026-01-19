package com.prof18.feedflow.shared.di

import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.platformLogWriter
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DesktopOS
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.database.createDatabaseDriver
import com.prof18.feedflow.shared.data.DesktopWindowSettingsRepository
import com.prof18.feedflow.shared.domain.DatabaseCloser
import com.prof18.feedflow.shared.domain.JvmHtmlParser
import com.prof18.feedflow.shared.domain.contentprefetch.ContentPrefetchRepository
import com.prof18.feedflow.shared.domain.contentprefetch.ContentPrefetchRepositoryIosDesktop
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncJvmWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedbinHistorySyncScheduler
import com.prof18.feedflow.shared.domain.feedsync.FeedbinHistorySyncSchedulerIosDesktop
import com.prof18.feedflow.shared.domain.model.CurrentOS
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.domain.parser.DesktopFeedItemParserWorker
import com.prof18.feedflow.shared.domain.parser.FeedItemContentFileHandlerDesktop
import com.prof18.feedflow.shared.logging.SentryLogWriter
import com.prof18.feedflow.shared.presentation.DropboxSyncViewModel
import com.prof18.feedflow.shared.presentation.GoogleDriveSyncViewModel
import com.prof18.feedflow.shared.presentation.ICloudSyncViewModel
import com.prof18.feedflow.shared.presentation.MarkdownToHtmlConverter
import com.prof18.feedflow.shared.utils.UserAgentInterceptor
import com.prof18.rssparser.RssParserBuilder
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import java.util.prefs.Preferences

fun initKoinDesktop(
    appEnvironment: AppEnvironment,
    isICloudEnabled: Boolean,
    isDropboxEnabled: Boolean,
    isGoogleDriveEnabled: Boolean = true,
    version: String,
    modules: List<Module>,
): KoinApplication = initKoin(
    appConfig = AppConfig(
        appEnvironment = appEnvironment,
        isLoggingEnabled = true,
        isDropboxSyncEnabled = isDropboxEnabled,
        isGoogleDriveSyncEnabled = isGoogleDriveEnabled,
        isIcloudSyncEnabled = isICloudEnabled,
        appVersion = version,
        platformName = "${System.getProperty("os.name")}",
        platformVersion = "${System.getProperty("os.version")}",
    ),
    crashReportingLogWriter = SentryLogWriter(),
    modules = modules + getDatabaseModule(appEnvironment),
)

private fun getDatabaseModule(appEnvironment: AppEnvironment): Module =
    module {
        single<SqlDriver> {
            createDatabaseDriver(
                appEnvironment = appEnvironment,
                logger = getWith("initDatabase"),
            )
        }
    }

internal actual fun getPlatformModule(appEnvironment: AppEnvironment): Module = module {
    single {
        RssParserBuilder(
            callFactory = OkHttpClient
                .Builder()
                .addInterceptor(UserAgentInterceptor())
                .build(),
        ).build()
    }

    factory {
        OpmlFeedHandler(
            dispatcherProvider = get(),
        )
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

    factory<HtmlParser> {
        JvmHtmlParser(
            logger = getWith("JvmHtmlParser"),
        )
    }

    single<Settings> {
        val preferences = Preferences.userRoot()
        val nodeName = if (appEnvironment.isRelease()) {
            "feedflow"
        } else {
            "feedflow-dev"
        }
        PreferencesSettings(preferences.node(nodeName))
    }

    single {
        DesktopWindowSettingsRepository(
            settings = get(),
        )
    }

    single<FeedItemContentFileHandler> {
        FeedItemContentFileHandlerDesktop(
            dispatcherProvider = get(),
            logger = getWith("FeedItemContentFileHandler"),
            appEnvironment = appEnvironment,
        )
    }

    single<FeedItemParserWorker> {
        DesktopFeedItemParserWorker(
            htmlRetriever = get(),
            logger = getWith("FeedItemParserWorker"),
            dispatcherProvider = get(),
            feedItemContentFileHandler = get(),
            markdownToHtmlConverter = get(),
            settingsRepository = get(),
        )
    }

    viewModel {
        DropboxSyncViewModel(
            logger = getWith("DropboxSyncViewModel"),
            dropboxSettings = get(),
            dropboxDataSource = get(),
            feedSyncRepository = get(),
            dateFormatter = get(),
            accountsRepository = get(),
            feedFetcherRepository = get(),
        )
    }

    viewModel {
        GoogleDriveSyncViewModel(
            googleDriveSettings = get(),
            googleDriveDataSource = get(),
            feedSyncRepository = get(),
            dateFormatter = get(),
            accountsRepository = get(),
            feedFetcherRepository = get(),
        )
    }

    single<FeedSyncWorker> {
        FeedSyncJvmWorker(
            dropboxDataSource = get(),
            googleDriveDataSource = get(),
            appEnvironment = appEnvironment,
            logger = getWith("FeedSyncJvmWorker"),
            feedSyncer = get(),
            feedSyncMessageQueue = get(),
            settingsRepository = get(),
            dispatcherProvider = get(),
            dropboxSettings = get(),
            googleDriveSettings = get(),
            accountsRepository = get(),
            iCloudSettings = get(),
        )
    }

    factory<CurrentOS> {
        when (getDesktopOS()) {
            DesktopOS.MAC -> CurrentOS.Desktop.Mac
            DesktopOS.WINDOWS -> CurrentOS.Desktop.Windows
            DesktopOS.LINUX -> CurrentOS.Desktop.Linux
        }
    }

    single {
        MarkdownToHtmlConverter(
            converter = FlexmarkHtmlConverter.builder().build(),
            dispatcherProvider = get(),
        )
    }

    factoryOf(::DatabaseCloser)

    viewModel {
        ICloudSyncViewModel(
            iCloudSettings = get(),
            dateFormatter = get(),
            accountsRepository = get(),
            feedSyncRepository = get(),
            feedFetcherRepository = get(),
            feedSyncMessageQueue = get(),
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

internal actual fun platformLogWriters(): List<LogWriter> =
    listOf(platformLogWriter())
