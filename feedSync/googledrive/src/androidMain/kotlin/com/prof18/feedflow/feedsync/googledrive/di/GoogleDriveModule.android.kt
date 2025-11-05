package com.prof18.feedflow.feedsync.googledrive.di

import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSource
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceJvm
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

actual val googleDriveModule = module {
    single<GoogleDriveDataSource> {
        GoogleDriveDataSourceJvm(
            logger = get(parameters = { parametersOf("GoogleDriveDataSourceJvm") }),
            dispatcherProvider = get(),
            googleDriveSettings = get(),
            clientId = "",
            clientSecret = "",
        )
    }

    factory {
        GoogleDriveSettings(
            settings = get(),
        )
    }
}
