package com.prof18.feedflow

import com.prof18.feedflow.addtfeed.AddFeedViewModel
import com.prof18.feedflow.feedlist.FeedSourceListViewModel
import com.prof18.feedflow.home.HomeViewModel
import com.prof18.feedflow.settings.SettingsViewModel
import com.prof18.feedflow.workmanager.WorkManagerHandler
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        WorkManagerHandler(
            context = get(),
        )
    }

    viewModel {
        SettingsViewModel(
            feedManagerRepository = get(),
            opmlImporter = get(),
            feedRetrieverRepository = get(),
            workManagerHandler = get(),
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
