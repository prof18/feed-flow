package com.prof18.feedflow.shared.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.createDatabaseDriver
import com.prof18.feedflow.shared.domain.FeedDownloadWorker
import com.prof18.feedflow.shared.domain.FeedDownloadWorkerEnqueuer
import com.prof18.feedflow.shared.domain.JvmHtmlParser
import com.prof18.feedflow.shared.domain.contentprefetch.ContentPrefetchRepository
import com.prof18.feedflow.shared.domain.contentprefetch.ContentPrefetchRepositoryAndroid
import com.prof18.feedflow.shared.domain.contentprefetch.ContentPrefetchWorker
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedbinHistorySyncScheduler
import com.prof18.feedflow.shared.domain.feedsync.FeedbinHistorySyncSchedulerAndroid
import com.prof18.feedflow.shared.domain.feedsync.FeedbinHistorySyncWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncAndroidWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.domain.feedsync.SyncWorkManager
import com.prof18.feedflow.shared.domain.model.CurrentOS
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.domain.parser.AndroidFeedItemParserWorker
import com.prof18.feedflow.shared.domain.parser.FeedItemContentFileHandlerAndroid
import com.prof18.feedflow.shared.presentation.DropboxSyncViewModel
import com.prof18.feedflow.shared.presentation.GoogleDriveSyncViewModel
import com.prof18.feedflow.shared.presentation.ThemeViewModel
import com.prof18.feedflow.shared.utils.UserAgentInterceptor
import com.prof18.rssparser.RssParserBuilder
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import org.koin.androidx.workmanager.dsl.worker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

internal actual fun getPlatformModule(appEnvironment: AppEnvironment): Module = module {
    single {
        RssParserBuilder(
            callFactory = OkHttpClient
                .Builder()
                .addInterceptor(UserAgentInterceptor())
                .build(),
        ).build()
    }

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

    single<FeedbinHistorySyncScheduler> {
        FeedbinHistorySyncSchedulerAndroid(
            appContext = get(),
        )
    }

    single<FeedItemContentFileHandler> {
        FeedItemContentFileHandlerAndroid(
            appContext = get(),
            dispatcherProvider = get(),
            logger = getWith("FeedItemContentFileHandler"),
        )
    }

    single<FeedItemParserWorker> {
        AndroidFeedItemParserWorker(
            htmlRetriever = get(),
            appContext = get(),
            logger = getWith("FeedItemParserWorker"),
            dispatcherProvider = get(),
            feedItemContentFileHandler = get(),
            settingsRepository = get(),
        )
    }

    viewModel {
        DropboxSyncViewModel(
            logger = getWith("DropboxSyncViewModel"),
            dropboxSettings = get(),
            dropboxDataSource = get(),
            feedSyncRepository = get(),
            dateFormatter = get(),
            feedFetcherRepository = get(),
            feedSyncMessageQueue = get(),
            accountsRepository = get(),
        )
    }

    viewModel {
        GoogleDriveSyncViewModel(
            logger = getWith("GoogleDriveSyncViewModel"),
            googleDriveSettings = get(),
            googleDriveDataSource = get(),
            feedSyncRepository = get(),
            dateFormatter = get(),
            feedFetcherRepository = get(),
            feedSyncMessageQueue = get(),
            accountsRepository = get(),
        )
    }

    single {
        FeedSyncAndroidWorker(
            context = get(),
            dropboxDataSource = get(),
            googleDriveDataSource = get(),
            appEnvironment = appEnvironment,
            logger = getWith("FeedSyncAndroidWorker"),
            feedSyncer = get(),
            feedSyncMessageQueue = get(),
            dispatcherProvider = get(),
            dropboxSettings = get(),
            googleDriveSettings = get(),
            settingsRepository = get(),
            accountsRepository = get(),
        )
    } bind FeedSyncWorker::class

    factory<CurrentOS> { CurrentOS.Android }

    workerOf(::SyncWorkManager)
    workerOf(::FeedbinHistorySyncWorker)

    worker {
        FeedDownloadWorker(
            feedFetcherRepository = get(),
            widgetUpdater = get(),
            databaseHelper = get(),
            notifier = get(),
            appContext = get(),
            workerParams = get(),
        )
    }

    single {
        FeedDownloadWorkerEnqueuer(
            settingsRepository = get(),
            context = get(),
        )
    }

    worker {
        ContentPrefetchWorker(
            appContext = get(),
            workerParams = get(),
            databaseHelper = get(),
            dispatcherProvider = get(),
            htmlRetriever = get(),
            feedItemContentFileHandler = get(),
            logger = getWith("ContentPrefetchWorker"),
        )
    }

    single<ContentPrefetchRepository> {
        ContentPrefetchRepositoryAndroid(
            logger = getWith("ContentPrefetchRepositoryAndroid"),
            settingsRepository = get(),
            databaseHelper = get(),
            dispatcherProvider = get(),
            htmlRetriever = get(),
            appContext = get(),
            feedItemContentFileHandler = get(),
        )
    }

    viewModel {
        ThemeViewModel(
            settingsRepository = get(),
        )
    }
}
