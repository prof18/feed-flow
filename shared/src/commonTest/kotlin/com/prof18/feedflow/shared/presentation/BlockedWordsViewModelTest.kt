package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
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

    private suspend fun populateDatabaseWithTitle(title: String) {
        val feedItem = FeedItemGenerator.unreadFeedItem(title = title)
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
}
