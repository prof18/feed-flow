package com.prof18.feedflow.android

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.prof18.feedflow.android.readermode.ReaderModeViewModel
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.shared.di.getWith
import com.prof18.feedflow.shared.di.initKoin
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.ui.utils.coilImageLoader
import com.prof18.feedflow.shared.utils.enableKmpCrashlytics
import org.koin.android.ext.android.inject
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class FeedFlowApp : Application() {

    private val feedSyncRepo by inject<FeedSyncRepository>()

    override fun onCreate() {
        super.onCreate()

        val appEnvironment = if (BuildConfig.DEBUG) {
            AppEnvironment.Debug
        } else {
            AppEnvironment.Release
        }

        if (appEnvironment.isRelease()) {
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
            enableKmpCrashlytics()
        }

        initKoin(
            appEnvironment = appEnvironment,
            platformSetup = {
                workManagerFactory()
            },
            modules = listOf(
                module {
                    single<Context> { this@FeedFlowApp }
                    single {
                        BrowserManager(
                            context = this@FeedFlowApp,
                            browserSettingsRepository = get(),
                            logger = getWith("BrowserManager"),
                            settingsRepository = get(),
                        )
                    }
                    single {
                        coilImageLoader(
                            context = this@FeedFlowApp,
                            debug = appEnvironment.isDebug(),
                        )
                    }
                    viewModel {
                        ReaderModeViewModel(
                            readerModeExtractor = get(),
                            settingsRepository = get(),
                        )
                    }
                },
            ),
        )

        with(ProcessLifecycleOwner.get()) {
            lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStop(owner: LifecycleOwner) {
                        super.onStop(owner)
                        feedSyncRepo.enqueueBackup()
                    }
                },
            )
        }
    }
}
