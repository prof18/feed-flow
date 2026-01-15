# Phase 3: Repository Tests with Koin

**Goal**: Test business logic in repositories using Koin DI with real implementations, faking only external boundaries.

---

## Task 3.1: Create Minimal Fakes for External Boundaries Only

**Philosophy**: Use real implementations everywhere except for external boundaries we cannot control in tests.

### What We Fake (External Boundaries Only)

Only create fakes for:
1. **HTTP Client** - Network calls (use Ktor MockEngine)
2. **File System** - Platform-specific file operations
3. **Cloud Services** - Dropbox, Google Drive, iCloud SDKs

### What We DON'T Fake (Use Real via Koin)

✅ All repositories - Use real implementations
✅ All database operations - Use real DatabaseHelper with in-memory SQLite
✅ All domain logic - Use real mappers, use cases
✅ All settings - Use real Settings with in-memory implementation (from multiplatform-settings library)

### Ktor MockEngine Helper

**File**: `shared/src/commonTest/kotlin/.../test/network/MockHttpClient.kt`

```kotlin
package com.prof18.feedflow.shared.test.network

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*

/**
 * Helper to create HTTP client with mocked responses
 */
fun createMockHttpClient(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
): HttpClient {
    return HttpClient(MockEngine) {
        engine {
            addHandler(handler)
        }
    }
}

/**
 * Convenience function for simple JSON responses
 */
fun respondJson(content: String, status: HttpStatusCode = HttpStatusCode.OK) =
    HttpResponseData(
        statusCode = status,
        requestTime = GMTDate(),
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
        version = HttpProtocolVersion.HTTP_1_1,
        body = ByteReadChannel(content),
        callContext = Job()
    )
```

### Platform-Specific File System Fake

**File**: `shared/src/commonTest/kotlin/.../test/fakes/FakeFileSystem.kt`

Only create this if needed for OPML/CSV import tests:

```kotlin
package com.prof18.feedflow.shared.test.fakes

/**
 * Simple in-memory file system for testing file operations
 * Use only when testing file read/write that can't be replaced with string content
 */
class FakeFileSystem {
    private val files = mutableMapOf<String, String>()

    fun writeFile(path: String, content: String) {
        files[path] = content
    }

    fun readFile(path: String): String? {
        return files[path]
    }

    fun clear() {
        files.clear()
    }
}
```

### Cloud Service Fakes (Only if Testing Sync Repositories)

**File**: `feedSync/dropbox/src/commonTest/kotlin/.../test/FakeDropboxDataSource.kt`

```kotlin
package com.prof18.feedflow.feedsync.dropbox.test

import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource

/**
 * Fake for Dropbox SDK - cannot use real SDK in tests
 */
class FakeDropboxDataSource : DropboxDataSource {
    private var uploadedContent: String? = null

    override suspend fun uploadFile(content: String, path: String) {
        uploadedContent = content
    }

    override suspend fun downloadFile(path: String): String? {
        return uploadedContent
    }

    fun getLastUpload(): String? = uploadedContent
}
```

**Similar fakes for**:
- `FakeGoogleDriveDataSource.kt` (Google Drive SDK)
- `FakeICloudDataSource.kt` (iCloud directory access)

**Acceptance Criteria**:
- ✅ Only external boundaries faked
- ✅ All domain logic uses real implementations via Koin
- ✅ HTTP mocking uses Ktor MockEngine

---

## Task 3.2: FeedStateRepository Tests (CRITICAL)

**Strategy**: Use 100% real implementations - Real repository + Real database + Real dispatchers.

**File**: `shared/src/commonTest/kotlin/.../domain/feed/FeedStateRepositoryTest.kt`

