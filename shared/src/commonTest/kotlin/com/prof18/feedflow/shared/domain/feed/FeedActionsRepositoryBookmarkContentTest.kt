package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FeedActionsRepositoryBookmarkContentTest : KoinTestBase() {

    private val feedActionsRepository: FeedActionsRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val feedItemContentFileHandler: FeedItemContentFileHandler by inject()
    private var parseCount = 0

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<FeedItemParserWorker> {
            object : FeedItemParserWorker {
                override suspend fun parse(feedItemId: String, url: String, imageUrl: String?): ParsingResult {
                    parseCount++
                    return ParsingResult.Success(
                        htmlContent = "Parsed",
                        title = "T",
                        siteName = "S",
                    )
                }
            }
        }
    }

    @Test
    fun `bookmarking saves parsed content when save on open is enabled`() = runTest(testDispatcher) {
        settingsRepository.setSaveItemContentOnOpen(true)
        insertFeedItem()

        feedActionsRepository.updateBookmarkStatus(FeedItemId(ITEM_ID), isBookmarked = true)

        assertEquals(1, parseCount)
        assertEquals("Parsed", feedItemContentFileHandler.loadFeedItemContent(ITEM_ID))
    }

    @Test
    fun `bookmarking does not parse when save on open is disabled`() = runTest(testDispatcher) {
        settingsRepository.setSaveItemContentOnOpen(false)
        insertFeedItem()

        feedActionsRepository.updateBookmarkStatus(FeedItemId(ITEM_ID), isBookmarked = true)

        assertEquals(0, parseCount)
        assertFalse(feedItemContentFileHandler.isContentAvailable(ITEM_ID))
    }

    @Test
    fun `unbookmarking does not parse`() = runTest(testDispatcher) {
        settingsRepository.setSaveItemContentOnOpen(true)
        insertFeedItem()

        feedActionsRepository.updateBookmarkStatus(FeedItemId(ITEM_ID), isBookmarked = false)

        assertEquals(0, parseCount)
        assertFalse(feedItemContentFileHandler.isContentAvailable(ITEM_ID))
    }

    private suspend fun insertFeedItem() {
        val feedSource = FeedSource(
            id = "source-1",
            url = "https://example.com/source-1/feed.xml",
            title = "Source",
            category = null,
            lastSyncTimestamp = null,
            logoUrl = null,
            websiteUrl = "https://example.com/source-1",
            fetchFailed = false,
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHiddenFromTimeline = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )
        databaseHelper.insertFeedSourceWithCategory(feedSource)
        databaseHelper.insertFeedItems(
            listOf(buildFeedItem(ITEM_ID, "Article", 10_000L, feedSource)),
            lastSyncTimestamp = 0,
        )
    }

    private companion object {
        const val ITEM_ID = "item-1"
    }
}
