package com.prof18.feedflow.shared.di

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.platformLogWriter
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.database.di.getFeedSyncModule
import com.prof18.feedflow.feedsync.dropbox.di.dropboxModule
import com.prof18.feedflow.feedsync.greader.di.getGReaderModule
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.shared.data.ReviewRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.DateFormatterImpl
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feed.FeedFontSizeRepository
import com.prof18.feedflow.shared.domain.feed.FeedImportExportRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourceLogoRetriever
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feed.FeedUrlRetriever
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncer
import com.prof18.feedflow.shared.domain.mappers.RssChannelMapper
import com.prof18.feedflow.shared.presentation.AccountsViewModel
import com.prof18.feedflow.shared.presentation.AddFeedViewModel
import com.prof18.feedflow.shared.presentation.DeeplinkFeedViewModel
import com.prof18.feedflow.shared.presentation.EditFeedViewModel
import com.prof18.feedflow.shared.presentation.FeedSourceListViewModel
import com.prof18.feedflow.shared.presentation.FreshRssSyncViewModel
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.ICloudSyncViewModel
import com.prof18.feedflow.shared.presentation.ImportExportViewModel
import com.prof18.feedflow.shared.presentation.NotificationsViewModel
import com.prof18.feedflow.shared.presentation.ReviewViewModel
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.presentation.SettingsViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.definition.Definition
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.dsl.module

fun initKoin(
    appConfig: AppConfig,
    crashReportingLogWriter: LogWriter,
    modules: List<Module>,
    platformSetup: KoinApplication.() -> Unit = {},
): KoinApplication {
    return startKoin {
        modules(
            modules +
                getCoreModule(appConfig) +
                dropboxModule +
                getGReaderModule(appConfig.appEnvironment) +
                getLoggingModule(appConfig, crashReportingLogWriter) +
                getPlatformModule(appConfig.appEnvironment) +
                getFeedSyncModule(appConfig.appEnvironment),
        )
        platformSetup()
    }
}

private fun getLoggingModule(
    appConfig: AppConfig,
    crashReportingLogWriter: LogWriter,
): Module =
    module {
        val loggers = mutableListOf(platformLogWriter())
        if (appConfig.appEnvironment.isRelease() && appConfig.isLoggingEnabled) {
            loggers.add(crashReportingLogWriter)
        }

        val baseLogger = Logger(
            config = StaticConfig(
                logWriterList = loggers,
            ),
            tag = "FeedFlow",
        )
        factory { (tag: String?) ->
            if (tag != null) {
                baseLogger.withTag(tag)
            } else {
                baseLogger
            }
        }
    }

