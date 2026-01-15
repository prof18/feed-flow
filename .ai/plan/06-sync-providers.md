# Phase 5: Sync Provider Tests (Complex - Later)

**Goal**: Test network-heavy sync providers using Ktor MockEngine to simulate HTTP responses.

**Note**: This is the most complex phase. Consider tackling this after the foundation is solid.

---

## Task 5.1: GReaderClient Tests (Ktor MockEngine)

**File**: `feedSync/greader/src/commonTest/kotlin/.../data/GReaderClientTest.kt`

```kotlin
package com.prof18.feedflow.feedsync.greader.data

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class GReaderClientTest {

    private fun createMockClient(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): GReaderClient {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler(handler)
            }
        }
        return GReaderClient(httpClient, baseUrl = "https://example.com")
    }

    @Test
    fun `should login successfully`() = runTest {
        val client = createMockClient { request ->
            when (request.url.encodedPath) {
                "/accounts/ClientLogin" -> {
                    respond(
                        content = "Auth=test-auth-token",
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }

        val authToken = client.login(username = "user", password = "pass")
        authToken shouldBe "test-auth-token"
    }

    @Test
    fun `should parse subscription list`() = runTest {
        val client = createMockClient { request ->
            when (request.url.encodedPath) {
                "/reader/api/0/subscription/list" -> {
                    respond(
                        content = """
                            {
                                "subscriptions": [
                                    {
                                        "id": "feed/https://example.com/feed.xml",
                                        "title": "Example Feed",
                                        "categories": [
                                            {"id": "user/-/label/Tech", "label": "Tech"}
                                        ]
                                    }
                                ]
                            }
                        """.trimIndent(),
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }

        val subs = client.getSubscriptions(authToken = "test-token")
        subs.shouldNotBeEmpty()
        subs[0].title shouldBe "Example Feed"
        subs[0].categories.first().label shouldBe "Tech"
    }

    @Test
    fun `should fetch stream items with pagination`() = runTest {
        val client = createMockClient { request ->
            when (request.url.encodedPath) {
                "/reader/api/0/stream/contents" -> {
                    respond(
                        content = """
                            {
                                "items": [
                                    {
                                        "id": "tag:google.com,2005:reader/item/abc123",
                                        "title": "Article 1",
                                        "canonical": [{"href": "https://example.com/1"}],
                                        "published": 1234567890,
                                        "categories": []
                                    }
                                ],
                                "continuation": "next-page-token"
                            }
                        """.trimIndent(),
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }

        val result = client.getStreamContents(
            authToken = "test-token",
            streamId = "user/-/state/com.google/reading-list"
        )

        result.items.shouldNotBeEmpty()
        result.continuation shouldBe "next-page-token"
    }

    @Test
    fun `should edit tag for star operation`() = runTest {
        val client = createMockClient { request ->
            when (request.url.encodedPath) {
                "/reader/api/0/edit-tag" -> {
                    respond(
                        content = "OK",
                        status = HttpStatusCode.OK
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }

        val result = client.editTag(
            authToken = "test-token",
            itemId = "item/123",
            addTag = "user/-/state/com.google/starred"
        )

        result.isSuccess shouldBe true
    }

    @Test
    fun `should refresh token on 401`() = runTest {
        var callCount = 0
        val client = createMockClient { request ->
            when (request.url.encodedPath) {
                "/reader/api/0/subscription/list" -> {
                    callCount++
                    if (callCount == 1) {
                        respond("", HttpStatusCode.Unauthorized)
                    } else {
                        respond(
                            content = """{"subscriptions": []}""",
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    }
                }
                "/accounts/ClientLogin" -> {
                    respond(
                        content = "Auth=new-token",
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }

        // First call gets 401, should retry with new token
        val subs = client.getSubscriptions(authToken = "old-token")
        callCount shouldBe 2  // First attempt + retry
    }

    @Test
    fun `should chunk large requests`() = runTest {
        val itemIds = List(2000) { "item/$it" }  // More than 1000 items
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            respond("OK", HttpStatusCode.OK)
        }

        client.markAsRead(authToken = "test-token", itemIds = itemIds)

        requestCount shouldBe 2  // Should chunk into 2 requests (1000 each)
    }
}
```

