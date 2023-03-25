package com.prof18.feedflow.di

import com.prof.rssparser.Parser
import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.domain.FeedRetrieverRepository
import com.prof18.feedflow.domain.feedmanager.FeedManagerRepository
import kotlinx.coroutines.Dispatchers
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module


fun initKoin(modules: List<Module>): KoinApplication {
    return startKoin {
        modules(modules + coreModule + platformModule)
    }
}

private val coreModule = module {
    single {
        DatabaseHelper(
            get(),
            Dispatchers.Default
        )
    }

    factory {
        FeedManagerRepository(
            databaseHelper = get(),
            opmlFeedParser = get(),
        )
    }

    single {
        FeedRetrieverRepository(
            parser = get(),
            databaseHelper = get(),
            dispatcherProvider = get(),
        )
    }
}

expect val platformModule: Module
