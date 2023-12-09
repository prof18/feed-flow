package com.prof18.feedflow.di

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.versionchecker.NewVersionChecker
import org.koin.core.Koin
import org.koin.dsl.module

object DI {
    lateinit var koin: Koin

    fun initKoin(appEnvironment: AppEnvironment) {
        koin = initKoinDesktop(
            appEnvironment = appEnvironment,
            modules = listOf(
                module {
                    factory {
                        NewVersionChecker(
                            dispatcherProvider = get(),
                            logger = getWith("NewVersionChecker"),
                        )
                    }
                },
            ),
        ).koin
    }
}
