package com.prof18.feedflow.shared.di

import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DesktopOS
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.database.createDatabaseDriver
import com.prof18.feedflow.shared.domain.JvmHtmlParser
import com.prof18.feedflow.shared.domain.ReaderModeExtractor
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncJvmWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.domain.model.CurrentOS
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.logging.SentryLogWriter
import com.prof18.feedflow.shared.presentation.DropboxSyncViewModel
import com.prof18.feedflow.shared.presentation.MarkdownToHtmlConverter
import com.prof18.feedflow.shared.presentation.ReaderModeViewModel
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
    modules: List<Module>,
): KoinApplication = initKoin(
    appConfig = AppConfig(
        appEnvironment = appEnvironment,
        isLoggingEnabled = true,
        isDropboxSyncEnabled = true,
        isIcloudSyncEnabled = isICloudEnabled,
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

    factory<HtmlParser> {
        JvmHtmlParser(
            logger = getWith("JvmHtmlParser"),
        )
    }

    single<Settings> {
        val preferences = Preferences.userRoot()
        PreferencesSettings(preferences)
    }

    factoryOf(::ReaderModeExtractor)

    viewModel {
        DropboxSyncViewModel(
            logger = getWith("DropboxSyncViewModel"),
            dropboxSettings = get(),
            dropboxDataSource = get(),
            feedSyncRepository = get(),
            dateFormatter = get(),
            feedRetrieverRepository = get(),
            accountsRepository = get(),
        )
    }

    single<FeedSyncWorker> {
        FeedSyncJvmWorker(
            dropboxDataSource = get(),
            appEnvironment = appEnvironment,
            logger = getWith("FeedSyncJvmWorker"),
            feedSyncer = get(),
            feedSyncMessageQueue = get(),
            settingsRepository = get(),
            dispatcherProvider = get(),
            dropboxSettings = get(),
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

    factoryOf(::ReaderModeViewModel)
}
