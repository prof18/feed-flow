package com.prof18.feedflow.desktop.di

import coil3.PlatformContext
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.desktop.BrowserManager
import com.prof18.feedflow.desktop.versionchecker.NewVersionChecker
import com.prof18.feedflow.shared.di.getWith
import com.prof18.feedflow.shared.di.initKoinDesktop
import com.prof18.feedflow.shared.ui.utils.coilImageLoader
import org.koin.core.Koin
import org.koin.dsl.module

object DI {
    lateinit var koin: Koin

    fun initKoin(appEnvironment: AppEnvironment, isICloudEnabled: Boolean) {
        koin = initKoinDesktop(
            appEnvironment = appEnvironment,
            isICloudEnabled = isICloudEnabled,
            modules = listOf(
                module {
                    factory {
                        NewVersionChecker(
                            dispatcherProvider = get(),
                            logger = getWith("NewVersionChecker"),
                        )
                    }
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
                },
            ),
        ).koin
    }
}
