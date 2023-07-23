package com.prof18.feedflow

import android.app.Application
import android.content.Context
import com.prof18.feedflow.di.initKoin
import com.prof18.feedflow.utils.AppEnvironment
import org.koin.dsl.module

class FeedFlowApp : Application() {

    override fun onCreate() {
        super.onCreate()

        initKoin(
            appEnvironment = if (BuildConfig.DEBUG) {
                AppEnvironment.Debug
            } else {
                AppEnvironment.Release
            },
            modules = listOf(
                module {
                    single<Context> { this@FeedFlowApp }
                    single {
                        BrowserManager(
                            context = this@FeedFlowApp,
                            feedManagerRepository = get(),
                        )
                    }
                },
            ),
        )
    }
}