```kotlin
package com.prof18.feedflow.shared.domain.feed

import app.cash.turbine.test
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import com.prof18.feedflow.shared.test.koin.TestModules
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test

class FeedStateRepositoryTest : KoinTestBase() {

    // ALL REAL IMPLEMENTATIONS via Koin
    override fun getTestModules(): List<Module> = listOf(
        TestModules.createTestDatabaseModule(),  // Real DatabaseHelper with in-memory SQLite
        module {
            single { FeedStateRepository(get(), get()) }  // Real repository with real business logic
        }
    )

    private val repository: FeedStateRepository by inject()  // Real repository
    private val database: DatabaseHelper by inject()  // Direct access for test setup

    @Test
    fun `should emit feed items from database`() = runTest {
        // Arrange: Insert test data
        val items = List(10) { FeedItemGenerator.feedItemArb.sample().value }
        items.forEach { database.insertFeedItem(it) }

        // Act & Assert
        repository.getFeeds().test {
            val state = awaitItem()
            state.feedItems.size shouldBe 10
        }
    }

    @Test
    fun `should load more items on pagination`() = runTest {
        // Insert 100 items
        val items = List(100) { FeedItemGenerator.feedItemArb.sample().value }
        items.forEach { database.insertFeedItem(it) }

        repository.getFeeds().test {
            val initial = awaitItem()
            initial.feedItems.size shouldBe 40  // First page

            repository.loadMoreFeeds()

            val afterLoadMore = awaitItem()
            afterLoadMore.feedItems.size shouldBe 80  // Two pages
        }
    }

    @Test
    fun `should reload when filter changes`() = runTest {
        val readItems = List(5) {
            FeedItemGenerator.feedItemArb.sample().value.copy(isRead = true)
        }
        val unreadItems = List(5) {
            FeedItemGenerator.feedItemArb.sample().value.copy(isRead = false)
        }

        (readItems + unreadItems).forEach { database.insertFeedItem(it) }

        repository.getFeeds().test {
            skipItems(1)  // Skip initial state

            repository.updateFeedFilter(FeedFilter.OnlyUnread)

            val updated = awaitItem()
            updated.currentFilter shouldBe FeedFilter.OnlyUnread
            updated.feedItems.size shouldBe 5
            updated.feedItems.all { !it.isRead } shouldBe true
        }
    }

    @Test
    fun `should update in-memory state when marking as read`() = runTest {
        val item = FeedItemGenerator.unreadFeedItemArb().sample().value
        database.insertFeedItem(item)

        repository.getFeeds().test {
            val initial = awaitItem()
            initial.feedItems.first().isRead shouldBe false

            repository.markAsRead(item.id, isRead = true)

            val updated = awaitItem()
            updated.feedItems.first().isRead shouldBe true
        }
    }

    @Test
    fun `should mark items above as read`() = runTest {
        val items = List(10) { index ->
            FeedItemGenerator.unreadFeedItemArb().sample().value.copy(
                pubDate = System.currentTimeMillis() - (index * 1000L)
            )
        }
        items.forEach { database.insertFeedItem(it) }

        repository.getFeeds().test {
            skipItems(1)

            repository.markItemsAboveAsRead(items[5].id)

            val updated = awaitItem()
            // First 5 items should be read
            updated.feedItems.take(5).all { it.isRead } shouldBe true
            // Remaining items should be unread
            updated.feedItems.drop(5).all { !it.isRead } shouldBe true
        }
    }

    @Test
    fun `should trigger load more when approaching end`() = runTest {
        val items = List(50) { FeedItemGenerator.feedItemArb.sample().value }
        items.forEach { database.insertFeedItem(it) }

        repository.getFeeds().test {
            val initial = awaitItem()
            initial.feedItems.size shouldBe 40

            // Request next article near the end (e.g., article 35)
            repository.getNextArticle(initial.feedItems[35].id)

            val updated = awaitItem()
            updated.feedItems.size shouldBe 50  // Should have loaded more
        }
    }
}
```

**Acceptance Criteria**:
- ✅ Feed state emission works
- ✅ Pagination tested
- ✅ Filter updates trigger reload
- ✅ Mark as read updates state
- ✅ Mark above/below operations work
- ✅ Next article triggers load more

---

## Task 3.3: FeedActionsRepository Tests (CRITICAL)

**Strategy**: Use ALL real implementations. Even AccountsRepository is real, we just configure it for test scenarios.

