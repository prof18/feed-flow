package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlockedWordsViewModelTest : KoinTestBase() {

    private val viewModel: BlockedWordsViewModel by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    @Test
    fun `initial state is empty list`() = runTest {
        val initialState = viewModel.wordsState.value
        assertTrue(initialState.isEmpty())
    }

    @Test
    fun `onAddWord adds word to state`() = runTest {
        viewModel.wordsState.test {
            assertEquals(emptyList(), awaitItem())
            viewModel.onAddWord("test")
            assertEquals(listOf("test"), awaitItem())
        }
    }

    @Test
    fun `onAddWord trims whitespace from word`() = runTest {
        viewModel.wordsState.test {
            assertEquals(emptyList(), awaitItem())
            viewModel.onAddWord("  trimmed  ")
            assertEquals(listOf("trimmed"), awaitItem())
        }
    }

    @Test
    fun `onAddWord triggers getFeeds and blocks matching feed items`() = runTest {
        val blockedWord = "blockedkeyword"
        populateDatabaseWithTitle("Article with $blockedWord in title")

        feedStateRepository.feedState.test {
            assertEquals(emptyList(), awaitItem())

            feedStateRepository.getFeeds()
            val feedsBeforeBlock = awaitItem()
            assertEquals(1, feedsBeforeBlock.size)

            viewModel.onAddWord(blockedWord)
            val feedsAfterBlock = awaitItem()
            assertEquals(0, feedsAfterBlock.size)
        }
    }

    @Test
    fun `onRemoveWord removes word from state`() = runTest {
        viewModel.wordsState.test {
            assertEquals(emptyList(), awaitItem())
            viewModel.onAddWord("word1")
            val afterAdd = awaitItem()
            assertTrue(afterAdd.contains("word1"))
            viewModel.onRemoveWord("word1")
            val afterRemove = awaitItem()
            assertFalse(afterRemove.contains("word1"))
        }
    }

    @Test
    fun `onRemoveWord triggers getFeeds and unblocks matching feed items`() = runTest {
        val blockedWord = "unblockkeyword"
        populateDatabaseWithTitle("Article with $blockedWord in title")

        feedStateRepository.feedState.test {
            assertEquals(emptyList(), awaitItem())

            feedStateRepository.getFeeds()
            val feedsBeforeBlock = awaitItem()
            assertEquals(1, feedsBeforeBlock.size)

            viewModel.onAddWord(blockedWord)
            val feedsWhileBlocked = awaitItem()
            assertEquals(0, feedsWhileBlocked.size)

            viewModel.onRemoveWord(blockedWord)
            val feedsAfterUnblock = awaitItem()
            assertEquals(1, feedsAfterUnblock.size)
        }
    }

    @Test
    fun `add blocked word does not block existing items with matching content`() = runTest {
        val blockedWord = "contentonlykeyword"
        populateDatabaseWithItem(
            id = "existing-content-match",
            title = "Visible article",
            subtitle = "Visible subtitle",
            content = "Article body with $blockedWord",
        )

        databaseHelper.addBlockedWord(blockedWord)

        assertEquals(1, getVisibleFeedCount())
    }

    @Test
    fun `insert feed item does not block matching content`() = runTest {
        val blockedWord = "insertcontentkeyword"
        databaseHelper.addBlockedWord(blockedWord)

        populateDatabaseWithItem(
            id = "new-content-match",
            title = "Visible article",
            subtitle = "Visible subtitle",
            content = "Article body with $blockedWord",
        )

        assertEquals(1, getVisibleFeedCount())
    }

    private suspend fun populateDatabaseWithTitle(title: String) {
        populateDatabaseWithItem(title = title)
    }

    private suspend fun populateDatabaseWithItem(
        id: String = "feed-item-id",
        title: String? = "Feed item title",
        subtitle: String? = "Feed item subtitle",
        content: String? = "Feed item content",
    ) {
        val feedItem = FeedItemGenerator.unreadFeedItem(
            id = id,
            title = title,
            subtitle = subtitle,
            content = content,
        )
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
    }

    private suspend fun getVisibleFeedCount(): Int =
        databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 20,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        ).size
}
