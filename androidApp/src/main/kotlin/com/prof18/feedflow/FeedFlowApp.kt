package com.prof18.feedflow

import android.app.Application
import android.content.Context
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.di.getWith
import com.prof18.feedflow.di.initKoin
import com.prof18.feedflow.utils.enableKmpCrashlytics
import org.koin.dsl.module

class FeedFlowApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val appEnvironment = if (BuildConfig.DEBUG) {
            AppEnvironment.Debug
        } else {
            AppEnvironment.Release
        }

        if (appEnvironment.isRelease()) {
            enableKmpCrashlytics()
        }

        initKoin(
            appEnvironment = appEnvironment,
            modules = listOf(
                module {
                    single<Context> { this@FeedFlowApp }
                    single {
                        BrowserManager(
                            context = this@FeedFlowApp,
                            browserSettingsRepository = get(),
                            logger = getWith("BrowserManager"),
                        )
                    }
                },
            ),
        )
    }
}
