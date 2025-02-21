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
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.DateFormatterImpl
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.browser.BrowserSettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedFontSizeRepository
import com.prof18.feedflow.shared.domain.feed.FeedImportExportRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourceLogoRetriever
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.FeedUrlRetriever
import com.prof18.feedflow.shared.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.shared.domain.feed.retriever.FeedStateRepository
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncer
import com.prof18.feedflow.shared.domain.mappers.RssChannelMapper
import com.prof18.feedflow.shared.presentation.AccountsViewModel
import com.prof18.feedflow.shared.presentation.AddFeedViewModel
import com.prof18.feedflow.shared.presentation.EditFeedViewModel
import com.prof18.feedflow.shared.presentation.FeedSourceListViewModel
import com.prof18.feedflow.shared.presentation.FreshRssSyncViewModel
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.ICloudSyncViewModel
import com.prof18.feedflow.shared.presentation.ImportExportViewModel
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
            backgroundDispatcher = Dispatchers.IO,
            logger = getWith("DatabaseHelper"),
        )
    }

    single {
        FeedRetrieverRepository(
            parser = get(),
            databaseHelper = get(),
            dispatcherProvider = get(),
            logger = getWith("FeedRetrieverRepositoryImpl"),
            dateFormatter = get(),
            settingsRepository = get(),
            feedSourceLogoRetriever = get(),
            rssChannelMapper = get(),
            feedUrlRetriever = get(),
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
            feedRetrieverRepository = get(),
            feedSourcesRepository = get(),
            settingsRepository = get(),
            feedSyncRepository = get(),
            feedFontSizeRepository = get(),
            feedCategoryRepository = get(),
            feedStateRepository = get(),
        )
    }

    viewModel {
        AddFeedViewModel(
            feedRetrieverRepository = get(),
            categoryUseCase = get(),
        )
    }

    viewModel {
        FeedSourceListViewModel(
            feedSourcesRepository = get(),
            feedRetrieverRepository = get(),
            feedStateRepository = get(),
        )
    }

    single {
        SettingsRepository(
            settings = get(),
        )
    }

    viewModel {
        ImportExportViewModel(
            feedImportExportRepository = get(),
            feedRetrieverRepository = get(),
            logger = getWith("ImportExportViewModel"),
            dateFormatter = get(),
        )
    }

    factory {
        BrowserSettingsRepository(
            settingsRepository = get(),
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
        SearchViewModel(
            feedRetrieverRepository = get(),
            dateFormatter = get(),
            settingsRepository = get(),
            feedFontSizeRepository = get(),
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
            feedRetrieverRepository = get(),
            accountsRepository = get(),
        )
    }

    viewModel {
        ICloudSyncViewModel(
            iCloudSettings = get(),
            dateFormatter = get(),
            accountsRepository = get(),
            feedSyncRepository = get(),
            feedRetrieverRepository = get(),
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

    factoryOf(::FeedSourcesRepository)

    single {
        FeedStateRepository(
            databaseHelper = get(),
            settingsRepository = get(),
            dateFormatter = get(),
            logger = getWith("FeedStateRepository"),
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