**File**: `shared/src/commonTest/kotlin/.../domain/feed/FeedActionsRepositoryTest.kt`

```kotlin
package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.AccountType
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.account.AccountsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import com.prof18.feedflow.shared.test.koin.TestModules
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test

class FeedActionsRepositoryTest : KoinTestBase() {

    // ALL REAL IMPLEMENTATIONS
    override fun getTestModules(): List<Module> = listOf(
        TestModules.createTestDatabaseModule(),  // Real database
        TestModules.createTestAccountModule(),  // Real AccountsRepository with test settings
        module {
            single { FeedActionsRepository(get(), get(), get()) }  // Real repository
        }
    )

    private val repository: FeedActionsRepository by inject()  // Real
    private val database: DatabaseHelper by inject()  // Real
    private val accountsRepository: AccountsRepository by inject()  // Real

    @Test
    fun `should update database for local account`() = runTest {
        // Configure real AccountsRepository to use Local account
        accountsRepository.switchAccount(AccountType.Local)

        val feedItem = FeedItemGenerator.feedItemArb.sample().value
        database.insertFeedItem(feedItem)

        repository.markAsRead(feedItem.id, true)

        val updated = database.getFeedItemById(feedItem.id)
        updated?.isRead shouldBe true
    }

    @Test
    fun `should update bookmark status`() = runTest {
        accountsRepository.setAccount(AccountType.Local)

        val feedItem = FeedItemGenerator.feedItemArb.sample().value.copy(isBookmarked = false)
        database.insertFeedItem(feedItem)

        repository.updateBookmarkStatus(feedItem.id, isBookmarked = true)

        val updated = database.getFeedItemById(feedItem.id)
        updated?.isBookmarked shouldBe true
    }

    @Test
    fun `should search feed items`() = runTest {
        val items = listOf(
            FeedItemGenerator.feedItemArb.sample().value.copy(title = "Kotlin News"),
            FeedItemGenerator.feedItemArb.sample().value.copy(title = "Java Update"),
        )
        items.forEach { database.insertFeedItem(it) }

        val results = repository.searchFeedItems("Kotlin")
        results.size shouldBe 1
        results[0].title shouldBe "Kotlin News"
    }

    // TODO: Add tests for GReader and Feedbin accounts
    // These will be added in Phase 5 when we have those repositories tested
}
```

**Acceptance Criteria**:
- ✅ All real implementations via Koin
- ✅ Local account updates database directly
- ✅ Bookmark operations work
- ✅ Search delegates to database

---

## Task 3.4: FeedFetcherRepository Tests (CRITICAL)

**Strategy**: Real implementations + Mock HTTP for RSS parsing only.

**Note**: For RSS parsing, we'll provide static XML strings to the real parser instead of faking the parser. This tests the actual parsing logic.

**File**: `shared/src/commonTest/kotlin/.../domain/feed/FeedFetcherRepositoryTest.kt`

