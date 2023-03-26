package com.prof18.feedflow

import com.prof18.feedflow.presentation.AddFeedViewModel
import com.prof18.feedflow.workmanager.WorkManagerHandler
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        WorkManagerHandler(
            context = get(),
        )
    }


}
