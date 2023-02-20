package com.prof18.feedflow

import com.prof18.feedflow.addtfeed.AddFeedViewModel
import com.prof18.feedflow.feedlist.FeedSourceListViewModel
import com.prof18.feedflow.home.HomeViewModel
import com.prof18.feedflow.settings.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
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
