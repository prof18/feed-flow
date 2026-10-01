package com.prof18.feedflow.shared.domain.feed

import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.database.CloudArticleFlag
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseHelperRelativeItemsFilterTest : KoinTestBase() {

    private val databaseHelper: DatabaseHelper by inject()
    private val sqlDriver: SqlDriver by inject()
    private val feedActionsRepository: FeedActionsRepository by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository by inject()

    @Test
    fun `range selection and updates respect every filter including cloud pending flags`() = runTest(testDispatcher) {
        val cases = mapOf(
            FeedFilter.Bookmarks to setOf("target", "bookmarked", "hidden"),
            FeedFilter.Timeline to setOf("new", "target", "bookmarked", "sibling", "uncategorized", "other", "old"),
            FeedFilter.Read to emptySet(),
            FeedFilter.Source(selectedSource) to setOf("new", "target", "bookmarked", "old"),
            FeedFilter.Category(category) to setOf("new", "target", "bookmarked", "sibling", "old"),
            FeedFilter.Uncategorized to setOf("uncategorized"),
        )
        for ((filter, expectedIds) in cases) {
            for (newer in listOf(true, false)) {
                seedItems()
                databaseHelper.ensureCloudSyncState(SESSION_ID)
                val target = if (newer) "old" else "new"
                val selectedIds = if (newer) {
                    databaseHelper.getNewerItems(target, filter)
                } else {
                    databaseHelper.getOlderItems(target, filter)
                }
                val context = "$filter, newer=$newer"
                assertEquals(expectedIds, selectedIds.toSet(), context)

                if (newer) {
                    databaseHelper.markAllNewerAsRead(target, filter, cloudSessionId = SESSION_ID)
                } else {
                    databaseHelper.markAllOlderAsRead(target, filter, cloudSessionId = SESSION_ID)
                }

                assertEquals(expectedIds + "read", readItemIds(), context)
                val pending = databaseHelper.getCloudPendingArticleFlags(SESSION_ID)
                assertEquals(expectedIds, pending.map { it.itemId }.toSet(), context)
                pending.forEach {
                    assertEquals(CloudArticleFlag.READ, it.field, context)
                    assertEquals(true, it.value, context)
                }
            }
        }
    }

    @Test
    fun `repository bookmark mark above and below honors both feed orders`() = runTest(testDispatcher) {
        for (order in FeedOrder.entries) {
            for (above in listOf(true, false)) {
                seedItems()
                feedAppearanceSettingsRepository.setFeedOrder(order)
                feedStateRepository.updateFeedFilter(FeedFilter.Bookmarks)
                advanceUntilIdle()

                if (above) {
                    feedActionsRepository.markAllAboveAsRead("target")
                } else {
                    feedActionsRepository.markAllBelowAsRead("target")
                }
                advanceUntilIdle()

                val newer = above == (order == FeedOrder.NEWEST_FIRST)
                val expected = if (newer) {
                    setOf("read", "target")
                } else {
                    setOf("bookmarked", "hidden", "read", "target")
                }
                assertEquals(expected, readItemIds(), "$order, above=$above")
            }
        }
    }

    @Test
    fun `range actions in Read view leave unread articles untouched`() = runTest(testDispatcher) {
        seedItems()
        feedStateRepository.updateFeedFilter(FeedFilter.Read)
        advanceUntilIdle()

        feedActionsRepository.markAllAboveAsRead("read")
        feedActionsRepository.markAllBelowAsRead("read")
        advanceUntilIdle()

        assertEquals(setOf("read"), readItemIds())
    }

    private suspend fun seedItems() {
        databaseHelper.deleteAll()
        listOf(selectedSource, siblingSource, otherSource, uncategorizedSource, hiddenSource).forEach {
            databaseHelper.insertFeedSourceWithCategory(it)
        }
        databaseHelper.insertFeedItems(
            listOf(
                buildFeedItem("new", "New", 50, selectedSource),
                buildFeedItem("target", "Target", 40, selectedSource),
                buildFeedItem("sibling", "Sibling", 35, siblingSource),
                buildFeedItem("bookmarked", "Bookmarked", 30, selectedSource),
                buildFeedItem("hidden", "Hidden", 25, hiddenSource),
                buildFeedItem("blocked", "Blocked", 25, selectedSource),
                buildFeedItem("uncategorized", "Uncategorized", 20, uncategorizedSource),
                buildFeedItem("other", "Other", 15, otherSource),
                buildFeedItem("old", "Old", 10, selectedSource),
                buildFeedItem("read", "Read", 0, selectedSource),
            ),
            lastSyncTimestamp = 0,
        )
        listOf("target", "bookmarked", "hidden", "blocked", "read").forEach {
            databaseHelper.updateBookmarkStatus(FeedItemId(it), isBookmarked = true)
        }
        databaseHelper.updateReadStatus(FeedItemId("read"), isRead = true)
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = hiddenSource.id,
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHidden = true,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )
        sqlDriver.execute(
            identifier = null,
            sql = "UPDATE feed_item SET is_blocked = 1 WHERE url_hash = 'blocked'",
            parameters = 0,
        ).value
    }

    private suspend fun readItemIds(): Set<String> = databaseHelper.getAllFeedItemFlagsForCloud()
        .filter { it.isRead }
        .map { it.id }
        .toSet()

    private companion object {
        const val SESSION_ID = "relative-items-filter-session"
        val category = FeedSourceCategory(id = "selected-category", title = "Selected")
        val otherCategory = FeedSourceCategory(id = "other-category", title = "Other")
        val selectedSource = FeedSourceGenerator.feedSource(id = "selected", category = category)
        val siblingSource = FeedSourceGenerator.feedSource(id = "sibling", category = category)
        val otherSource = FeedSourceGenerator.feedSource(id = "other", category = otherCategory)
        val uncategorizedSource = FeedSourceGenerator.feedSource(id = "uncategorized")
        val hiddenSource = FeedSourceGenerator.feedSource(id = "hidden", category = otherCategory)
    }
}
