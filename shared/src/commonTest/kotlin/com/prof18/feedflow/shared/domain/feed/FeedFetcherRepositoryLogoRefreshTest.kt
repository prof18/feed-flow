package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.domain.FeedSourceLogoRetriever
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.toParsedFeedSource
import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssImage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FeedFetcherRepositoryLogoRefreshTest : FeedFetcherRepositoryTestBase() {

    private val fakeRssParserWrapper = FakeLogoRssParserWrapper()
    private val fakeLogoRetriever = FakeFeedSourceLogoRetriever()
    private val feedFetcherRepository: FeedFetcherRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() + module {
            single<RssParserWrapper> { fakeRssParserWrapper }
            single<FeedSourceLogoRetriever> { fakeLogoRetriever }
        }

    // Not a @BeforeTest: the native runner does not order it after the base class's own.
    private fun setupLocalAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.LOCAL)
    }

    @Test
    fun `keeps a stored logo that still resolves`() = runTest(testDispatcher) {
        insertSource(logoUrl = STORED_LOGO)
        fakeLogoRetriever.availableUrls += STORED_LOGO

        fetch()

        assertEquals(STORED_LOGO, storedLogoUrl())
        assertEquals(emptyList(), fakeLogoRetriever.channelResolutions)
    }

    @Test
    fun `replaces a stored logo that stopped resolving`() = runTest(testDispatcher) {
        insertSource(logoUrl = STORED_LOGO)
        fakeLogoRetriever.channelLogo = ROTATED_LOGO

        fetch()

        assertEquals(ROTATED_LOGO, storedLogoUrl())
    }

    @Test
    fun `resolves from the website when the channel image is the logo that died`() =
        runTest(testDispatcher) {
            insertSource(logoUrl = STORED_LOGO)
            fakeLogoRetriever.channelLogo = STORED_LOGO
            fakeLogoRetriever.websiteLogo = ROTATED_LOGO
            fakeRssParserWrapper.channelImageUrl = STORED_LOGO

            fetch()

            assertEquals(ROTATED_LOGO, storedLogoUrl())
            assertEquals(listOf<String?>(WEBSITE_URL), fakeLogoRetriever.websiteResolutions)
        }

    @Test
    fun `resolves a missing logo without checking availability first`() = runTest(testDispatcher) {
        insertSource(logoUrl = null)
        fakeLogoRetriever.channelLogo = ROTATED_LOGO

        fetch()

        assertEquals(ROTATED_LOGO, storedLogoUrl())
        assertEquals(emptyList(), fakeLogoRetriever.availabilityChecks)
    }

    @Test
    fun `re-checks the logo of a feed that answered not modified`() = runTest(testDispatcher) {
        insertSource(logoUrl = STORED_LOGO)
        fakeRssParserWrapper.notModified = true
        fakeLogoRetriever.websiteLogo = ROTATED_LOGO

        fetch()

        assertEquals(ROTATED_LOGO, storedLogoUrl())
        assertEquals(listOf(STORED_LOGO), fakeLogoRetriever.availabilityChecks)
    }

    @Test
    fun `drops the memoised lookups before a refresh starts`() = runTest(testDispatcher) {
        insertSource(logoUrl = STORED_LOGO)
        fakeLogoRetriever.availableUrls += STORED_LOGO

        fetch()

        assertTrue(fakeLogoRetriever.invalidationCount >= 1)
    }

    private suspend fun TestScope.fetch() {
        setupLocalAccount()
        fakeRssParserWrapper.channelLink = WEBSITE_URL
        feedFetcherRepository.fetchFeeds(forceRefresh = true)
        advanceUntilIdle()
    }

    private suspend fun insertSource(logoUrl: String?) {
        databaseHelper.insertFeedSource(listOf(feedSource(logoUrl).toParsedFeedSource()))
    }

    private suspend fun storedLogoUrl(): String? =
        databaseHelper.getFeedSources().single { it.id == SOURCE_ID }.logoUrl

    private fun feedSource(logoUrl: String?): FeedSource = FeedSource(
        id = SOURCE_ID,
        url = FEED_URL,
        title = "Logo Feed",
        category = null,
        lastSyncTimestamp = null,
        logoUrl = logoUrl,
        websiteUrl = WEBSITE_URL,
        fetchFailed = false,
        articleOpenMode = ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
    )

    private companion object {
        const val SOURCE_ID = "logo-source"
        const val FEED_URL = "https://example.com/logo-source/rss.xml"
        const val WEBSITE_URL = "https://example.com"
        const val STORED_LOGO = "https://example.com/build/aaaa/favicon.png"
        const val ROTATED_LOGO = "https://example.com/build/bbbb/favicon.png"
    }
}

private class FakeFeedSourceLogoRetriever : FeedSourceLogoRetriever {
    val availableUrls = mutableSetOf<String>()
    val availabilityChecks = mutableListOf<String>()
    val channelResolutions = mutableListOf<String?>()
    val websiteResolutions = mutableListOf<String?>()
    var channelLogo: String? = null
    var websiteLogo: String? = null
    var invalidationCount = 0
        private set

    override suspend fun getFeedSourceLogoUrl(rssChannel: RssChannel): String? {
        channelResolutions += rssChannel.link
        return channelLogo
    }

    override suspend fun getFeedSourceLogoUrl(websiteLink: String?): String? {
        websiteResolutions += websiteLink
        return websiteLogo
    }

    override suspend fun isStoredLogoStillAvailable(logoUrl: String): Boolean {
        availabilityChecks += logoUrl
        return logoUrl in availableUrls
    }

    override suspend fun invalidateLookupCaches() {
        invalidationCount += 1
    }
}

private class FakeLogoRssParserWrapper : RssParserWrapper {
    var channelLink: String? = null
    var channelImageUrl: String? = null
    var notModified = false

    override suspend fun getRssChannel(url: String, allowBrowserTier: Boolean): RssChannel {
        if (notModified) {
            throw HttpException(code = HTTP_NOT_MODIFIED, message = "Not Modified")
        }
        return RssChannelGenerator.rssChannel(
            title = "Logo Feed",
            link = channelLink,
            image = channelImageUrl?.let { RssImage(title = null, url = it, link = null, description = null) },
            items = emptyList(),
        )
    }

    private companion object {
        const val HTTP_NOT_MODIFIED = 304
    }
}
