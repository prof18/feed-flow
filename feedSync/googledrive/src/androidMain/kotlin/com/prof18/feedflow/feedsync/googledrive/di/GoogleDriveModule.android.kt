package com.prof18.feedflow.feedsync.googledrive.di

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceAndroid
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

@Suppress("UNUSED_PARAMETER")
actual fun googleDriveModule(appEnvironment: AppEnvironment): Module = module {
    single {
        GoogleDriveDataSourceAndroid(
            context = get(),
            googleDriveSettings = get(),
            logger = get(parameters = { parametersOf("GoogleDriveDataSourceAndroid") }),
            dispatcherProvider = get(),
        )
    }

    factory {
        GoogleDriveSettings(
            settings = get(),
        )
    }
}
