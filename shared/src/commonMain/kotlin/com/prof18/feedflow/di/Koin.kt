package com.prof18.feedflow.di

import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.domain.FeedRetrieverRepository
import com.prof18.feedflow.domain.feedmanager.FeedManagerRepository
import com.prof18.feedflow.presentation.AddFeedViewModel
import com.prof18.feedflow.presentation.BaseViewModel
import com.prof18.feedflow.presentation.FeedSourceListViewModel
import com.prof18.feedflow.presentation.HomeViewModel
import com.prof18.feedflow.presentation.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.definition.Definition
import org.koin.core.instance.InstanceFactory
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
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

    viewModel {
        SettingsViewModel(
            feedManagerRepository = get(),
            opmlImporter = get(),
            feedRetrieverRepository = get(),
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
        )
    }
}

expect val platformModule: Module

expect inline fun <reified T: BaseViewModel> Module.viewModel(
    qualifier: Qualifier? = null,
    noinline definition: Definition<T>
): Pair<Module, InstanceFactory<T>>