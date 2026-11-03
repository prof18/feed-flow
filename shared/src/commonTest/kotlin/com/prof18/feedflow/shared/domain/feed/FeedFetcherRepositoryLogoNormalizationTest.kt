package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.toParsedFeedSource
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
import kotlin.test.assertNull

/** Uses the real [FeedSourceLogoRetrieverImpl]: the URL normalisation is what is under test. */
@OptIn(ExperimentalCoroutinesApi::class)
class FeedFetcherRepositoryLogoNormalizationTest : FeedFetcherRepositoryTestBase() {

    private val fakeRssParserWrapper = FakeLinklessRssParserWrapper()
    private val feedFetcherRepository: FeedFetcherRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() + module {
            single<RssParserWrapper> { fakeRssParserWrapper }
        }

    @Test
    fun `a logo that cannot be made absolute settles instead of flipping every refresh`() =
        runTest(testDispatcher) {
            // <image>logo.png</image> and no channel link: nothing to resolve the path against.
            insertSource(logoUrl = RELATIVE_LOGO)

            fetch()
            assertNull(storedLogoUrl())

            fetch()
            assertNull(storedLogoUrl())
        }

    private suspend fun TestScope.fetch() {
        setupLocalAccount()
        feedFetcherRepository.fetchFeeds(forceRefresh = true)
        advanceUntilIdle()
    }

    // Not a @BeforeTest: the native runner does not order it after the base class's own.
    private fun setupLocalAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.LOCAL)
    }

    private suspend fun insertSource(logoUrl: String?) {
        databaseHelper.insertFeedSource(listOf(feedSource(logoUrl).toParsedFeedSource()))
    }

    private suspend fun storedLogoUrl(): String? =
        databaseHelper.getFeedSources().single { it.id == SOURCE_ID }.logoUrl

    private fun feedSource(logoUrl: String?): FeedSource = FeedSource(
        id = SOURCE_ID,
        url = FEED_URL,
        title = "Linkless Feed",
        category = null,
        lastSyncTimestamp = null,
        logoUrl = logoUrl,
        websiteUrl = null,
        fetchFailed = false,
        articleOpenMode = ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
    )

    private companion object {
        const val SOURCE_ID = "linkless-source"
        const val FEED_URL = "https://example.com/linkless/rss.xml"
        const val RELATIVE_LOGO = "logo.png"
    }
}

private class FakeLinklessRssParserWrapper : RssParserWrapper {
    override suspend fun getRssChannel(url: String, allowBrowserTier: Boolean): RssChannel =
        RssChannelGenerator.rssChannel(
            title = "Linkless Feed",
            link = null,
            image = RssImage(title = null, url = "logo.png", link = null, description = null),
            items = emptyList(),
        )
}
