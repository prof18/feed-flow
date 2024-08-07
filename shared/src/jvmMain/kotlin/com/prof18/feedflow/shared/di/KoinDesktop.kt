package com.prof18.feedflow.shared.di

import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.createDatabaseDriver
import com.prof18.feedflow.shared.domain.HtmlParser
import com.prof18.feedflow.shared.domain.JvmHtmlParser
import com.prof18.feedflow.shared.domain.ReaderModeExtractor
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncJvmWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.domain.model.CurrentOS
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.presentation.DropboxSyncViewModel
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.prefs.Preferences

fun initKoinDesktop(
    appEnvironment: AppEnvironment,
    modules: List<Module>,
): KoinApplication = initKoin(
    appEnvironment = appEnvironment,
    modules = modules + getDatabaseModule(appEnvironment),
)

private fun getDatabaseModule(appEnvironment: AppEnvironment): Module =
    module {
        single<SqlDriver> {
            createDatabaseDriver(
                appEnvironment = appEnvironment,
                logger = getWith("initDatabase"),
            )
        }
    }

internal actual fun getPlatformModule(appEnvironment: AppEnvironment): Module = module {
    factory {
        OpmlFeedHandler(
            dispatcherProvider = get(),
        )
    }

    single<DispatcherProvider> {
        object : DispatcherProvider {
            override val main: CoroutineDispatcher = Dispatchers.Main
            override val default: CoroutineDispatcher = Dispatchers.Default
            override val io: CoroutineDispatcher = Dispatchers.IO
        }
    }

    factory<HtmlParser> {
        JvmHtmlParser(
            logger = getWith("JvmHtmlParser"),
        )
    }

    single<Settings> {
        val preferences = Preferences.userRoot()
        PreferencesSettings(preferences)
    }

    factory {
        ReaderModeExtractor(
            dispatcherProvider = get(),
            htmlRetriever = get(),
        )
    }

    viewModel {
        DropboxSyncViewModel(
            logger = getWith("DropboxSyncViewModel"),
            dropboxSettings = get(),
            dropboxDataSource = get(),
            feedSyncRepository = get(),
            dateFormatter = get(),
            feedRetrieverRepository = get(),
            accountsRepository = get(),
        )
    }

    single<FeedSyncWorker> {
        FeedSyncJvmWorker(
            dropboxDataSource = get(),
            appEnvironment = appEnvironment,
            logger = getWith("FeedSyncJvmWorker"),
            feedSyncer = get(),
            feedSyncMessageQueue = get(),
            settingsRepository = get(),
            dispatcherProvider = get(),
            dropboxSettings = get(),
        )
    }

    factory<CurrentOS> { CurrentOS.Desktop }
}
