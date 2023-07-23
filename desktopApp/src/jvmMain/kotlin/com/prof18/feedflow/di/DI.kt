package com.prof18.feedflow.di

import com.prof18.feedflow.utils.AppEnvironment
import org.koin.core.Koin

object DI {
    lateinit var koin: Koin

    fun initKoin(appEnvironment: AppEnvironment) {
        koin = initKoinDesktop(appEnvironment).koin
    }
}
