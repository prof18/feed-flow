package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.greader.configureMinifluxMocks
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

class FeedSourcesRepositoryMinifluxTest : KoinTestBase() {

    private val feedSourcesRepository: FeedSourcesRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + getFeedSyncTestModules(
            gReaderProvider = SyncAccounts.MINIFLUX,
            gReaderBaseURL = "https://miniflux.example.com/reader/api/0/",
            gReaderConfig = {
                configureMinifluxMocks()
            },
        )

    fun setupMinifluxAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.MINIFLUX)
        settings.setSyncUsername("testuser")
        settings.setSyncPwd("testpassword")
        settings.setSyncUrl("https://miniflux.example.com/reader/api/0/")
    }

    @Test
    fun `deleteFeed should call gReaderRepository and delete feed source`() = runTest(testDispatcher) {
        setupMinifluxAccount()
        val feedSource = createFeedSource(
            id = "feed/1",
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
        setupMinifluxAccount()
        val feedSource = createFeedSource(
            id = "feed/1",
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
        setupMinifluxAccount()
        val feedUrl = "https://example.com/feed"
        val category = FeedSourceCategory(id = "user/1/label/Tech", title = "Tech")

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
        setupMinifluxAccount()
        val originalFeedSource = createFeedSource(
            id = "feed/1",
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
