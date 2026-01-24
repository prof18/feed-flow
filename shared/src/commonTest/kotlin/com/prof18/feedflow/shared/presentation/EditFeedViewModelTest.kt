package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedSourceSettings
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.toParsedFeedSource
import io.kotest.property.arbitrary.next
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
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
            linkPreference = LinkOpeningPreference.INTERNAL_BROWSER,
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
                linkOpeningPreference = feedSource.linkOpeningPreference,
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
        viewModel.updateLinkOpeningPreference(LinkOpeningPreference.PREFERRED_BROWSER)
        viewModel.updateIsHiddenFromTimeline(true)
        viewModel.updateIsPinned(true)
        viewModel.updateIsNotificationEnabled(true)
        advanceUntilIdle()

        viewModel.feedEditedState.test {
            viewModel.editFeed()
            assertEquals(FeedEditedState.Loading, awaitItem())
            assertEquals(FeedEditedState.FeedEdited("New Name"), awaitItem())
        }

        val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertNotNull(updatedFeedSource)
        assertEquals("New Name", updatedFeedSource.title)
        assertEquals(LinkOpeningPreference.PREFERRED_BROWSER, updatedFeedSource.linkOpeningPreference)
        assertEquals(true, updatedFeedSource.isHiddenFromTimeline)
        assertEquals(true, updatedFeedSource.isPinned)
        assertEquals(true, updatedFeedSource.isNotificationEnabled)
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
        linkPreference: LinkOpeningPreference = LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline: Boolean = false,
        isPinned: Boolean = false,
        isNotificationEnabled: Boolean = false,
    ) = FeedSourceGenerator.feedSourceArb.next().copy(
        id = id,
        url = "https://example.com/$id/rss.xml",
        title = title,
        category = category,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = null,
        fetchFailed = false,
        linkOpeningPreference = linkPreference,
        isHiddenFromTimeline = isHiddenFromTimeline,
        isPinned = isPinned,
        isNotificationEnabled = isNotificationEnabled,
    )
}
