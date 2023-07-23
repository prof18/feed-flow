package com.prof18.feedflow.di

import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.crashlytics.CrashlyticsLogWriter
import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.data.SettingsHelper
import com.prof18.feedflow.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.domain.feed.manager.FeedManagerRepositoryImpl
import com.prof18.feedflow.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.domain.feed.retriever.FeedRetrieverRepositoryImpl
import com.prof18.feedflow.logging.feedFlowLogWriter
import com.prof18.feedflow.presentation.AddFeedViewModel
import com.prof18.feedflow.presentation.BaseViewModel
import com.prof18.feedflow.presentation.FeedSourceListViewModel
import com.prof18.feedflow.presentation.HomeViewModel
import com.prof18.feedflow.presentation.SettingsViewModel
import com.prof18.feedflow.utils.AppEnvironment
import kotlinx.coroutines.Dispatchers
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

@OptIn(ExperimentalKermitApi::class)
private fun getLoggingModule(appEnvironment: AppEnvironment): Module =
    module {
        val loggers = mutableListOf(feedFlowLogWriter(appEnvironment))
        if (appEnvironment.isRelease()) {
            loggers.add(CrashlyticsLogWriter())
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
            backgroundDispatcher = Dispatchers.Default,
            logger = getWith("DatabaseHelper"),
        )
    }

    factory<FeedManagerRepository> {
        FeedManagerRepositoryImpl(
            databaseHelper = get(),
            opmlFeedHandler = get(),
            settingsHelper = get(),
        )
    }

    single<FeedRetrieverRepository> {
        FeedRetrieverRepositoryImpl(
            parser = get(),
            databaseHelper = get(),
            dispatcherProvider = get(),
            htmlParser = get(),
            logger = getWith("FeedRetrieverRepositoryImpl"),
        )
    }

    viewModel {
        SettingsViewModel(
            feedManagerRepository = get(),
            feedRetrieverRepository = get(),
            logger = getWith("SettingsViewModel"),
        )
    }

    viewModel {
        HomeViewModel(
            feedRetrieverRepository = get(),
        )
    }

    viewModel {
        AddFeedViewModel(
            feedManagerRepository = get(),
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
}

internal expect val platformModule: Module

internal expect inline fun <reified T : BaseViewModel> Module.viewModel(
    qualifier: Qualifier? = null,
    noinline definition: Definition<T>,
): KoinDefinition<T>

internal inline fun <reified T> Scope.getWith(vararg params: Any?): T {
    return get(parameters = { parametersOf(*params) })
}
