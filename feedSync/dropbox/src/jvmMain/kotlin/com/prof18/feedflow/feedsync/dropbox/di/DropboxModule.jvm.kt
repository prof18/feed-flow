package com.prof18.feedflow.feedsync.dropbox.di

import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSourceJvm
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

actual val dropboxModule = module {
    single<DropboxDataSource> {
        DropboxDataSourceJvm(
            logger = get(parameters = { parametersOf("DropboxDataSourceJvm") }),
            dispatcherProvider = get(),
        )
    }

    factory {
        DropboxSettings(
            settings = get(),
        )
    }
}
