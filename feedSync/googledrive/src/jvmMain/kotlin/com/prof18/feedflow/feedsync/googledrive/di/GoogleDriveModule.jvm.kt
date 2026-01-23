package com.prof18.feedflow.feedsync.googledrive.di

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceJvm
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceJvmImpl
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

actual fun googleDriveModule(appEnvironment: AppEnvironment): Module = module {
    single<GoogleDriveDataSourceJvm> {
        GoogleDriveDataSourceJvmImpl(
            logger = get(parameters = { parametersOf("GoogleDriveDataSourceJvm") }),
            dispatcherProvider = get(),
            googleDriveSettings = get(),
            appEnvironment = appEnvironment,
        )
    }

    factory {
        GoogleDriveSettings(
            settings = get(),
        )
    }
}