```kotlin
package com.prof18.feedflow.shared.domain.feed

import app.cash.turbine.test
import com.prof18.feedflow.core.model.AccountType
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.account.AccountsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.koin.TestModules
import com.prof18.feedflow.shared.test.network.createMockHttpClient
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test

class FeedFetcherRepositoryTest : KoinTestBase() {

    // Mock HTTP client to return RSS XML strings
    private val mockHttpClient = createMockHttpClient { request ->
        when (request.url.toString()) {
            "https://example.com/feed.xml" -> respondJson(
                content = """
                    <?xml version="1.0"?>
                    <rss version="2.0">
                        <channel>
                            <item>
                                <title>Test Article</title>
                                <link>https://example.com/article</link>
                                <description>Article content</description>
                                <pubDate>Mon, 01 Jan 2024 00:00:00 GMT</pubDate>
                            </item>
                        </channel>
                    </rss>
                """.trimIndent()
            )
            else -> respond("", HttpStatusCode.NotFound)
        }
    }

    // ALL REAL IMPLEMENTATIONS
    override fun getTestModules(): List<Module> = listOf(
        TestModules.createTestDatabaseModule(),  // Real DB
        TestModules.createTestAccountModule(),  // Real AccountsRepository
        module {
            single<HttpClient> { mockHttpClient }  // Only HTTP is mocked
            single { RssFeedParser(get()) }  // Real parser with mock HTTP
            single { FeedFetcherRepository(get(), get(), get(), get()) }  // Real repository
        }
    )

    private val repository: FeedFetcherRepository by inject()
    private val database: DatabaseHelper by inject()
    private val accountsRepository: AccountsRepository by inject()

    @Test
    fun `should fetch local feeds via RSS parser`() = runTest {
        accountsRepository.switchAccount(AccountType.Local)

        val feedSource = FeedSourceGenerator.feedSourceArb.sample().value
        database.insertFeedSource(feedSource)

        val items = List(5) { FeedItemGenerator.feedItemArb.sample().value }
        fakeFeedParser.setupSuccessResponse(feedSource.url, items)

        repository.fetchFeeds().test {
            val progress = awaitItem()
            progress shouldBe FetchProgress.InProgress(1, 1)

            val completion = awaitItem()
            completion shouldBe FetchProgress.Complete

            awaitComplete()
        }

        // Verify items were inserted
        val dbItems = database.getFeedItemsBySource(feedSource.id)
        dbItems.size shouldBe 5
    }

    @Test
    fun `should skip recently fetched feeds`() = runTest {
        fakeAccountsRepository.setAccount(AccountType.Local)

        val feedSource = FeedSourceGenerator.feedSourceArb.sample().value.copy(
            lastSyncTimestamp = System.currentTimeMillis() - (30 * 60 * 1000)  // 30 min ago
        )
        database.insertFeedSource(feedSource)

        repository.fetchFeeds().test {
            val completion = awaitItem()
            completion shouldBe FetchProgress.Complete
            awaitComplete()
        }

        // Should not have called parser
        // (verified by checking no items inserted)
    }

    @Test
    fun `should handle fetch errors gracefully`() = runTest {
        fakeAccountsRepository.setAccount(AccountType.Local)

        val feedSource = FeedSourceGenerator.feedSourceArb.sample().value
        database.insertFeedSource(feedSource)

        fakeFeedParser.setupErrorResponse(feedSource.url, Exception("Network error"))

        repository.fetchFeeds().test {
            awaitItem()  // Progress
            val error = awaitItem()
            error shouldBe FetchProgress.Error("Network error")
            awaitComplete()
        }
    }

    @Test
    fun `should auto-delete old items based on settings`() = runTest {
        fakeAccountsRepository.setAccount(AccountType.Local)

        val now = System.currentTimeMillis()
        val oldItem = FeedItemGenerator.feedItemArb.sample().value.copy(
            pubDate = now - 31L * 24 * 60 * 60 * 1000  // 31 days ago
        )
        database.insertFeedItem(oldItem)

        // Configure auto-delete for 30 days
        repository.setAutoDeletePeriod(30)
        repository.performAutoDelete()

        val remaining = database.getAllFeedItems()
        remaining.isEmpty() shouldBe true
    }

    @Test
    fun `should update feed source logo URL`() = runTest {
        val feedSource = FeedSourceGenerator.feedSourceArb.sample().value.copy(logoUrl = null)
        database.insertFeedSource(feedSource)

        repository.updateFeedSourceLogo(feedSource.id, "https://example.com/logo.png")

        val updated = database.getFeedSourceById(feedSource.id)
        updated?.logoUrl shouldBe "https://example.com/logo.png"
    }
}
```

**Acceptance Criteria**:
- ✅ Local account parses RSS feeds
- ✅ Progress emission works
- ✅ Recently fetched feeds skipped
- ✅ Errors handled gracefully
- ✅ Auto-delete works
- ✅ Logo updates work

---

## Task 3.5: FeedSourcesRepository Tests

**File**: `shared/src/commonTest/kotlin/.../domain/feed/FeedSourcesRepositoryTest.kt`

