package com.prof18.feedflow.shared.domain.feed

import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import com.prof18.feedflow.shared.test.FailableSqlDriver
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.createInMemoryDriver
import com.prof18.feedflow.shared.test.koin.TestModules
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedSourcesRepositoryEditDatabaseFailureTest : KoinTestBase() {

    private val failableDriver = FailableSqlDriver(createInMemoryDriver())
    private val feedSourcesRepository: FeedSourcesRepository by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + module {
            single<SqlDriver> { failableDriver }
        }

    @Test
    fun `editing a feed on a busy database returns a generic error and reports a save failure`() =
        runTest(testDispatcher) {
            getKoin().get<NetworkSettings>().setSyncAccountType(SyncAccounts.LOCAL)
            val original = source()
            databaseHelper.insertFeedSource(listOf(original.toParsedFeedSource()))
            failableDriver.failAllWritesAsBusy()

            var result: FeedEditedState? = null
            feedStateRepository.assertSaveFailed {
                result = feedSourcesRepository.editFeedSource(original.copy(isPinned = true), original)
            }

            assertEquals(FeedEditedState.Error.GenericError, result)
        }

    private fun source() = FeedSource(
        id = "source-1",
        url = "https://example.com/source-1/rss.xml",
        title = "Source",
        category = null,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = null,
        fetchFailed = false,
        articleOpenMode = ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
    )
}
