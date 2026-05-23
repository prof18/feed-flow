package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.greader.configureFreshRssMocks
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import com.prof18.feedflow.shared.test.koin.TestModules
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedActionsRepositoryFreshRssFailureTest : KoinTestBase() {

    private val feedActionsRepository: FeedActionsRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val feedStateRepository: FeedStateRepository by inject()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + getFeedSyncTestModules(
            gReaderProvider = SyncAccounts.FRESH_RSS,
            gReaderBaseURL = "https://freshrss.example.com/api/greader.php/",
            gReaderConfig = {
                addMockResponse(
                    urlPattern = "/reader/api/0/edit-tag",
                    method = "POST",
                    statusCode = HttpStatusCode.InternalServerError,
                    responseContent = "failed",
                )
                configureFreshRssMocks()
            },
        )

    @Test
    fun `markAsRead failure keeps pending action and local read state`() = runTest(testDispatcher) {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.FRESH_RSS)
        settings.setSyncUsername("testuser")
        settings.setSyncPwd("testpassword")
        settings.setSyncUrl("https://freshrss.example.com/api/greader.php/")
        val feedSource = FeedSourceGenerator.feedSource(id = "source-1", title = "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)
        val feedItem = buildFeedItem("0006494daa10a1e3", "Article 1", 10000L, feedSource)
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)
        feedStateRepository.getFeeds()
        advanceUntilIdle()

        feedActionsRepository.markAsRead(hashSetOf(FeedItemId(feedItem.id)))
        advanceUntilIdle()

        val updatedItem = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        ).first { it.url_hash == feedItem.id }
        assertTrue(updatedItem.is_read)
        assertEquals(1, databaseHelper.countReadStatusPendingActions())
    }
}
