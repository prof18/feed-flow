package com.prof18.feedflow.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.platformLogWriter
import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.data.SettingsHelper
import com.prof18.feedflow.domain.browser.BrowserSettingsRepository
import com.prof18.feedflow.domain.browser.BrowserSettingsRepositoryImpl
import com.prof18.feedflow.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.logging.crashReportingLogWriter
import com.prof18.feedflow.presentation.AddFeedViewModel
import com.prof18.feedflow.presentation.BaseViewModel
import com.prof18.feedflow.presentation.FeedSourceListViewModel
import com.prof18.feedflow.presentation.HomeViewModel
import com.prof18.feedflow.presentation.ImportExportViewModel
import com.prof18.feedflow.utils.AppEnvironment
import com.prof18.rssparser.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.definition.Definition
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.dsl.module

fun initKoin(
    appEnvironment: AppEnvironment,
    modules: List<Module>,
): KoinApplication {
    return startKoin {
        modules(modules + coreModule + getLoggingModule(appEnvironment) + platformModule)
    }
}

private fun getLoggingModule(appEnvironment: AppEnvironment): Module =
    module {
        val loggers = mutableListOf(platformLogWriter())
        if (appEnvironment.isRelease()) {
            loggers.add(crashReportingLogWriter())
        }

        val baseLogger = Logger(
            config = StaticConfig(
                logWriterList = loggers,
            ),
            "FeedFlow",
        )
        factory { (tag: String?) ->
            if (tag != null) {
                baseLogger.withTag(tag)
            } else {
                baseLogger
            }
        }
    }

private val coreModule = module {
    single {
        DatabaseHelper(
            sqlDriver = get(),
            backgroundDispatcher = Dispatchers.IO,
            logger = getWith("DatabaseHelper"),
        )
    }

    single {
        RssParser()
    }

    factory {
        FeedManagerRepository(
            databaseHelper = get(),
            opmlFeedHandler = get(),
            rssParser = get(),
            logger = getWith("FeedManagerRepositoryImpl"),
        )
    }

    single {
        FeedRetrieverRepository(
            parser = get(),
            databaseHelper = get(),
            dispatcherProvider = get(),
            htmlParser = get(),
            logger = getWith("FeedRetrieverRepositoryImpl"),
            dateFormatter = get(),
        )
    }

    viewModel {
        HomeViewModel(
            feedRetrieverRepository = get(),
            feedManagerRepository = get(),
        )
    }

    viewModel {
        AddFeedViewModel(
            feedRetrieverRepository = get(),
        )
    }

    viewModel {
        FeedSourceListViewModel(
            feedManagerRepository = get(),
            feedRetrieverRepository = get(),
        )
    }

    factory {
        SettingsHelper(
            settings = get(),
        )
    }

    viewModel {
        ImportExportViewModel(
            feedManagerRepository = get(),
            feedRetrieverRepository = get(),
            logger = getWith("FeedImporterUseCase"),
        )
    }

    factory<BrowserSettingsRepository> {
        BrowserSettingsRepositoryImpl(
            settingsHelper = get(),
        )
    }
}

internal expect val platformModule: Module

internal expect inline fun <reified T : BaseViewModel> Module.viewModel(
    qualifier: Qualifier? = null,
    noinline definition: Definition<T>,
): KoinDefinition<T>

inline fun <reified T> Scope.getWith(vararg params: Any?): T {
    return get(parameters = { parametersOf(*params) })
}