**Acceptance Criteria**:
- ✅ Login tested
- ✅ Subscription parsing tested
- ✅ Stream pagination tested
- ✅ Edit tag operations tested
- ✅ Token refresh on 401 tested
- ✅ Request chunking tested

---

## Task 5.2: GReaderRepository Tests

**File**: `feedSync/greader/src/commonTest/kotlin/.../domain/GReaderRepositoryTest.kt`

```kotlin
package com.prof18.feedflow.feedsync.greader.domain

import com.prof18.feedflow.feedsync.database.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.greader.data.GReaderClient
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.koin.TestModules
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test

class GReaderRepositoryTest : KoinTestBase() {

    // Use a fake GReaderClient that wraps MockEngine
    private val fakeClient = FakeGReaderClient()

    override fun getTestModules(): List<Module> = listOf(
        TestModules.createTestDatabaseModule(),
        module {
            single<GReaderClient> { fakeClient }
            single { SyncedDatabaseHelper(get()) }
            single { GReaderRepository(get(), get()) }
        }
    )

    private val repository: GReaderRepository by inject()
    private val database: SyncedDatabaseHelper by inject()

    @Test
    fun `should sync categories and feeds`() = runTest {
        fakeClient.setupSubscriptions(
            listOf(
                SubscriptionDTO(
                    id = "feed/https://example.com/feed.xml",
                    title = "Example Feed",
                    categories = listOf(CategoryDTO(id = "label/Tech", label = "Tech"))
                )
            )
        )

        repository.sync()

        val categories = database.getAllCategories()
        categories.any { it.name == "Tech" } shouldBe true

        val feeds = database.getAllFeedSources()
        feeds.any { it.title == "Example Feed" } shouldBe true
    }

    @Test
    fun `should use item ID sync for Miniflux`() = runTest {
        // Miniflux uses item IDs instead of timestamps
        fakeClient.providerType = GReaderProviderType.Miniflux

        fakeClient.setupStreamItems(
            listOf(
                ItemContentDTO(
                    id = "item/123",
                    title = "Article",
                    published = 1234567890
                )
            )
        )

        repository.sync()

        val items = database.getAllFeedItems()
        items.shouldNotBeEmpty()
    }

    @Test
    fun `should mark items as read`() = runTest {
        val itemId = "item/123"
        database.insertFeedItem(
            FeedItem(id = itemId, title = "Article", isRead = false)
        )

        repository.markAsRead(itemId, isRead = true)

        val updated = database.getFeedItemById(itemId)
        updated?.isRead shouldBe true
    }

    @Test
    fun `should update last sync timestamp`() = runTest {
        val beforeSync = System.currentTimeMillis()

        repository.sync()

        val lastSync = database.getLastSyncTimestamp()
        lastSync shouldBeGreaterThan beforeSync
    }

    @Test
    fun `should delete category and its feeds`() = runTest {
        database.insertCategory(FeedSourceCategory(id = "cat-1", name = "Tech"))
        database.insertFeedSource(
            FeedSource(id = "feed-1", title = "Feed", category = "Tech")
        )

        repository.deleteCategory("cat-1")

        val categories = database.getAllCategories()
        categories.none { it.id == "cat-1" } shouldBe true
    }

    @Test
    fun `should add feed with category`() = runTest {
        fakeClient.setupAddSubscriptionSuccess()

        repository.addFeed(
            url = "https://example.com/feed.xml",
            title = "New Feed",
            category = "Tech"
        )

        val feeds = database.getAllFeedSources()
        feeds.any { it.title == "New Feed" } shouldBe true
    }
}
```

**Acceptance Criteria**:
- ✅ Sync fetches categories and feeds
- ✅ Provider-specific sync logic tested (Miniflux vs FreshRSS)
- ✅ Read/bookmark updates work
- ✅ Last sync timestamp updated
- ✅ Category/feed operations work

