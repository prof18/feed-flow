package com.prof18.feedflow.shared.domain.feed

import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.test.FailableSqlDriver
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.createInMemoryDriver
import com.prof18.feedflow.shared.test.koin.TestModules
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.BeforeTest
import kotlin.test.Test

class FeedActionsRepositoryDatabaseFailureTest : KoinTestBase() {

    private val failableDriver = FailableSqlDriver(createInMemoryDriver())
    private val feedActionsRepository: FeedActionsRepository by inject()
    private val feedStateRepository: FeedStateRepository by inject()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + module {
            single<SqlDriver> { failableDriver }
        }

    @BeforeTest
    fun setUpAccount() {
        getKoin().get<NetworkSettings>().setSyncAccountType(SyncAccounts.LOCAL)
        failableDriver.failAllWritesAsBusy()
    }

    @Test
    fun `marking as read on a busy database reports a save failure`() = runTest(testDispatcher) {
        feedStateRepository.assertSaveFailed { feedActionsRepository.markAsRead(hashSetOf(FeedItemId("item"))) }
    }

    @Test
    fun `changing the read status on a busy database reports a save failure`() = runTest(testDispatcher) {
        feedStateRepository.assertSaveFailed {
            feedActionsRepository.updateReadStatus(FeedItemId("item"), isRead = true)
        }
    }

    @Test
    fun `bookmarking on a busy database reports a save failure`() = runTest(testDispatcher) {
        feedStateRepository.assertSaveFailed {
            feedActionsRepository.updateBookmarkStatus(FeedItemId("item"), isBookmarked = true)
        }
    }

    @Test
    fun `marking everything as read on a busy database reports a save failure`() = runTest(testDispatcher) {
        feedStateRepository.assertSaveFailed { feedActionsRepository.markAllFeedAsRead(FeedFilter.Timeline) }
    }
}
