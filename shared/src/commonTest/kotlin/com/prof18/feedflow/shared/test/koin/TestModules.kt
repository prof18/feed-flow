package com.prof18.feedflow.shared.test.koin

import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.domain.FeedSourceLogoRetriever
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.database.di.FEED_SYNC_SCOPE_NAME
import com.prof18.feedflow.feedsync.database.di.SYNC_DB_DRIVER
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.feedbin.di.getFeedbinModule
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.feedsync.greader.di.getGReaderModule
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.currentOS
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.DateFormatterImpl
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.contentprefetch.ContentPrefetchRepository
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feed.FeedFontSizeRepository
import com.prof18.feedflow.shared.domain.feed.FeedImportExportRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourceLogoRetrieverImpl
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.domain.mappers.RssChannelMapper
import com.prof18.feedflow.shared.test.FeedItemContentFileHandlerTestImpl
import com.prof18.feedflow.shared.test.TestDispatcherProvider
import com.prof18.feedflow.shared.test.createInMemoryDriver
import com.prof18.feedflow.shared.test.createInMemorySyncDriver
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module

object TestModules {

    val testAppConfig = AppConfig(
        appEnvironment = AppEnvironment.Debug,
        isLoggingEnabled = true,
        isDropboxSyncEnabled = true,
        isGoogleDriveSyncEnabled = true,
        isIcloudSyncEnabled = true,
        appVersion = "1.0.0",
        platformName = "Test",
        platformVersion = "1.0.0",
    )

    fun createTestDatabaseModule(): Module = module {
        single { createInMemoryDriver() }
        single {
            DatabaseHelper(
                sqlDriver = get(),
                backgroundDispatcher = TestDispatcherProvider.testDispatcher,
                logger = getWith("DatabaseHelper"),
            )
        }
    }

    fun createTestSyncedDatabaseModule(): Module = module {
        single { SyncedDatabaseHelper(backgroundDispatcher = TestDispatcherProvider.testDispatcher) }
        scope(named(FEED_SYNC_SCOPE_NAME)) {
            scoped<SqlDriver>(named(SYNC_DB_DRIVER)) { createInMemorySyncDriver() }
        }
    }

    fun createTestRepositoriesModule(): Module = module {
        singleOf(::FeedStateRepository)
        single { FeedSyncMessageQueue() }
        single<FeedSyncWorker> {
            object : FeedSyncWorker {
                override fun upload() {
                    // No-op
                }

                override suspend fun uploadImmediate() {
                    // No-op
                }

                override suspend fun download(isFirstSync: Boolean): SyncResult = SyncResult.Success
                override suspend fun syncFeedSources(): SyncResult = SyncResult.Success
                override suspend fun syncFeedItems(): SyncResult = SyncResult.Success
            }
        }
        singleOf(::FeedSyncRepository)
        single<FeedItemParserWorker> {
            object : FeedItemParserWorker {
                override suspend fun parse(feedItemId: String, url: String): ParsingResult =
                    ParsingResult.Success(
                        htmlContent = "Content",
                        title = "Title",
                        siteName = "Site Name",
                    )
            }
        }
        single {
            FeedActionsRepository(
                databaseHelper = get(),
                feedSyncRepository = get(),
                gReaderRepository = get(),
                feedbinRepository = get(),
                accountsRepository = get(),
                feedStateRepository = get(),
                feedItemParserWorker = get(),
            )
        }
        factoryOf(::FeedSourcesRepository)
        factoryOf(::FeedCategoryRepository)
        factory {
            FeedFetcherRepository(
                dispatcherProvider = get(),
                feedStateRepository = get(),
                gReaderRepository = get(),
                feedbinRepository = get(),
                databaseHelper = get(),
                feedSyncRepository = get(),
                settingsRepository = get(),
                logger = getWith("FeedFetcherRepository"),
                rssParser = get(),
                rssChannelMapper = get(),
                dateFormatter = get(),
                feedSourceLogoRetriever = get(),
                contentPrefetchRepository = get(),
            )
        }
        factoryOf(::FeedImportExportRepository)
        singleOf(::SettingsRepository)
        singleOf(::FeedAppearanceSettingsRepository)
        singleOf(::FeedFontSizeRepository)
        factoryOf(::FeedWidgetRepository)
    }

    fun createTestDomainModule(): Module = module {
        factory<DateFormatter> {
            DateFormatterImpl(getWith("DateFormatter"))
        }
        factoryOf(::RssChannelMapper)
        factoryOf(::FeedSourceLogoRetrieverImpl) bind FeedSourceLogoRetriever::class
        single<FeedItemContentFileHandler> { FeedItemContentFileHandlerTestImpl() }
        single<HtmlParser> {
            object : HtmlParser {
                override fun getTextFromHTML(html: String): String? = null
                override fun getFaviconUrl(html: String): String? = null
                override fun getRssUrl(html: String): String? = null
            }
        }
        singleOf(::HtmlRetriever)
        single<ContentPrefetchRepository> {
            object : ContentPrefetchRepository {
                override suspend fun prefetchContent() {
                    // No-op
                }

                override fun startBackgroundFetching() {
                    // No-op
                }

                override suspend fun cancelFetching() {
                    // No-op
                }
            }
        }
    }

    fun createTestSettingsModule(): Module = module {
        single<Settings> { MapSettings() }
        single<DispatcherProvider> { TestDispatcherProvider }
    }

    fun createTestAccountModule(): Module = module {
        singleOf(::DropboxSettings)
        singleOf(::GoogleDriveSettings)
        singleOf(::ICloudSettings)
        singleOf(::NetworkSettings)
        single {
            AccountsRepository(
                currentOS = currentOS,
                dropboxSettings = get(),
                googleDriveSettings = get(),
                icloudSettings = get(),
                appConfig = testAppConfig,
                gReaderRepository = get(),
                networkSettings = get(),
                feedbinRepository = get(),
            )
        }
    }

    /**
     * Mock Logging module
     */
    fun createTestLoggingModule(): Module = module {
        val baseLogger = Logger(
            config = StaticConfig(
                logWriterList = emptyList(),
            ),
            tag = "FeedFlowTest",
        )
        factory {
            val tag = it.getOrNull<String>()
            if (tag != null) {
                baseLogger.withTag(tag)
            } else {
                baseLogger
            }
        }
    }

    fun createTestNetworkModule(): Module = module {
        single {
            HttpClient(MockEngine) {
                engine {
                    addHandler { _ ->
                        respondOk()
                    }
                }
            }
        }
    }

    fun createCompleteTestModule(): Module = module {
        includes(
            createTestLoggingModule(),
            createTestDatabaseModule(),
            createTestSyncedDatabaseModule(),
            createTestSettingsModule(),
            createTestDomainModule(),
            createTestAccountModule(),
            createTestRepositoriesModule(),
            createTestNetworkModule(),
            getGReaderModule(AppEnvironment.Debug),
            getFeedbinModule(AppEnvironment.Debug),
        )
    }
}

inline fun <reified T> Scope.getWith(vararg params: Any?): T {
    return get(parameters = { parametersOf(*params) })
}