---

## Task 5.3: FeedbinClient Tests

**File**: `feedSync/feedbin/src/commonTest/kotlin/.../data/FeedbinClientTest.kt`

```kotlin
package com.prof18.feedflow.feedsync.feedbin.data

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class FeedbinClientTest {

    private fun createMockClient(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): FeedbinClient {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler(handler)
            }
        }
        return FeedbinClient(httpClient)
    }

    @Test
    fun `should login with Basic Auth`() = runTest {
        val client = createMockClient { request ->
            when (request.url.encodedPath) {
                "/v2/authentication.json" -> {
                    respond(
                        content = """{"id": 123, "email": "user@example.com"}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }

        val result = client.login(email = "user@example.com", password = "pass")
        result.isSuccess shouldBe true
    }

    @Test
    fun `should parse subscriptions`() = runTest {
        val client = createMockClient { request ->
            when (request.url.encodedPath) {
                "/v2/subscriptions.json" -> {
                    respond(
                        content = """
                            [
                                {
                                    "id": 1,
                                    "feed_id": 101,
                                    "title": "Example Feed",
                                    "feed_url": "https://example.com/feed.xml"
                                }
                            ]
                        """.trimIndent(),
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }

        val subs = client.getSubscriptions(credentials = "test-creds")
        subs.shouldNotBeEmpty()
        subs[0].title shouldBe "Example Feed"
    }

    @Test
    fun `should handle pagination with Link header`() = runTest {
        val client = createMockClient { request ->
            when (request.url.encodedPath) {
                "/v2/entries.json" -> {
                    respond(
                        content = """[{"id": 1, "title": "Article"}]""",
                        headers = headersOf(
                            HttpHeaders.ContentType to listOf("application/json"),
                            HttpHeaders.Link to listOf("<https://api.feedbin.com/v2/entries.json?page=2>; rel=\"next\"")
                        )
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }

        val (entries, nextPage) = client.getEntries(credentials = "test-creds")
        entries.shouldNotBeEmpty()
        nextPage shouldBe "https://api.feedbin.com/v2/entries.json?page=2"
    }

    @Test
    fun `should mark entries as read`() = runTest {
        val client = createMockClient { request ->
            when (request.url.encodedPath) {
                "/v2/unread_entries.json" -> {
                    respond("", HttpStatusCode.NoContent)
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }

        val result = client.markAsRead(
            credentials = "test-creds",
            entryIds = listOf(1, 2, 3)
        )

        result.isSuccess shouldBe true
    }

    @Test
    fun `should get taggings`() = runTest {
        val client = createMockClient { request ->
            when (request.url.encodedPath) {
                "/v2/taggings.json" -> {
                    respond(
                        content = """
                            [
                                {
                                    "id": 1,
                                    "feed_id": 101,
                                    "name": "Tech"
                                }
                            ]
                        """.trimIndent(),
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }

        val taggings = client.getTaggings(credentials = "test-creds")
        taggings.shouldNotBeEmpty()
        taggings[0].name shouldBe "Tech"
    }
}
```

**Acceptance Criteria**:
- ✅ Basic auth login tested
- ✅ Subscription parsing tested
- ✅ Link header pagination tested
- ✅ Entry operations tested
- ✅ Tagging operations tested

---

## Task 5.4: FeedbinRepository Tests

**File**: `feedSync/feedbin/src/commonTest/kotlin/.../domain/FeedbinRepositoryTest.kt`

