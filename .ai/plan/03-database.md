# Phase 2: Database Tests

**Goal**: Test all database operations using real DatabaseHelper with in-memory SQLite.

Uses the in-memory driver infrastructure already in place.

---

## Task 2.1: DatabaseHelper CRUD Tests

**File**: `database/src/commonTest/kotlin/.../DatabaseHelperTest.kt`

```kotlin
package com.prof18.feedflow.database

import com.prof18.feedflow.shared.test.DatabaseTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DatabaseHelperTest : DatabaseTestBase() {

    @Test
    fun `should insert and retrieve feed sources`() = runTest {
        checkAll(10, FeedSourceGenerator.feedSourceArb) { feedSource ->
            database.insertFeedSource(feedSource)
            val retrieved = database.getFeedSourceById(feedSource.id)

            retrieved.shouldNotBeNull()
            retrieved.id shouldBe feedSource.id
            retrieved.title shouldBe feedSource.title
            retrieved.url shouldBe feedSource.url
        }
    }

    @Test
    fun `should update existing feed source`() = runTest {
        val original = FeedSourceGenerator.feedSourceArb.sample().value
        database.insertFeedSource(original)

        val updated = original.copy(title = "Updated Title")
        database.updateFeedSource(updated)

        val retrieved = database.getFeedSourceById(original.id)
        retrieved?.title shouldBe "Updated Title"
    }

    @Test
    fun `should delete feed source`() = runTest {
        val feedSource = FeedSourceGenerator.feedSourceArb.sample().value
        database.insertFeedSource(feedSource)

        database.deleteFeedSource(feedSource.id)

        val retrieved = database.getFeedSourceById(feedSource.id)
        retrieved.shouldBeNull()
    }

    @Test
    fun `should retrieve all feed sources`() = runTest {
        val sources = List(5) { FeedSourceGenerator.feedSourceArb.sample().value }
        sources.forEach { database.insertFeedSource(it) }

        val allSources = database.getAllFeedSources()
        allSources shouldHaveSize 5
    }
}
```

**Acceptance Criteria**:
- ✅ Insert/update/delete operations work
- ✅ Retrieval methods tested
- ✅ Property-based tests with random data

---

## Task 2.2: Pagination Tests

**File**: `database/src/commonTest/kotlin/.../DatabaseHelperPaginationTest.kt`

```kotlin
package com.prof18.feedflow.database

import com.prof18.feedflow.shared.test.DatabaseTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DatabaseHelperPaginationTest : DatabaseTestBase() {

    @Test
    fun `should paginate feed items with 40-item pages`() = runTest {
        // Insert 100 feed items
        val items = List(100) { FeedItemGenerator.feedItemArb.sample().value }
        items.forEach { database.insertFeedItem(it) }

        // Query first page (40 items)
        val page1 = database.getFeedItems(offset = 0, limit = 40)
        page1.size shouldBe 40

        // Query second page
        val page2 = database.getFeedItems(offset = 40, limit = 40)
        page2.size shouldBe 40

        // Query third page (20 items remaining)
        val page3 = database.getFeedItems(offset = 80, limit = 40)
        page3.size shouldBe 20

        // No overlap between pages
        val allIds = page1.map { it.id } + page2.map { it.id } + page3.map { it.id }
        allIds.distinct().size shouldBe 100
    }

    @Test
    fun `should handle empty results`() = runTest {
        val results = database.getFeedItems(offset = 0, limit = 40)
        results.shouldBeEmpty()
    }

    @Test
    fun `should handle offset beyond total items`() = runTest {
        val items = List(10) { FeedItemGenerator.feedItemArb.sample().value }
        items.forEach { database.insertFeedItem(it) }

        val results = database.getFeedItems(offset = 100, limit = 40)
        results.shouldBeEmpty()
    }
}
```

**Acceptance Criteria**:
- ✅ 40-item pagination works
- ✅ No overlap between pages
- ✅ Edge cases handled (empty, out of range)

---

## Task 2.3: Filter Tests

**File**: `database/src/commonTest/kotlin/.../DatabaseHelperFilterTest.kt`

