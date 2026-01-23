package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.presentation.model.DeeplinkFeedState
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import io.kotest.property.arbitrary.next
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DeeplinkFeedViewModelTest : KoinTestBase() {

    private val viewModel: DeeplinkFeedViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val accountsRepository: AccountsRepository by inject()

    @Test
    fun `getReaderModeUrl updates state to Success when item exists`() = runTest {
        val feedItem = FeedItemGenerator.feedItemArb.next()
        val feedItemId = FeedItemId(feedItem.id)
        val feedItemUrlInfo = FeedItemUrlInfo(
            id = feedItem.id,
            url = feedItem.url,
            title = feedItem.title,
            isBookmarked = feedItem.isBookmarked,
            linkOpeningPreference = feedItem.feedSource.linkOpeningPreference,
            commentsUrl = feedItem.commentsUrl,
        )

        // Seed database
        databaseHelper.insertFeedSource(
            listOf(
                ParsedFeedSource(
                    id = feedItem.feedSource.id,
                    url = feedItem.feedSource.url,
                    title = feedItem.feedSource.title,
                    category = feedItem.feedSource.category,
                    logoUrl = feedItem.feedSource.logoUrl,
                    websiteUrl = feedItem.feedSource.websiteUrl,
                ),
            ),
        )

        databaseHelper.insertFeedItems(
            listOf(feedItem),
            lastSyncTimestamp = 0,
        )

        viewModel.deeplinkFeedState.test {
            assertEquals(DeeplinkFeedState.Loading, awaitItem())

            viewModel.getReaderModeUrl(feedItemId)

            val successState = awaitItem()
            assertIs<DeeplinkFeedState.Success>(successState)
            assertEquals(feedItemUrlInfo.id, successState.data.id)
            assertEquals(feedItemUrlInfo.url, successState.data.url)
            assertEquals(feedItemUrlInfo.title, successState.data.title)
        }
    }

    @Test
    fun `getReaderModeUrl updates state to Error when item does not exist`() = runTest {
        val feedItemId = FeedItemId("non-existent")

        viewModel.deeplinkFeedState.test {
            assertEquals(DeeplinkFeedState.Loading, awaitItem())

            viewModel.getReaderModeUrl(feedItemId)

            assertEquals(DeeplinkFeedState.Error, awaitItem())
        }
    }

    @Test
    fun `markAsRead calls repository`() = runTest {
        val feedItem = FeedItemGenerator.unreadFeedItemArb().next()
        val feedItemId = FeedItemId(feedItem.id)

        // Seed database
        databaseHelper.insertFeedSource(
            listOf(
                ParsedFeedSource(
                    id = feedItem.feedSource.id,
                    url = feedItem.feedSource.url,
                    title = feedItem.feedSource.title,
                    category = feedItem.feedSource.category,
                    logoUrl = feedItem.feedSource.logoUrl,
                    websiteUrl = feedItem.feedSource.websiteUrl,
                ),
            ),
        )

        databaseHelper.insertFeedItems(
            listOf(feedItem),
            lastSyncTimestamp = 0,
        )

        // Ensure we are in LOCAL account to use databaseHelper assertions easily
        accountsRepository.clearAccount()
        assertEquals(SyncAccounts.LOCAL, accountsRepository.getCurrentSyncAccount())

        viewModel.markAsRead(feedItemId)

        val feeds = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        val item = feeds.find { it.url_hash == feedItemId.id }
        assertTrue(item?.is_read == true)
    }
}
