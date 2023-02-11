package com.prof18.feedflow

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel {
        ImportFeedViewModel(
            feedManagerRepository = get(),
            opmlImporter = get(),
        )
    }

    viewModel {
        HomeViewModel(
            feedRetrieverRepository = get(),
        )
    }
}