```kotlin
package com.prof18.feedflow.database

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.shared.test.DatabaseTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DatabaseHelperFilterTest : DatabaseTestBase() {

    @Test
    fun `should filter by read status`() = runTest {
        val readItems = List(5) {
            FeedItemGenerator.feedItemArb.sample().value.copy(isRead = true)
        }
        val unreadItems = List(5) {
            FeedItemGenerator.feedItemArb.sample().value.copy(isRead = false)
        }

        (readItems + unreadItems).forEach { database.insertFeedItem(it) }

        val readResults = database.getFeedItems(filter = FeedFilter.OnlyRead)
        readResults.size shouldBe 5
        readResults.all { it.isRead } shouldBe true

        val unreadResults = database.getFeedItems(filter = FeedFilter.OnlyUnread)
        unreadResults.size shouldBe 5
        unreadResults.all { !it.isRead } shouldBe true
    }

    @Test
    fun `should filter by bookmarked status`() = runTest {
        val bookmarkedItems = List(3) {
            FeedItemGenerator.feedItemArb.sample().value.copy(isBookmarked = true)
        }
        val normalItems = List(7) {
            FeedItemGenerator.feedItemArb.sample().value.copy(isBookmarked = false)
        }

        (bookmarkedItems + normalItems).forEach { database.insertFeedItem(it) }

        val results = database.getFeedItems(filter = FeedFilter.OnlyBookmarked)
        results.size shouldBe 3
        results.all { it.isBookmarked } shouldBe true
    }

    @Test
    fun `should filter by category`() = runTest {
        val category = "Tech"
        val source = FeedSourceGenerator.feedSourceArb.sample().value.copy(category = category)
        database.insertFeedSource(source)

        val itemsInCategory = List(5) {
            FeedItemGenerator.feedItemArb.sample().value.copy(feedSourceId = source.id)
        }
        itemsInCategory.forEach { database.insertFeedItem(it) }

        val results = database.getFeedItems(filter = FeedFilter.ByCategory(category))
        results.size shouldBe 5
    }

    @Test
    fun `should filter by feed source`() = runTest {
        val source1 = FeedSourceGenerator.feedSourceArb.sample().value
        val source2 = FeedSourceGenerator.feedSourceArb.sample().value
        database.insertFeedSource(source1)
        database.insertFeedSource(source2)

        val itemsSource1 = List(3) {
            FeedItemGenerator.feedItemArb.sample().value.copy(feedSourceId = source1.id)
        }
        val itemsSource2 = List(2) {
            FeedItemGenerator.feedItemArb.sample().value.copy(feedSourceId = source2.id)
        }

        (itemsSource1 + itemsSource2).forEach { database.insertFeedItem(it) }

        val results = database.getFeedItems(filter = FeedFilter.ByFeedSource(source1.id))
        results.size shouldBe 3
    }

    @Test
    fun `should filter uncategorized items`() = runTest {
        val categorized = FeedSourceGenerator.feedSourceArb.sample().value.copy(category = "Tech")
        val uncategorized = FeedSourceGenerator.feedSourceArb.sample().value.copy(category = null)

        database.insertFeedSource(categorized)
        database.insertFeedSource(uncategorized)

        val categorizedItems = List(3) {
            FeedItemGenerator.feedItemArb.sample().value.copy(feedSourceId = categorized.id)
        }
        val uncategorizedItems = List(4) {
            FeedItemGenerator.feedItemArb.sample().value.copy(feedSourceId = uncategorized.id)
        }

        (categorizedItems + uncategorizedItems).forEach { database.insertFeedItem(it) }

        val results = database.getFeedItems(filter = FeedFilter.Uncategorized)
        results.size shouldBe 4
    }
}
```

**Acceptance Criteria**:
- ✅ Read/unread filtering works
- ✅ Bookmarked filtering works
- ✅ Category filtering works
- ✅ Feed source filtering works
- ✅ Uncategorized filtering works

---

## Task 2.4: Mark Read Operations Tests

**File**: `database/src/commonTest/kotlin/.../DatabaseHelperMarkReadTest.kt`

