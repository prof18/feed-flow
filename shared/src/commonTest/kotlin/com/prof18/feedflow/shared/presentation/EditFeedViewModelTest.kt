package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedSourceSettings
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.YouTubeOptionsState
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EditFeedViewModelTest : KoinTestBase() {

    private val viewModel: EditFeedViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val networkSettings: NetworkSettings by inject()

    @Test
    fun `loadFeedToEdit populates state and selects category`() = runTest {
        val category = FeedSourceCategory(id = "category-1", title = "Tech")
        databaseHelper.insertCategories(listOf(category))

        val feedSource = createFeedSource(
            id = "source-1",
            title = "Tech Feed",
            category = category,
            articleOpenMode = ArticleOpenMode.INTERNAL_BROWSER,
            isHiddenFromTimeline = true,
            isPinned = true,
            isNotificationEnabled = true,
        )

        viewModel.loadFeedToEdit(feedSource)
        advanceUntilIdle()

        assertEquals(feedSource.url, viewModel.feedUrlState.value)
        assertEquals(feedSource.title, viewModel.feedNameState.value)
        assertEquals(
            FeedSourceSettings(
                articleOpenMode = feedSource.articleOpenMode,
                isHiddenFromTimeline = feedSource.isHiddenFromTimeline,
                isPinned = feedSource.isPinned,
                isNotificationEnabled = feedSource.isNotificationEnabled,
            ),
            viewModel.feedSourceSettingsState.value,
        )

        val categories = viewModel.categoriesState.value.categories
        val selectedCategory = categories.first { it.id == category.id }
        assertTrue(selectedCategory.isSelected)
        assertTrue(categories.first { it.name == null }.isSelected.not())
    }

    @Test
    fun `loadFeedToEdit uses the latest persisted feed settings`() = runTest {
        val staleFeedSource = createFeedSource(
            id = "source-stale",
            title = "Stale Feed",
            articleOpenMode = ArticleOpenMode.FEED_CONTENT,
        )
        val persistedFeedSource = staleFeedSource.copy(
            title = "Updated Feed",
            articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
            isHiddenFromTimeline = true,
            isPinned = true,
            isNotificationEnabled = true,
            isHideImagesEnabled = true,
        )
        insertFeedSource(persistedFeedSource)
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = persistedFeedSource.id,
            articleOpenMode = persistedFeedSource.articleOpenMode,
            isHidden = persistedFeedSource.isHiddenFromTimeline,
            isPinned = persistedFeedSource.isPinned,
            isNotificationEnabled = persistedFeedSource.isNotificationEnabled,
            isHideImagesEnabled = persistedFeedSource.isHideImagesEnabled,
        )

        viewModel.loadFeedToEdit(staleFeedSource)
        advanceUntilIdle()

        assertEquals(persistedFeedSource.title, viewModel.feedNameState.value)
        assertEquals(
            FeedSourceSettings(
                articleOpenMode = persistedFeedSource.articleOpenMode,
                isHiddenFromTimeline = persistedFeedSource.isHiddenFromTimeline,
                isPinned = persistedFeedSource.isPinned,
                isNotificationEnabled = persistedFeedSource.isNotificationEnabled,
                isHideImagesEnabled = persistedFeedSource.isHideImagesEnabled,
            ),
            viewModel.feedSourceSettingsState.value,
        )
    }

    @Test
    fun `loadFeedToEdit loads keep unread on scroll`() = runTest {
        val feedSource = createFeedSource(
            id = "source-keep-unread",
            title = "Newsletter",
        )
        insertFeedSource(feedSource)
        databaseHelper.setKeepUnreadOnScroll(feedSource.id, true)

        viewModel.loadFeedToEdit(feedSource)
        advanceUntilIdle()

        assertTrue(viewModel.feedSourceSettingsState.value.isKeepUnreadOnScrollEnabled)
    }

    @Test
    fun `editFeed saves keep unread on scroll`() = runTest {
        val feedSource = createFeedSource(
            id = "source-save-keep-unread",
            title = "Newsletter",
        )
        insertFeedSource(feedSource)

        viewModel.loadFeedToEdit(feedSource)
        advanceUntilIdle()

        viewModel.updateIsKeepUnreadOnScrollEnabled(true)
        advanceUntilIdle()

        viewModel.feedEditedState.test {
            viewModel.editFeed()
            assertEquals(FeedEditedState.Loading, awaitItem())
            assertEquals(FeedEditedState.FeedEdited("Newsletter"), awaitItem())
        }

        assertTrue(databaseHelper.isKeepUnreadOnScrollEnabled(feedSource.id))
    }

    @Test
    fun `canEditUrl returns true for local account`() = runTest {
        assertTrue(viewModel.canEditUrl())
    }

    @Test
    fun `canEditUrl returns false for restricted accounts`() = runTest {
        val accounts = listOf(
            SyncAccounts.FRESH_RSS,
            SyncAccounts.MINIFLUX,
            SyncAccounts.BAZQUX,
            SyncAccounts.FEEDBIN,
        )

        accounts.forEach { account ->
            networkSettings.setSyncAccountType(account)
            networkSettings.setSyncPwd("token")
            networkSettings.setSyncUrl("https://example.com")

            assertTrue(viewModel.canEditUrl().not())
        }
    }

    @Test
    fun `updateFeedUrlTextFieldValue emits idle and updates state`() = runTest {
        viewModel.feedEditedState.test {
            viewModel.updateFeedUrlTextFieldValue("https://example.com/rss.xml")
            assertEquals("https://example.com/rss.xml", viewModel.feedUrlState.value)
            assertEquals(FeedEditedState.Idle, awaitItem())
        }
    }

    @Test
    fun `channel feed URL shows Hide Shorts off`() = runTest {
        viewModel.updateFeedUrlTextFieldValue(CHANNEL_FEED_URL)

        assertEquals(
            YouTubeOptionsState(showHideShortsToggle = true, hideShorts = false),
            viewModel.youTubeOptionsState.value,
        )
    }

    @Test
    fun `no-Shorts feed URL shows Hide Shorts on`() = runTest {
        viewModel.updateFeedUrlTextFieldValue(NO_SHORTS_FEED_URL)

        assertEquals(
            YouTubeOptionsState(showHideShortsToggle = true, hideShorts = true),
            viewModel.youTubeOptionsState.value,
        )
    }

    @Test
    fun `non YouTube URL hides Hide Shorts`() = runTest {
        viewModel.updateFeedUrlTextFieldValue("https://example.com/rss.xml")

        assertEquals(YouTubeOptionsState(), viewModel.youTubeOptionsState.value)
    }

    @Test
    fun `toggling Hide Shorts rewrites the feed URL`() = runTest {
        viewModel.updateFeedUrlTextFieldValue(CHANNEL_FEED_URL)

        viewModel.updateHideShortsStatus(true)
        assertEquals(NO_SHORTS_FEED_URL, viewModel.feedUrlState.value)
        assertTrue(viewModel.youTubeOptionsState.value.hideShorts)

        viewModel.updateHideShortsStatus(false)
        assertEquals(CHANNEL_FEED_URL, viewModel.feedUrlState.value)
        assertFalse(viewModel.youTubeOptionsState.value.hideShorts)
    }

    @Test
    fun `restricted account hides Hide Shorts`() = runTest {
        networkSettings.setSyncAccountType(SyncAccounts.FRESH_RSS)
        networkSettings.setSyncPwd("token")
        networkSettings.setSyncUrl("https://example.com")

        viewModel.updateFeedUrlTextFieldValue(CHANNEL_FEED_URL)

        assertEquals(YouTubeOptionsState(), viewModel.youTubeOptionsState.value)
    }

    @Test
    fun `updateFeedNameTextFieldValue emits idle and updates state`() = runTest {
        viewModel.feedEditedState.test {
            viewModel.updateFeedNameTextFieldValue("Updated Name")
            assertEquals("Updated Name", viewModel.feedNameState.value)
            assertEquals(FeedEditedState.Idle, awaitItem())
        }
    }

    @Test
    fun `editFeed emits invalid title link when required fields are missing`() = runTest {
        viewModel.feedEditedState.test {
            viewModel.editFeed()
            assertEquals(FeedEditedState.Error.InvalidTitleLink, awaitItem())
        }
    }

    @Test
    fun `editFeed updates feed source and preferences`() = runTest {
        val feedSource = createFeedSource(
            id = "source-2",
            title = "Old Name",
        )
        insertFeedSource(feedSource)

        viewModel.loadFeedToEdit(feedSource)
        advanceUntilIdle()

        viewModel.updateFeedNameTextFieldValue("New Name")
        viewModel.updateArticleOpenMode(ArticleOpenMode.PREFERRED_BROWSER)
        viewModel.updateIsHiddenFromTimeline(true)
        viewModel.updateIsPinned(true)
        viewModel.updateIsNotificationEnabled(true)
        viewModel.updateIsHideImagesEnabled(true)
        advanceUntilIdle()

        viewModel.feedEditedState.test {
            viewModel.editFeed()
            assertEquals(FeedEditedState.Loading, awaitItem())
            assertEquals(FeedEditedState.FeedEdited("New Name"), awaitItem())
        }

        val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertNotNull(updatedFeedSource)
        assertEquals("New Name", updatedFeedSource.title)
        assertEquals(ArticleOpenMode.PREFERRED_BROWSER, updatedFeedSource.articleOpenMode)
        assertEquals(true, updatedFeedSource.isHiddenFromTimeline)
        assertEquals(true, updatedFeedSource.isPinned)
        assertEquals(true, updatedFeedSource.isNotificationEnabled)
        assertEquals(true, updatedFeedSource.isHideImagesEnabled)
    }

    @Test
    fun `deleteFeed removes feed and emits deletion state`() = runTest {
        val feedSource = createFeedSource(
            id = "source-3",
            title = "Remove Me",
        )
        insertFeedSource(feedSource)

        viewModel.loadFeedToEdit(feedSource)
        advanceUntilIdle()

        viewModel.feedDeletedState.test {
            viewModel.deleteFeed()
            assertEquals(Unit, awaitItem())
        }

        val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertEquals(null, updatedFeedSource)
    }

    private suspend fun insertFeedSource(feedSource: FeedSource) {
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
    }

    private fun createFeedSource(
        id: String,
        title: String,
        category: FeedSourceCategory? = null,
        articleOpenMode: ArticleOpenMode = ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline: Boolean = false,
        isPinned: Boolean = false,
        isNotificationEnabled: Boolean = false,
    ) = FeedSourceGenerator.feedSource(
        id = id,
        url = "https://example.com/$id/rss.xml",
        title = title,
        category = category,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = null,
        fetchFailed = false,
        articleOpenMode = articleOpenMode,
        isHiddenFromTimeline = isHiddenFromTimeline,
        isPinned = isPinned,
        isNotificationEnabled = isNotificationEnabled,
    )

    private companion object {
        const val CHANNEL_FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=UCsBjURrPoezykLs9EqgamOA"
        const val NO_SHORTS_FEED_URL =
            "https://www.youtube.com/feeds/videos.xml?playlist_id=UULFsBjURrPoezykLs9EqgamOA"
    }
}