private fun getCoreModule(appConfig: AppConfig) = module {
    single {
        DatabaseHelper(
            sqlDriver = get(),
            backgroundDispatcher = Dispatchers.IO.limitedParallelism(1),
            logger = getWith("DatabaseHelper"),
        )
    }

    single {
        FeedActionsRepository(
            databaseHelper = get(),
            feedSyncRepository = get(),
            gReaderRepository = get(),
            accountsRepository = get(),
            feedStateRepository = get(),
        )
    }

    single<DateFormatter> {
        DateFormatterImpl(
            logger = getWith("DateFormatter"),
        )
    }

    viewModel {
        HomeViewModel(
            feedActionsRepository = get(),
            feedSourcesRepository = get(),
            settingsRepository = get(),
            feedSyncRepository = get(),
            feedFontSizeRepository = get(),
            feedCategoryRepository = get(),
            feedStateRepository = get(),
            feedFetcherRepository = get(),
        )
    }

    viewModel {
        AddFeedViewModel(
            feedSourcesRepository = get(),
            categoryRepository = get(),
            databaseHelper = get(),
        )
    }

    viewModel {
        FeedSourceListViewModel(
            feedSourcesRepository = get(),
            feedStateRepository = get(),
            feedFetcherRepository = get(),
        )
    }

    single {
        SettingsRepository(
            settings = get(),
        )
    }

    factory {
        ReviewRepository(
            settingsRepository = get(),
            databaseHelper = get(),
            appConfig = appConfig,
        )
    }

    viewModel {
        ImportExportViewModel(
            feedImportExportRepository = get(),
            feedFetcherRepository = get(),
            logger = getWith("ImportExportViewModel"),
            dateFormatter = get(),
        )
    }

    factory {
        RssChannelMapper(
            dateFormatter = get(),
            htmlParser = get(),
            logger = getWith("RssChannelMapper"),
        )
    }

    factory {
        FeedSourceLogoRetriever(
            htmlRetriever = get(),
            htmlParser = get(),
        )
    }

    viewModel {
        SettingsViewModel(
            settingsRepository = get(),
            fontSizeRepository = get(),
            feedStateRepository = get(),
        )
    }

    viewModel {
        ReviewViewModel(
            reviewRepository = get(),
        )
    }

    viewModel {
        SearchViewModel(
            feedActionsRepository = get(),
            dateFormatter = get(),
            settingsRepository = get(),
            feedFontSizeRepository = get(),
            feedStateRepository = get(),
        )
    }

    factory {
        FeedUrlRetriever(
            htmlParser = get(),
            htmlRetriever = get(),
        )
    }

    single {
        HtmlRetriever(
            logger = getWith("HtmlRetriever"),
            client = HttpClient(),
        )
    }

    single {
        FeedSyncRepository(
            syncedDatabaseHelper = get(),
            feedSyncWorker = get(),
            feedSyncAccountRepository = get(),
            feedSyncMessageQueue = get(),
            dropboxSettings = get(),
            logger = getWith("FeedSyncRepository"),
            settingsRepository = get(),
        )
    }

    factory {
        FeedSyncer(
            syncedDatabaseHelper = get(),
            appDatabaseHelper = get(),
            logger = getWith("FeedSyncer"),
        )
    }

    viewModel {
        AccountsViewModel(
            accountsRepository = get(),
        )
    }

    singleOf(::FeedSyncMessageQueue)

    single {
        AccountsRepository(
            currentOS = get(),
            dropboxSettings = get(),
            icloudSettings = get(),
            appConfig = appConfig,
            gReaderRepository = get(),
        )
    }

    factoryOf(::FeedCategoryRepository)

    factory {
        ICloudSettings(
            settings = get(),
        )
    }

    viewModel {
        EditFeedViewModel(
            categoryUseCase = get(),
            feedSourcesRepository = get(),
            accountsRepository = get(),
            databaseHelper = get(),
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
        )
    }

    singleOf(::FeedFontSizeRepository)

    viewModel {
        FreshRssSyncViewModel(
            gReaderRepository = get(),
            accountsRepository = get(),
            dateFormatter = get(),
            feedStateRepository = get(),
        )
    }

    factory {
        FeedImportExportRepository(
            dispatcherProvider = get(),
            feedSyncRepository = get(),
            accountsRepository = get(),
            gReaderRepository = get(),
            logger = getWith("FeedImportExportRepository"),
            logoRetriever = get(),
            rssParser = get(),
            databaseHelper = get(),
            opmlFeedHandler = get(),
        )
    }

    factory {
        FeedSourcesRepository(
            databaseHelper = get(),
            accountsRepository = get(),
            feedSyncRepository = get(),
            gReaderRepository = get(),
            dispatcherProvider = get(),
            logger = getWith("FeedSourcesRepository"),
            feedStateRepository = get(),
            feedUrlRetriever = get(),
            feedSourceLogoRetriever = get(),
            parser = get(),
            dateFormatter = get(),
            rssChannelMapper = get(),
        )
    }

    single {
        FeedStateRepository(
            databaseHelper = get(),
            settingsRepository = get(),
            dateFormatter = get(),
            logger = getWith("FeedStateRepository"),
        )
    }

    factory {
        FeedFetcherRepository(
            dispatcherProvider = get(),
            feedStateRepository = get(),
            gReaderRepository = get(),
            databaseHelper = get(),
            feedSyncRepository = get(),
            settingsRepository = get(),
            logger = getWith("FeedFetcherRepository"),
            rssParser = get(),
            rssChannelMapper = get(),
            dateFormatter = get(),
        )
    }

    factory {
        FeedWidgetRepository(
            databaseHelper = get(),
            dateFormatter = get(),
            settingsRepository = get(),
        )
    }

    viewModel {
        DeeplinkFeedViewModel(
            widgetRepository = get(),
            feedActionsRepository = get(),
        )
    }

    viewModel {
        NotificationsViewModel(
            databaseHelper = get(),
            settingsRepository = get(),
        )
    }
}

internal expect fun getPlatformModule(appEnvironment: AppEnvironment): Module

inline fun <reified T : ViewModel> Module.viewModel(
    qualifier: Qualifier? = null,
    noinline definition: Definition<T>,
): KoinDefinition<T> {
    return factory(qualifier, definition)
}

inline fun <reified T> Scope.getWith(vararg params: Any?): T {
    return get(parameters = { parametersOf(*params) })
}
