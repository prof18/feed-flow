package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.feedbin.configureFeedbinMocks
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import com.prof18.feedflow.shared.test.koin.TestModules
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.test.inject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedSourcesRepositoryFeedbinTest : KoinTestBase() {

    private val feedSourcesRepository: FeedSourcesRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val networkSettings: NetworkSettings by inject()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + getFeedSyncTestModules(
            feedbinBaseURL = "https://api.feedbin.com/",
            feedbinConfig = {
                configureFeedbinMocks()
            },
        )

    @BeforeTest
    fun setupFeedbinAccount() {
        networkSettings.setSyncAccountType(SyncAccounts.FEEDBIN)
        networkSettings.setSyncUsername("testuser")
        networkSettings.setSyncPwd("testpassword")
    }

    @Test
    fun `deleteFeed should call feedbinRepository and delete feed source`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(
            id = "feedbin/9115993/1240842",
            title = "Test Feed",
        )
        databaseHelper.insertFeedSourceWithCategory(feedSource)
        advanceUntilIdle()

        feedSourcesRepository.deleteFeed(feedSource)
        advanceUntilIdle()

        val deletedFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertNull(deletedFeedSource, "Feed source should be deleted")
    }

    @Test
    fun `updateFeedSourceName should call feedbinRepository and update name`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(
            id = "feedbin/9115993/1240842",
            title = "Original Name",
        )
        databaseHelper.insertFeedSourceWithCategory(feedSource)
        advanceUntilIdle()

        val newName = "Updated Name"
        feedSourcesRepository.updateFeedSourceName(feedSource.id, newName)
        advanceUntilIdle()

        val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertTrue(updatedFeedSource != null, "Feed source should exist")
        assertTrue(updatedFeedSource?.title == newName, "Feed source name should be updated")
    }

    @Test
    fun `addFeedSource should call feedbinRepository and return success`() = runTest(testDispatcher) {
        val feedUrl = "https://example.com/feed"
        val category = FeedSourceCategory(id = "user/-/label/Tech", title = "Tech")

        val result = feedSourcesRepository.addFeedSource(
            feedUrl = feedUrl,
            categoryName = category,
            isNotificationEnabled = false,
        )
        advanceUntilIdle()

        assertIs<FeedAddedState.FeedAdded>(result, "Should return FeedAdded on success")
    }

    @Test
    fun `editFeedSource should call feedbinRepository and return success`() = runTest(testDispatcher) {
        val originalFeedSource = createFeedSource(
            id = "feedbin/9115993/1240842",
            title = "Original Name",
        )
        databaseHelper.insertFeedSourceWithCategory(originalFeedSource)
        advanceUntilIdle()

        val newFeedSource = originalFeedSource.copy(
            title = "Updated Name",
        )

        val result = feedSourcesRepository.editFeedSource(
            newFeedSource = newFeedSource,
            originalFeedSource = originalFeedSource,
        )
        advanceUntilIdle()

        assertIs<FeedEditedState.FeedEdited>(result, "Should return FeedEdited on success")
        assertTrue(result.feedName == "Updated Name", "Should return updated feed name")
    }

    private fun createFeedSource(
        id: String,
        title: String,
        category: FeedSourceCategory? = null,
    ): FeedSource = FeedSource(
        id = id,
        url = "https://example.com/feed.xml",
        title = title,
        category = category,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = "https://example.com",
        fetchFailed = false,
        linkOpeningPreference = com.prof18.feedflow.core.model.LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
    )
}