```kotlin
package com.prof18.feedflow.database

import com.prof18.feedflow.shared.test.DatabaseTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DatabaseHelperMarkReadTest : DatabaseTestBase() {

    @Test
    fun `should mark single item as read`() = runTest {
        val item = FeedItemGenerator.unreadFeedItemArb().sample().value
        database.insertFeedItem(item)

        database.markAsRead(item.id, isRead = true)

        val updated = database.getFeedItemById(item.id)
        updated?.isRead shouldBe true
    }

    @Test
    fun `should mark multiple items as read`() = runTest {
        val items = List(5) { FeedItemGenerator.unreadFeedItemArb().sample().value }
        items.forEach { database.insertFeedItem(it) }

        database.markAsRead(items.map { it.id }, isRead = true)

        items.forEach { item ->
            val updated = database.getFeedItemById(item.id)
            updated?.isRead shouldBe true
        }
    }

    @Test
    fun `should mark all items above as read`() = runTest {
        val items = List(10) { index ->
            FeedItemGenerator.unreadFeedItemArb().sample().value.copy(
                pubDate = System.currentTimeMillis() - (index * 1000L)  // Descending order
            )
        }
        items.forEach { database.insertFeedItem(it) }

        // Mark all items above index 5 as read
        val pivotItem = items[5]
        database.markAllAboveAsRead(pivotItem.id)

        // Items 0-4 should be read, 5-9 should be unread
        items.take(5).forEach { item ->
            val updated = database.getFeedItemById(item.id)
            updated?.isRead shouldBe true
        }
        items.drop(5).forEach { item ->
            val updated = database.getFeedItemById(item.id)
            updated?.isRead shouldBe false
        }
    }

    @Test
    fun `should mark all items below as read`() = runTest {
        val items = List(10) { index ->
            FeedItemGenerator.unreadFeedItemArb().sample().value.copy(
                pubDate = System.currentTimeMillis() - (index * 1000L)
            )
        }
        items.forEach { database.insertFeedItem(it) }

        // Mark all items below index 4 as read
        val pivotItem = items[4]
        database.markAllBelowAsRead(pivotItem.id)

        // Items 0-4 should be unread, 5-9 should be read
        items.take(5).forEach { item ->
            val updated = database.getFeedItemById(item.id)
            updated?.isRead shouldBe false
        }
        items.drop(5).forEach { item ->
            val updated = database.getFeedItemById(item.id)
            updated?.isRead shouldBe true
        }
    }
}
```

**Acceptance Criteria**:
- ✅ Single item mark as read works
- ✅ Multiple items mark as read works
- ✅ Mark above/below operations work correctly

---

## Task 2.5: Cleanup Operations Tests

**File**: `database/src/commonTest/kotlin/.../DatabaseHelperCleanupTest.kt`

```kotlin
package com.prof18.feedflow.database

import com.prof18.feedflow.shared.test.DatabaseTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DatabaseHelperCleanupTest : DatabaseTestBase() {

    @Test
    fun `should delete old feed items respecting threshold`() = runTest {
        val now = System.currentTimeMillis()
        val oldItems = List(5) {
            FeedItemGenerator.feedItemArb.sample().value.copy(
                pubDate = now - 31L * 24 * 60 * 60 * 1000  // 31 days ago
            )
        }
        val recentItems = List(5) {
            FeedItemGenerator.feedItemArb.sample().value.copy(
                pubDate = now - 5L * 24 * 60 * 60 * 1000  // 5 days ago
            )
        }

        (oldItems + recentItems).forEach { database.insertFeedItem(it) }

        // Delete items older than 30 days
        database.deleteOldFeedItems(thresholdDays = 30)

        val remaining = database.getAllFeedItems()
        remaining.size shouldBe 5  // Only recent items remain
    }

    @Test
    fun `should add deleted items to deleted items table`() = runTest {
        val item = FeedItemGenerator.feedItemArb.sample().value
        database.insertFeedItem(item)

        database.deleteFeedItem(item.id)

        val deletedItems = database.getDeletedItemIds()
        deletedItems shouldContain item.id
    }

    @Test
    fun `should cleanup old deleted items after 6 months`() = runTest {
        val now = System.currentTimeMillis()
        val oldDeletedId = "old-deleted"
        val recentDeletedId = "recent-deleted"

        database.insertDeletedItem(oldDeletedId, deletedAt = now - 200L * 24 * 60 * 60 * 1000)  // 200 days ago
        database.insertDeletedItem(recentDeletedId, deletedAt = now - 30L * 24 * 60 * 60 * 1000)  // 30 days ago

        database.cleanupOldDeletedItems(thresholdDays = 180)  // 6 months

        val remaining = database.getDeletedItemIds()
        remaining shouldContain recentDeletedId
        remaining shouldNotContain oldDeletedId
    }

    @Test
    fun `should not delete bookmarked items`() = runTest {
        val now = System.currentTimeMillis()
        val oldBookmarked = FeedItemGenerator.feedItemArb.sample().value.copy(
            pubDate = now - 100L * 24 * 60 * 60 * 1000,  // 100 days ago
            isBookmarked = true
        )
        val oldNormal = FeedItemGenerator.feedItemArb.sample().value.copy(
            pubDate = now - 100L * 24 * 60 * 60 * 1000,
            isBookmarked = false
        )

        database.insertFeedItem(oldBookmarked)
        database.insertFeedItem(oldNormal)

        database.deleteOldFeedItems(thresholdDays = 30)

        val bookmarkedStillExists = database.getFeedItemById(oldBookmarked.id)
        bookmarkedStillExists.shouldNotBeNull()

        val normalDeleted = database.getFeedItemById(oldNormal.id)
        normalDeleted.shouldBeNull()
    }
}
```

