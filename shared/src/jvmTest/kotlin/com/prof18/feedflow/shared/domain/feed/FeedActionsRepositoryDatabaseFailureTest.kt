package com.prof18.feedflow.shared.domain.feed

import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.test.FailableSqlDriver
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.createInMemoryDriver
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.koin.TestModules
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FeedActionsRepositoryDatabaseFailureTest : KoinTestBase() {

    private val failableDriver = FailableSqlDriver(createInMemoryDriver())
    private val feedActionsRepository: FeedActionsRepository by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

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
    fun `a read or bookmark change the database rejected is undone in the list`() = runTest(testDispatcher) {
        failableDriver.writeFailure = { null }
        val source = FeedSourceGenerator.feedSource(id = "source")
        databaseHelper.insertFeedSource(listOf(source.toParsedFeedSource()))
        databaseHelper.insertFeedItems(
            listOf(buildFeedItem(id = "item", title = "Item", pubDateMillis = 1000, source = source)),
            lastSyncTimestamp = 0,
        )
        feedStateRepository.getFeeds()
        failableDriver.failAllWritesAsBusy()

        feedActionsRepository.updateReadStatus(FeedItemId("item"), isRead = true)
        feedActionsRepository.updateBookmarkStatus(FeedItemId("item"), isBookmarked = true)

        val item = feedStateRepository.feedState.value.single()
        assertEquals("item", item.id)
        assertFalse(item.isRead)
        assertFalse(item.isBookmarked)
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
