package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.next
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
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
            awaitItem() shouldBe emptyList()
            viewModel.onAddWord("test")
            awaitItem() shouldBe listOf("test")
        }
    }

    @Test
    fun `onAddWord trims whitespace from word`() = runTest {
        viewModel.wordsState.test {
            awaitItem() shouldBe emptyList()
            viewModel.onAddWord("  trimmed  ")
            awaitItem() shouldBe listOf("trimmed")
        }
    }

    @Test
    fun `onAddWord triggers getFeeds and blocks matching feed items`() = runTest {
        val blockedWord = "blockedkeyword"
        populateDatabaseWithTitle("Article with $blockedWord in title")

        feedStateRepository.feedState.test {
            awaitItem() shouldBe emptyList()

            feedStateRepository.getFeeds()
            val feedsBeforeBlock = awaitItem()
            feedsBeforeBlock.size shouldBe 1

            viewModel.onAddWord(blockedWord)
            val feedsAfterBlock = awaitItem()
            feedsAfterBlock.size shouldBe 0
        }
    }

    @Test
    fun `onRemoveWord removes word from state`() = runTest {
        viewModel.wordsState.test {
            awaitItem() shouldBe emptyList()
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
            awaitItem() shouldBe emptyList()

            feedStateRepository.getFeeds()
            val feedsBeforeBlock = awaitItem()
            feedsBeforeBlock.size shouldBe 1

            viewModel.onAddWord(blockedWord)
            val feedsWhileBlocked = awaitItem()
            feedsWhileBlocked.size shouldBe 0

            viewModel.onRemoveWord(blockedWord)
            val feedsAfterUnblock = awaitItem()
            feedsAfterUnblock.size shouldBe 1
        }
    }

    private suspend fun populateDatabaseWithTitle(title: String) {
        val feedItem = FeedItemGenerator.unreadFeedItemArb().next().copy(title = title)
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
