package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.greader.configureBazquxMocks
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedSourcesRepositoryBazquxTest : KoinTestBase() {

    private val feedSourcesRepository: FeedSourcesRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + getFeedSyncTestModules(
            gReaderProvider = SyncAccounts.BAZQUX,
            gReaderBaseURL = "https://bazqux.com/reader/api/0/",
            gReaderConfig = {
                configureBazquxMocks()
            },
        )

    fun setupBazquxAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.BAZQUX)
        settings.setSyncUsername("testuser")
        settings.setSyncPwd("testpassword")
        settings.setSyncUrl("https://bazqux.com/reader/api/0/")
    }

    @Test
    fun `deleteFeed should call gReaderRepository and delete feed source`() = runTest(testDispatcher) {
        setupBazquxAccount()
        val feedSource = createFeedSource(
            id = "feed/https://9to5google.com/feed/",
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
    fun `updateFeedSourceName should call gReaderRepository and update name`() = runTest(testDispatcher) {
        setupBazquxAccount()
        val feedSource = createFeedSource(
            id = "feed/https://9to5google.com/feed/",
            title = "Original Name",
        )
        databaseHelper.insertFeedSourceWithCategory(feedSource)
        advanceUntilIdle()

        val newName = "Updated Name"
        feedSourcesRepository.updateFeedSourceName(feedSource.id, newName)
        advanceUntilIdle()

        val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertTrue(updatedFeedSource != null, "Feed source should exist")
        assertEquals(updatedFeedSource.title, newName, "Feed source name should be updated")
    }

    @Test
    fun `addFeedSource should call gReaderRepository and return success`() = runTest(testDispatcher) {
        setupBazquxAccount()
        val feedUrl = "https://example.com/feed"
        val category = FeedSourceCategory(id = "user/01234567890123456789/label/Tech", title = "Tech")

        val result = feedSourcesRepository.addFeedSource(
            feedUrl = feedUrl,
            categoryName = category,
            isNotificationEnabled = false,
        )
        advanceUntilIdle()

        assertIs<FeedAddedState.FeedAdded>(result, "Should return FeedAdded on success")
    }

    @Test
    fun `editFeedSource should call gReaderRepository and return success`() = runTest(testDispatcher) {
        setupBazquxAccount()
        val originalFeedSource = createFeedSource(
            id = "feed/https://9to5google.com/feed/",
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
        assertEquals(result.feedName, "Updated Name", "Should return updated feed name")
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
