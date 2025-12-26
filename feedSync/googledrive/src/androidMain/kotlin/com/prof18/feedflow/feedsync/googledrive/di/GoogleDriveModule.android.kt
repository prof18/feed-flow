package com.prof18.feedflow.feedsync.googledrive.di

import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceAndroid
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

actual val googleDriveModule = module {
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