```kotlin
package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.koin.TestModules
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test

class FeedSourcesRepositoryTest : KoinTestBase() {

    override fun getTestModules(): List<Module> = listOf(
        TestModules.createTestDatabaseModule(),
        module {
            single { FeedSourcesRepository(get()) }
        }
    )

    private val repository: FeedSourcesRepository by inject()
    private val database: DatabaseHelper by inject()

    @Test
    fun `should add feed source with URL validation`() = runTest {
        val validUrl = "https://example.com/feed.xml"
        val result = repository.addFeedSource(validUrl, title = "Test Feed")

        result.isSuccess shouldBe true
        val added = database.getFeedSourceByUrl(validUrl)
        added.shouldNotBeNull()
    }

    @Test
    fun `should reject invalid URLs`() = runTest {
        val invalidUrl = "not a url"
        val result = repository.addFeedSource(invalidUrl, title = "Test Feed")

        result.isFailure shouldBe true
    }

    @Test
    fun `should detect duplicate URLs`() = runTest {
        val url = "https://example.com/feed.xml"
        repository.addFeedSource(url, title = "Feed 1")

        val result = repository.addFeedSource(url, title = "Feed 2")
        result.isFailure shouldBe true
    }

    @Test
    fun `should create category if not exists`() = runTest {
        val url = "https://example.com/feed.xml"
        repository.addFeedSource(url, title = "Feed", category = "Tech")

        val categories = database.getAllCategories()
        categories.any { it.name == "Tech" } shouldBe true
    }

    @Test
    fun `should delete feed source and its items`() = runTest {
        val feedSource = FeedSourceGenerator.feedSourceArb.sample().value
        database.insertFeedSource(feedSource)

        repository.deleteFeedSource(feedSource.id)

        val deleted = database.getFeedSourceById(feedSource.id)
        deleted.shouldBeNull()
    }
}
```

**Acceptance Criteria**:
- ✅ URL validation works
- ✅ Duplicate detection works
- ✅ Category creation works
- ✅ Delete operations work

---

## Task 3.6: FeedCategoryRepository Tests

**File**: `shared/src/commonTest/kotlin/.../domain/category/FeedCategoryRepositoryTest.kt`

```kotlin
package com.prof18.feedflow.shared.domain.category

import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.CategoryGenerator
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.koin.TestModules
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test

class FeedCategoryRepositoryTest : KoinTestBase() {

    override fun getTestModules(): List<Module> = listOf(
        TestModules.createTestDatabaseModule(),
        module {
            single { FeedCategoryRepository(get()) }
        }
    )

    private val repository: FeedCategoryRepository by inject()
    private val database: DatabaseHelper by inject()

    @Test
    fun `should delete category and reset feed source categories`() = runTest {
        val category = CategoryGenerator.categoryArb.sample().value
        database.insertCategory(category)

        val feedSource = FeedSourceGenerator.feedSourceArb.sample().value.copy(
            category = category.name
        )
        database.insertFeedSource(feedSource)

        repository.deleteCategory(category.id)

        // Category should be deleted
        val deletedCategory = database.getCategoryById(category.id)
        deletedCategory.shouldBeNull()

        // Feed source category should be reset to null
        val updatedSource = database.getFeedSourceById(feedSource.id)
        updatedSource?.category.shouldBeNull()
    }

    @Test
    fun `should rename category`() = runTest {
        val category = CategoryGenerator.categoryArb.sample().value
        database.insertCategory(category)

        repository.renameCategory(category.id, "New Name")

        val updated = database.getCategoryById(category.id)
        updated?.name shouldBe "New Name"
    }
}
```

**Acceptance Criteria**:
- ✅ Category delete resets feed sources
- ✅ Rename operations work

---

## Summary

After completing Phase 3, you will have:

- ✅ Fakes for external boundaries (network, parsers, accounts)
- ✅ FeedStateRepository fully tested with Turbine
- ✅ FeedActionsRepository tested for local accounts
- ✅ FeedFetcherRepository tested (sync logic, auto-delete)
- ✅ FeedSourcesRepository tested (CRUD, validation)
- ✅ FeedCategoryRepository tested

**Next Phase**: Phase 4 - ViewModel Tests → See `05-viewmodels.md`