**Acceptance Criteria**:
- ✅ Old items deleted correctly
- ✅ Deleted items tracked in deleted_items table
- ✅ Old deleted items cleaned up after 6 months
- ✅ Bookmarked items never deleted

---

## Task 2.6: Search Tests

**File**: `database/src/commonTest/kotlin/.../DatabaseHelperSearchTest.kt`

```kotlin
package com.prof18.feedflow.database

import com.prof18.feedflow.shared.test.DatabaseTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DatabaseHelperSearchTest : DatabaseTestBase() {

    @Test
    fun `should find items by title`() = runTest {
        val items = listOf(
            FeedItemGenerator.feedItemArb.sample().value.copy(title = "Kotlin News"),
            FeedItemGenerator.feedItemArb.sample().value.copy(title = "Java Update"),
            FeedItemGenerator.feedItemArb.sample().value.copy(title = "Kotlin 2.0 Released"),
        )
        items.forEach { database.insertFeedItem(it) }

        val results = database.searchFeedItems("Kotlin")
        results.size shouldBe 2
        results.map { it.title } shouldContain "Kotlin News"
        results.map { it.title } shouldContain "Kotlin 2.0 Released"
    }

    @Test
    fun `should find items by subtitle`() = runTest {
        val item = FeedItemGenerator.feedItemArb.sample().value.copy(
            title = "Article",
            subtitle = "This is about Android development"
        )
        database.insertFeedItem(item)

        val results = database.searchFeedItems("Android")
        results.size shouldBe 1
    }

    @Test
    fun `should be case insensitive`() = runTest {
        val item = FeedItemGenerator.feedItemArb.sample().value.copy(title = "Kotlin News")
        database.insertFeedItem(item)

        val results = database.searchFeedItems("kotlin")
        results.size shouldBe 1
    }

    @Test
    fun `should respect filters during search`() = runTest {
        val readItem = FeedItemGenerator.feedItemArb.sample().value.copy(
            title = "Read Kotlin Article",
            isRead = true
        )
        val unreadItem = FeedItemGenerator.feedItemArb.sample().value.copy(
            title = "Unread Kotlin Article",
            isRead = false
        )
        database.insertFeedItem(readItem)
        database.insertFeedItem(unreadItem)

        val results = database.searchFeedItems("Kotlin", filter = FeedFilter.OnlyUnread)
        results.size shouldBe 1
        results[0].title shouldBe "Unread Kotlin Article"
    }
}
```

**Acceptance Criteria**:
- ✅ Search finds items in title and subtitle
- ✅ Search is case-insensitive
- ✅ Filters applied during search

---

## Task 2.7: Sync Metadata Tests

**File**: `feedSync/database/src/commonTest/kotlin/.../SyncedDatabaseHelperTest.kt`

```kotlin
package com.prof18.feedflow.feedsync.database

import com.prof18.feedflow.shared.test.DatabaseTestBase
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SyncedDatabaseHelperTest : DatabaseTestBase() {

    @Test
    fun `should update sync timestamp`() = runTest {
        val feedSourceId = "source-1"
        val timestamp = System.currentTimeMillis()

        database.updateSyncTimestamp(feedSourceId, timestamp)

        val retrieved = database.getSyncTimestamp(feedSourceId)
        retrieved shouldBe timestamp
    }

    @Test
    fun `should set and clear sync flag`() = runTest {
        val feedItemId = "item-1"

        database.setSyncFlag(feedItemId, needsSync = true)
        database.getSyncFlag(feedItemId) shouldBe true

        database.setSyncFlag(feedItemId, needsSync = false)
        database.getSyncFlag(feedItemId) shouldBe false
    }

    @Test
    fun `should track account-specific sync metadata`() = runTest {
        val accountId = "account-1"
        val lastSyncTime = System.currentTimeMillis()

        database.updateAccountSyncTime(accountId, lastSyncTime)

        val retrieved = database.getAccountSyncTime(accountId)
        retrieved shouldBe lastSyncTime
    }
}
```

**Acceptance Criteria**:
- ✅ Sync timestamps tracked
- ✅ Sync flags work
- ✅ Account-specific metadata tracked

---

## Summary

After completing Phase 2, you will have:

- ✅ All DatabaseHelper CRUD operations tested
- ✅ Pagination tested with property-based inputs
- ✅ All filters tested (read, bookmarked, category, source)
- ✅ Mark read operations tested (single, multiple, above/below)
- ✅ Cleanup operations tested (old items, deleted items)
- ✅ Search functionality tested
- ✅ Sync metadata tested

**Next Phase**: Phase 3 - Repository Tests → See `04-repositories.md`
