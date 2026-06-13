package com.prof18.feedflow.shared.domain.feed

import app.cash.turbine.test
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSyncError
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.greader.configureMinifluxMocksWithBadTokenSyncFailure
import com.prof18.feedflow.shared.presentation.model.SyncError
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FeedFetcherRepositoryMinifluxBadTokenTest : FeedFetcherRepositoryTestBase() {

    private val feedFetcherRepository: FeedFetcherRepository by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() + getFeedSyncTestModules(
            gReaderProvider = SyncAccounts.MINIFLUX,
            gReaderBaseURL = "https://miniflux.example.com/",
            gReaderConfig = {
                configureMinifluxMocksWithBadTokenSyncFailure()
            },
        )

    @Test
    fun `fetchFeeds emits bad token sync error when Miniflux rejects token`() = runTest(testDispatcher) {
        setupMinifluxAccount()
        databaseHelper.insertFeedSource(listOf(createFeedSource().toParsedFeedSource()))

        feedStateRepository.errorState.test {
            feedFetcherRepository.fetchFeeds()

            val error = assertIs<SyncError>(awaitItem())
            assertEquals(FeedSyncError.GReaderBadToken, error.errorCode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun setupMinifluxAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.MINIFLUX)
        settings.setSyncUsername("testuser")
        settings.setSyncPwd("test-auth-token")
        settings.setSyncUrl("https://miniflux.example.com")
    }

    private fun createFeedSource(): FeedSource = FeedSource(
        id = "feed/1",
        url = "https://www.20percent.berlin/feed",
        title = "20 Percent Berlin",
        category = null,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = "https://www.20percent.berlin/feed",
        fetchFailed = false,
        linkOpeningPreference = LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
    )
}