```kotlin
package com.prof18.feedflow.feedsync.feedbin.domain

import com.prof18.feedflow.feedsync.database.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.feedbin.data.FeedbinClient
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.koin.TestModules
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test

class FeedbinRepositoryTest : KoinTestBase() {

    private val fakeClient = FakeFeedbinClient()

    override fun getTestModules(): List<Module> = listOf(
        TestModules.createTestDatabaseModule(),
        module {
            single<FeedbinClient> { fakeClient }
            single { SyncedDatabaseHelper(get()) }
            single { FeedbinRepository(get(), get()) }
        }
    )

    private val repository: FeedbinRepository by inject()
    private val database: SyncedDatabaseHelper by inject()

    @Test
    fun `should sync after login`() = runTest {
        fakeClient.setupLoginSuccess()
        fakeClient.setupSubscriptions(
            listOf(
                SubscriptionDTO(
                    id = 1,
                    feedId = 101,
                    title = "Example Feed",
                    feedUrl = "https://example.com/feed.xml"
                )
            )
        )

        repository.login(email = "user@example.com", password = "pass")
        repository.sync()

        val feeds = database.getAllFeedSources()
        feeds.any { it.title == "Example Feed" } shouldBe true
    }

    @Test
    fun `should sync 60 days of history`() = runTest {
        val now = System.currentTimeMillis()
        val entries = List(100) {
            EntryDTO(
                id = it,
                title = "Article $it",
                published = now - (it * 24 * 60 * 60 * 1000L)  // 1 day apart
            )
        }
        fakeClient.setupEntries(entries)

        repository.sync()

        val items = database.getAllFeedItems()
        // Should only sync 60 days worth
        items.all {
            it.pubDate >= now - (60L * 24 * 60 * 60 * 1000)
        } shouldBe true
    }

    @Test
    fun `should batch over 1000 items`() = runTest {
        val entryIds = List(2500) { it }

        var requestCount = 0
        fakeClient.onMarkAsRead = { ids ->
            requestCount++
            ids.size <= 1000 shouldBe true  // Verify batching
        }

        repository.markAsRead(entryIds, isRead = true)

        requestCount shouldBe 3  // 1000 + 1000 + 500
    }

    @Test
    fun `should handle taggings as categories`() = runTest {
        fakeClient.setupTaggings(
            listOf(
                TaggingDTO(id = 1, feedId = 101, name = "Tech")
            )
        )

        repository.sync()

        val categories = database.getAllCategories()
        categories.any { it.name == "Tech" } shouldBe true
    }
}
```

**Acceptance Criteria**:
- ✅ Sync after login works
- ✅ 60-day history sync tested
- ✅ Batching over 1000 items works
- ✅ Taggings mapped to categories

---

## Task 5.5: Full Sync Integration Tests

**File**: `feedSync/greader/src/commonTest/kotlin/.../integration/FullSyncTest.kt`

```kotlin
package com.prof18.feedflow.feedsync.greader.integration

import com.prof18.feedflow.feedsync.database.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class FullSyncTest : KoinTestBase() {

    @Test
    fun `should complete full sync flow for FreshRSS`() = runTest {
        // 1. Login
        val loginResult = repository.login(
            serverUrl = "https://freshrss.example.com",
            username = "user",
            password = "pass"
        )
        loginResult.isSuccess shouldBe true

        // 2. Fetch subscriptions
        repository.sync()
        val feeds = database.getAllFeedSources()
        feeds.shouldNotBeEmpty()

        // 3. Fetch items
        val items = database.getAllFeedItems()
        items.shouldNotBeEmpty()

        // 4. Mark items as read
        val itemToMark = items.first()
        repository.markAsRead(itemToMark.id, isRead = true)

        // 5. Re-sync
        repository.sync()

        // 6. Verify database state
        val updatedItem = database.getFeedItemById(itemToMark.id)
        updatedItem?.isRead shouldBe true
    }
}
```

**Acceptance Criteria**:
- ✅ Complete sync flow works from login to database

---

## Summary

After completing Phase 5, you will have:

- ✅ GReaderClient tested with Ktor MockEngine
- ✅ GReaderRepository tested for all GReader providers
- ✅ FeedbinClient tested with Ktor MockEngine
- ✅ FeedbinRepository tested with batching and pagination
- ✅ Full sync integration tests

**Next Phase**: Phase 6 - E2E Tests (Optional) → See `07-e2e.md`
