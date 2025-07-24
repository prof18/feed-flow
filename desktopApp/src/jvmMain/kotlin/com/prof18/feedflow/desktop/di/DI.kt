package com.prof18.feedflow.desktop.di

import coil3.PlatformContext
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.desktop.BrowserManager
import com.prof18.feedflow.desktop.telemetry.TelemetryDeckClient
import com.prof18.feedflow.shared.di.getWith
import com.prof18.feedflow.shared.di.initKoinDesktop
import com.prof18.feedflow.shared.domain.notification.Notifier
import com.prof18.feedflow.shared.ui.utils.coilImageLoader
import io.ktor.client.HttpClient
import org.koin.core.Koin
import org.koin.dsl.module

object DI {
    lateinit var koin: Koin

    fun initKoin(appEnvironment: AppEnvironment, isICloudEnabled: Boolean, version: String) {
        koin = initKoinDesktop(
            appEnvironment = appEnvironment,
            isICloudEnabled = isICloudEnabled,
            version = version,
            modules = listOf(
                module {
                    single {
                        coilImageLoader(
                            context = PlatformContext.INSTANCE,
                            debug = appEnvironment.isDebug(),
                        )
                    }

                    factory {
                        BrowserManager(
                            settingsRepository = get(),
                        )
                    }

                    single<Notifier> {
                        Notifier {}
                    }

                    single<TelemetryDeckClient> {
                        TelemetryDeckClient(
                            httpClient = HttpClient(),
                            appEnvironment = appEnvironment,
                            logger = getWith("TelemetryDeckClient"),
                        )
                    }
                },
            ),
        ).koin
    }
}
