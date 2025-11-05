package com.prof18.feedflow.feedsync.googledrive.di

import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSource
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceJvm
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import java.io.InputStream
import java.util.Properties

actual val googleDriveModule = module {
    single<GoogleDriveDataSource> {
        val properties = Properties()
        val propsFile = GoogleDriveDataSourceJvm::class.java.classLoader?.getResourceAsStream("props.properties")
            ?: InputStream.nullInputStream()
        properties.load(propsFile)

        val clientId = properties["google_drive_client_id"]?.toString()
            ?: "YOUR_DESKTOP_CLIENT_ID.apps.googleusercontent.com"
        val clientSecret = properties["google_drive_client_secret"]?.toString()
            ?: "YOUR_DESKTOP_CLIENT_SECRET"

        GoogleDriveDataSourceJvm(
            logger = get(parameters = { parametersOf("GoogleDriveDataSourceJvm") }),
            dispatcherProvider = get(),
            googleDriveSettings = get(),
            clientId = clientId,
            clientSecret = clientSecret,
        )
    }

    factory {
        GoogleDriveSettings(
            settings = get(),
        )
    }
}
