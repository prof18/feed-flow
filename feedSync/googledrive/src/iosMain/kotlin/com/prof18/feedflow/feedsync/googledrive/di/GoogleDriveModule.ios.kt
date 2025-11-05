package com.prof18.feedflow.feedsync.googledrive.di

import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import org.koin.dsl.module

actual val googleDriveModule = module {
    factory {
        GoogleDriveSettings(
            settings = get(),
        )
    }
}
