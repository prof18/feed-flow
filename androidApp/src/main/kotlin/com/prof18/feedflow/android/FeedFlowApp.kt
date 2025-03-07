package com.prof18.feedflow.android

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.coroutineScope
import com.prof18.feedflow.android.widget.FeedFlowWidget
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.di.getWith
import com.prof18.feedflow.shared.di.initKoin
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.ui.utils.coilImageLoader
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.dsl.module

class FeedFlowApp : Application() {

    private val feedSyncRepo by inject<FeedSyncRepository>()
    private val widgetRepository by inject<FeedWidgetRepository>()

    override fun onCreate() {
        super.onCreate()

        val isGooglePlayFlavor = when (BuildConfig.FLAVOR) {
            "googlePlay" -> true
            else -> false
        }
        val appEnvironment = if (BuildConfig.DEBUG) {
            AppEnvironment.Debug
        } else {
            AppEnvironment.Release
        }
        val appConfig = AppConfig(
            appEnvironment = appEnvironment,
            isLoggingEnabled = isGooglePlayFlavor,
            isDropboxSyncEnabled = isGooglePlayFlavor,
            isIcloudSyncEnabled = false,
        )

        if (isGooglePlayFlavor && appEnvironment.isRelease()) {
            CrashlyticsHelper.initCrashlytics()
        }

        initKoin(
            appConfig = appConfig,
            platformSetup = {
                workManagerFactory()
            },
            crashReportingLogWriter = CrashlyticsHelper.crashReportingLogWriter(),
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
                    single { appConfig }
                },
            ),
        )

        if (isGooglePlayFlavor && appEnvironment.isRelease()) {
            CrashlyticsHelper.setCollectionEnabled(
                enabled = getKoin().get<SettingsRepository>().getCrashReportingEnabled(),
            )
        }

        with(ProcessLifecycleOwner.get()) {
            lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStop(owner: LifecycleOwner) {
                        super.onStop(owner)
                        feedSyncRepo.enqueueBackup()
                        lifecycle.coroutineScope.launch {
                            GlanceAppWidgetManager(context = this@FeedFlowApp).getGlanceIds(FeedFlowWidget::class.java)
                                .forEach { id ->
                                    FeedFlowWidget(widgetRepository).update(this@FeedFlowApp, id)
                                }
                        }
                    }
                },
            )
        }
    }
}
