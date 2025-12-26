package com.prof18.feedflow.feedsync.googledrive.di

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSource
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceJvm
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import java.io.InputStream
import java.util.Properties

actual val googleDriveModule = module {
    single {
        GoogleDriveDataSourceJvm(
            logger = get(parameters = { parametersOf("GoogleDriveDataSourceJvm") }),
            dispatcherProvider = get(),
            googleDriveSettings = get(),
            // TODO: pass the real one
            appEnvironment = AppEnvironment.Debug
        )
    }

    factory {
        GoogleDriveSettings(
            settings = get(),
        )
    }
}
