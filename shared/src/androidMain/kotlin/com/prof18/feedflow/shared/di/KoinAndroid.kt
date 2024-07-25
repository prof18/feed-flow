package com.prof18.feedflow.shared.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.createDatabaseDriver
import com.prof18.feedflow.shared.domain.HtmlParser
import com.prof18.feedflow.shared.domain.JvmHtmlParser
import com.prof18.feedflow.shared.domain.ReaderModeExtractor
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncAndroidWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.domain.feedsync.SyncWorkManager
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.presentation.BaseViewModel
import com.prof18.feedflow.shared.presentation.DropboxSyncViewModel
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.definition.Definition
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel as koinViewModel

internal actual inline fun <reified T : BaseViewModel> Module.viewModel(
    qualifier: Qualifier?,
    noinline definition: Definition<T>,
): KoinDefinition<T> = koinViewModel(qualifier, definition)

internal actual fun getPlatformModule(appEnvironment: AppEnvironment): Module = module {
    single<SqlDriver> {
        createDatabaseDriver(
            context = get(),
            appEnvironment = appEnvironment,
        )
    }

    factory {
        OpmlFeedHandler(
            dispatcherProvider = get(),
        )
    }

    factory<HtmlParser> {
        JvmHtmlParser(
            logger = getWith("JvmHtmlParser"),
        )
    }

    single<Settings> {
        val sharedPrefs = get<Context>().getSharedPreferences("feedflow.shared.pref", Context.MODE_PRIVATE)
        SharedPreferencesSettings(sharedPrefs)
    }

    single<DispatcherProvider> {
        object : DispatcherProvider {
            override val main: CoroutineDispatcher = Dispatchers.Main
            override val default: CoroutineDispatcher = Dispatchers.Default
            override val io: CoroutineDispatcher = Dispatchers.IO
        }
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
            feedSyncMessageQueue = get(),
        )
    }

    single {
        FeedSyncAndroidWorker(
            context = get(),
            dropboxDataSource = get(),
            appEnvironment = appEnvironment,
            logger = getWith("FeedSyncAndroidWorker"),
            feedSyncer = get(),
            feedSyncMessageQueue = get(),
            dispatcherProvider = get(),
            dropboxSettings = get(),
            settingsRepository = get(),
        )
    } bind FeedSyncWorker::class

    workerOf(::SyncWorkManager)
}
