package com.prof18.feedflow.shared.test.koin

import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.database.di.FEED_SYNC_SCOPE_NAME
import com.prof18.feedflow.feedsync.database.di.SYNC_DB_DRIVER
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.shared.di.getAllModulesModules
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.contentprefetch.ContentPrefetchRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedbinHistorySyncScheduler
import com.prof18.feedflow.shared.domain.feedsync.FeedbinHistorySyncSchedulerIosDesktop
import com.prof18.feedflow.shared.test.ContentPrefetchRepositoryFake
import com.prof18.feedflow.shared.test.FeedItemContentFileHandlerTestImpl
import com.prof18.feedflow.shared.test.TestDispatcherProvider
import com.prof18.feedflow.shared.test.createInMemoryDriver
import com.prof18.feedflow.shared.test.createInMemorySyncDriver
import com.prof18.feedflow.shared.test.testLogger
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
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

    private val noOpLogWriter = object : LogWriter() {
        override fun log(
            severity: Severity,
            message: String,
            tag: String,
            throwable: Throwable?,
        ) = Unit
    }

    fun createTestModules(): List<Module> =
        getAllModulesModules(
            appConfig = testAppConfig,
            crashReportingLogWriter = noOpLogWriter,
        ) + createTestOverridesModule() + getFeedSyncTestModules()

    fun createTestOverridesModule(): Module = module {
        single<SqlDriver> { createInMemoryDriver() }
        single { testLogger }
        single {
            DatabaseHelper(
                sqlDriver = get(),
                backgroundDispatcher = TestDispatcherProvider.testDispatcher,
                logger = getWith("DatabaseHelper"),
            )
        }
        single {
            SyncedDatabaseHelper(backgroundDispatcher = TestDispatcherProvider.testDispatcher)
        }
        scope(named(FEED_SYNC_SCOPE_NAME)) {
            scoped<SqlDriver>(named(SYNC_DB_DRIVER)) { createInMemorySyncDriver() }
        }
        single<Settings> { MapSettings() }
        single<DispatcherProvider> { TestDispatcherProvider }
        single<FeedSyncWorker> {
            object : FeedSyncWorker {
                override fun upload() = Unit
                override suspend fun uploadImmediate() = Unit
                override suspend fun download(isFirstSync: Boolean): SyncResult = SyncResult.Success
                override suspend fun syncFeedSources(): SyncResult = SyncResult.Success
                override suspend fun syncFeedItems(): SyncResult = SyncResult.Success
            }
        }
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
        single<FeedItemContentFileHandler> { FeedItemContentFileHandlerTestImpl() }
        single<HtmlParser> {
            object : HtmlParser {
                override fun getTextFromHTML(html: String): String? = null
                override fun getFaviconUrl(html: String): String? = null
                override fun getRssUrl(html: String): String? = null
            }
        }
        single<HtmlRetriever> {
            HtmlRetriever(
                logger = getWith("HtmlRetriever"),
                client = HttpClient(MockEngine) {
                    engine {
                        addHandler { _ ->
                            respondOk()
                        }
                    }
                },
            )
        }
        single<ContentPrefetchRepository> { ContentPrefetchRepositoryFake() }
        single<FeedbinHistorySyncScheduler> {
            // We use this dependencies for test scenarios because the android one uses WorkManager
            FeedbinHistorySyncSchedulerIosDesktop(
                feedbinRepository = get(),
                logger = getWith("FeedbinHistorySyncSchedulerIosDesktop"),
                dispatcherProvider = get(),
            )
        }
    }
}

inline fun <reified T> Scope.getWith(vararg params: Any?): T {
    return get(parameters = { parametersOf(*params) })
}
