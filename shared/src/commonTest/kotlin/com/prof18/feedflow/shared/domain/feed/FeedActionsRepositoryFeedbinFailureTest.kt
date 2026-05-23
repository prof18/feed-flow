package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.feedbin.configureFeedbinMocks
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

class FeedActionsRepositoryFeedbinFailureTest : KoinTestBase() {

    private val feedActionsRepository: FeedActionsRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val feedStateRepository: FeedStateRepository by inject()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + getFeedSyncTestModules(
            feedbinBaseURL = "https://api.feedbin.com/",
            feedbinConfig = {
                configureFeedbinMocks()
                addMockResponse(
                    urlPattern = "/v2/unread_entries.json",
                    method = "DELETE",
                    statusCode = HttpStatusCode.InternalServerError,
                    responseContent = """{"error":"failed"}""",
                )
            },
        )

    @Test
    fun `markAsRead failure keeps pending action and local read state`() = runTest(testDispatcher) {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.FEEDBIN)
        settings.setSyncUsername("testuser")
        settings.setSyncPwd("testpassword")
        val feedSource = FeedSourceGenerator.feedSource(id = "source-1", title = "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)
        val feedItem = buildFeedItem("5031084432", "Article 1", 10000L, feedSource)
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
