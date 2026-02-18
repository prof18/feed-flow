package com.prof18.feedflow.android

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.coroutineScope
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.prof18.feedflow.android.notifications.AndroidNotifier
import com.prof18.feedflow.android.settings.widget.WidgetSettingsViewModel
import com.prof18.feedflow.android.widget.FeedFlowWidget
import com.prof18.feedflow.android.widget.WidgetConfigurationViewModel
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveAndroidDataSourceImpl
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceAndroid
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import com.prof18.feedflow.shared.di.getWith
import com.prof18.feedflow.shared.di.initKoin
import com.prof18.feedflow.shared.domain.AppForegroundState
import com.prof18.feedflow.shared.domain.FeedDownloadWorkerEnqueuer
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.notification.Notifier
import com.prof18.feedflow.shared.presentation.WidgetUpdater
import com.prof18.feedflow.shared.ui.utils.coilImageLoader
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

class FeedFlowApp : Application(), SingletonImageLoader.Factory {

    private val feedSyncRepo by inject<FeedSyncRepository>()
    private val widgetRepository by inject<FeedWidgetRepository>()
    private val widgetSettingsRepository by inject<WidgetSettingsRepository>()
    private val feedDownloadWorkerEnqueuer by inject<FeedDownloadWorkerEnqueuer>()
    private val appForegroundState by inject<AppForegroundState>()
    private val browserManager by inject<BrowserManager>()

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
            isGoogleDriveSyncEnabled = isGooglePlayFlavor,
            isIcloudSyncEnabled = false,
            appVersion = BuildConfig.VERSION_NAME,
            platformName = "Android",
            platformVersion = "${android.os.Build.VERSION.RELEASE} - API ${android.os.Build.VERSION.SDK_INT}",
        )

        if (isGooglePlayFlavor && appEnvironment.isRelease()) {
            CrashlyticsHelper.initCrashlytics()
            TelemetryHelper.initTelemetry(this)
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
                            logger = getWith("BrowserManager"),
                            settingsRepository = get(),
                        )
                    }
                    single<GoogleDriveDataSourceAndroid> {
                        GoogleDriveAndroidDataSourceImpl(
                            context = this@FeedFlowApp,
                            googleDriveSettings = get(),
                            logger = get(parameters = { parametersOf("GoogleDriveDataSourceAndroid") }),
                            dispatcherProvider = get(),
                        )
                    }
                    single { appConfig }
                    factory<WidgetUpdater> {
                        WidgetUpdater {
                            GlanceAppWidgetManager(
                                context = this@FeedFlowApp,
                            ).getGlanceIds(FeedFlowWidget::class.java)
                                .forEach { id ->
                                    FeedFlowWidget(
                                        widgetRepository,
                                        widgetSettingsRepository,
                                        get<BrowserManager>(),
                                    ).update(this@FeedFlowApp, id)
                                }
                        }
                    }
                    viewModel {
                        WidgetConfigurationViewModel(
                            settingsRepository = get(),
                            widgetSettingsRepository = get(),
                            feedDownloadWorkerEnqueuer = get(),
                        )
                    }
                    viewModel {
                        WidgetSettingsViewModel(
                            settingsRepository = get(),
                            widgetSettingsRepository = get(),
                            feedDownloadWorkerEnqueuer = get(),
                            widgetUpdater = get(),
                        )
                    }
                    single<Notifier> {
                        AndroidNotifier(
                            context = androidContext(),
                            settingsRepository = get(),
                        )
                    }
                },
            ),
        )

        feedDownloadWorkerEnqueuer.enqueueWork()

        with(ProcessLifecycleOwner.get()) {
            lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        super.onStart(owner)
                        appForegroundState.onAppForegrounded()
                    }

                    override fun onStop(owner: LifecycleOwner) {
                        super.onStop(owner)
                        appForegroundState.onAppBackgrounded()
                        feedSyncRepo.enqueueBackup()
                        lifecycle.coroutineScope.launch {
                            GlanceAppWidgetManager(
                                context = this@FeedFlowApp,
                            ).getGlanceIds(FeedFlowWidget::class.java)
                                .forEach { id ->
                                    FeedFlowWidget(
                                        widgetRepository,
                                        widgetSettingsRepository,
                                        browserManager,
                                    ).update(this@FeedFlowApp, id)
                                }
                        }
                    }
                },
            )
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val appEnvironment = if (BuildConfig.DEBUG) {
            AppEnvironment.Debug
        } else {
            AppEnvironment.Release
        }
        return coilImageLoader(
            context = this@FeedFlowApp,
            debug = appEnvironment.isDebug(),
        )
    }
}
