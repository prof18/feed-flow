package com.prof18.feedflow.feedsync.dropbox.di

import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

actual val dropboxModule = module {
    factory {
        DropboxSettings(
            settings = get(),
            logger = get(parameters = { parametersOf("DropboxSettings") }),
        )
    }
}
