package com.prof18.feedflow.feedsync.googledrive.di

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import org.koin.core.module.Module
import org.koin.dsl.module

@Suppress("UNUSED_PARAMETER")
actual fun googleDriveModule(appEnvironment: AppEnvironment): Module = module {
    factory {
        GoogleDriveSettings(
            settings = get(),
        )
    }
}
