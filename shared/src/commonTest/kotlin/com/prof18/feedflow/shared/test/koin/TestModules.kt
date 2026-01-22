package com.prof18.feedflow.shared.test.koin

import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.domain.FeedSourceLogoRetriever
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.database.di.FEED_SYNC_SCOPE_NAME
import com.prof18.feedflow.feedsync.database.di.SYNC_DB_DRIVER
import com.prof18.feedflow.shared.currentOS
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.DateFormatterImpl
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feed.FeedFontSizeRepository
import com.prof18.feedflow.shared.domain.feed.FeedImportExportRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourceLogoRetrieverImpl
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.mappers.RssChannelMapper
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
        factoryOf(::FeedFetcherRepository)
        factoryOf(::FeedImportExportRepository)
        singleOf(::SettingsRepository)
        singleOf(::FeedAppearanceSettingsRepository)
        singleOf(::FeedFontSizeRepository)
    }

    fun createTestDomainModule(): Module = module {
        factory<DateFormatter> {
            DateFormatterImpl(getWith("DateFormatter"))
        }
        factoryOf(::RssChannelMapper)
        factoryOf(::FeedSourceLogoRetrieverImpl) bind FeedSourceLogoRetriever::class
    }

    fun createTestSettingsModule(): Module = module {
        single<Settings> { MapSettings() }
    }

    fun createTestAccountModule(): Module = module {
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
            createTestSettingsModule(),
            createTestDomainModule(),
            createTestAccountModule(),
            createTestRepositoriesModule(),
            createTestNetworkModule(),
        )
    }
}

inline fun <reified T> Scope.getWith(vararg params: Any?): T {
    return get(parameters = { parametersOf(*params) })
}
