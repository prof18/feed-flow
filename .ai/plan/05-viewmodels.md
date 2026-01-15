# Phase 4: ViewModel Tests

**Goal**: Test UI state management in ViewModels using Turbine for Flow testing and Koin for dependencies.

---

## Task 4.1: HomeViewModel Tests (CRITICAL)

**File**: `shared/src/commonTest/kotlin/.../presentation/HomeViewModelTest.kt`

```kotlin
package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import com.prof18.feedflow.shared.test.koin.TestModules
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test

class HomeViewModelTest : KoinTestBase() {

    override fun getTestModules(): List<Module> = listOf(
        TestModules.createTestDatabaseModule(),
        module {
            single { FeedStateRepository(get(), get()) }
            single { FeedActionsRepository(get(), get(), get()) }
            single { HomeViewModel(get(), get(), get()) }
        }
    )

    private val viewModel: HomeViewModel by inject()
    private val database: DatabaseHelper by inject()

    @Test
    fun `should emit initial feed state`() = runTest {
        viewModel.homeState.test {
            val state = awaitItem()
            state.feedState.shouldNotBeNull()
            state.isLoading shouldBe false
        }
    }

    @Test
    fun `should update filter and reload feeds`() = runTest {
        val readItems = List(5) {
            FeedItemGenerator.feedItemArb.sample().value.copy(isRead = true)
        }
        val unreadItems = List(5) {
            FeedItemGenerator.feedItemArb.sample().value.copy(isRead = false)
        }

        (readItems + unreadItems).forEach { database.insertFeedItem(it) }

        viewModel.homeState.test {
            skipItems(1)  // Skip initial state

            viewModel.updateFilter(FeedFilter.OnlyUnread)

            val updated = awaitItem()
            updated.feedState.currentFilter shouldBe FeedFilter.OnlyUnread
            updated.feedState.feedItems.size shouldBe 5
        }
    }

    @Test
    fun `should show loading state during refresh`() = runTest {
        viewModel.homeState.test {
            skipItems(1)

            viewModel.refreshFeeds()

            val loading = awaitItem()
            loading.isLoading shouldBe true

            val completed = awaitItem()
            completed.isLoading shouldBe false
        }
    }

    @Test
    fun `should load more items on scroll`() = runTest {
        val items = List(100) { FeedItemGenerator.feedItemArb.sample().value }
        items.forEach { database.insertFeedItem(it) }

        viewModel.homeState.test {
            val initial = awaitItem()
            initial.feedState.feedItems.size shouldBe 40

            viewModel.loadMore()

            val updated = awaitItem()
            updated.feedState.feedItems.size shouldBe 80
        }
    }

    @Test
    fun `should mark as read on scroll`() = runTest {
        val items = List(10) { FeedItemGenerator.unreadFeedItemArb().sample().value }
        items.forEach { database.insertFeedItem(it) }

        viewModel.homeState.test {
            val initial = awaitItem()
            val firstItem = initial.feedState.feedItems.first()

            viewModel.onArticleViewed(firstItem.id)

            val updated = awaitItem()
            updated.feedState.feedItems.first().isRead shouldBe true
        }
    }

    @Test
    fun `should mark all as read`() = runTest {
        val items = List(10) { FeedItemGenerator.unreadFeedItemArb().sample().value }
        items.forEach { database.insertFeedItem(it) }

        viewModel.homeState.test {
            skipItems(1)

            viewModel.markAllAsRead()

            val updated = awaitItem()
            updated.feedState.feedItems.all { it.isRead } shouldBe true
        }
    }

    @Test
    fun `should delete feed item`() = runTest {
        val item = FeedItemGenerator.feedItemArb.sample().value
        database.insertFeedItem(item)

        viewModel.homeState.test {
            val initial = awaitItem()
            initial.feedState.feedItems.size shouldBe 1

            viewModel.deleteFeedItem(item.id)

            val updated = awaitItem()
            updated.feedState.feedItems.size shouldBe 0
        }
    }

    @Test
    fun `should toggle bookmark status`() = runTest {
        val item = FeedItemGenerator.feedItemArb.sample().value.copy(isBookmarked = false)
        database.insertFeedItem(item)

        viewModel.homeState.test {
            val initial = awaitItem()
            initial.feedState.feedItems.first().isBookmarked shouldBe false

            viewModel.toggleBookmark(item.id)

            val updated = awaitItem()
            updated.feedState.feedItems.first().isBookmarked shouldBe true
        }
    }

    @Test
    fun `should emit error state on fetch failure`() = runTest {
        // Configure feed parser to fail
        // (requires setup in FeedFetcherRepository)

        viewModel.homeState.test {
            skipItems(1)

            viewModel.refreshFeeds()

            val errorState = awaitItem()
            errorState.error.shouldNotBeNull()
        }
    }

    @Test
    fun `should update nav drawer state`() = runTest {
        viewModel.homeState.test {
            skipItems(1)

            viewModel.toggleNavDrawer()

            val updated = awaitItem()
            updated.isNavDrawerOpen shouldBe true

            viewModel.toggleNavDrawer()

            val closed = awaitItem()
            closed.isNavDrawerOpen shouldBe false
        }
    }
}
```

**Acceptance Criteria**:
- ✅ Initial state emission
- ✅ Filter updates
- ✅ Loading states
- ✅ Pagination
- ✅ Mark as read on scroll
- ✅ Mark all as read
- ✅ Delete operations
- ✅ Bookmark toggle
- ✅ Error states
- ✅ Nav drawer state

---

## Task 4.2: AddFeedViewModel Tests

**File**: `shared/src/commonTest/kotlin/.../presentation/AddFeedViewModelTest.kt`

```kotlin
package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.koin.TestModules
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.domain
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test

class AddFeedViewModelTest : KoinTestBase() {

    override fun getTestModules(): List<Module> = listOf(
        TestModules.createTestDatabaseModule(),
        module {
            single { FeedSourcesRepository(get()) }
            single { AddFeedViewModel(get()) }
        }
    )

    private val viewModel: AddFeedViewModel by inject()

    @Test
    fun `should validate URLs`() = runTest {
        checkAll(20, Arb.domain()) { domain ->
            val validUrl = "https://$domain/feed.xml"

            viewModel.state.test {
                viewModel.setUrl(validUrl)

                val state = awaitItem()
                state.isUrlValid shouldBe true
                state.urlError.shouldBeNull()
            }
        }
    }

    @Test
    fun `should reject invalid URLs`() = runTest {
        val invalidUrls = listOf("not a url", "ftp://example.com", "")

        invalidUrls.forEach { invalidUrl ->
            viewModel.state.test {
                viewModel.setUrl(invalidUrl)

                val state = awaitItem()
                state.isUrlValid shouldBe false
                state.urlError.shouldNotBeNull()
            }
        }
    }

    @Test
    fun `should add feed with category`() = runTest {
        viewModel.state.test {
            viewModel.setUrl("https://example.com/feed.xml")
            viewModel.setTitle("Test Feed")
            viewModel.setCategory("Tech")

            viewModel.addFeed()

            val success = awaitItem()
            success.isSuccess shouldBe true
        }
    }

    @Test
    fun `should detect duplicate feeds`() = runTest {
        val url = "https://example.com/feed.xml"

        // Add first feed
        viewModel.setUrl(url)
        viewModel.setTitle("Feed 1")
        viewModel.addFeed()

        // Try to add duplicate
        viewModel.state.test {
            viewModel.setUrl(url)
            viewModel.setTitle("Feed 2")
            viewModel.addFeed()

            val error = awaitItem()
            error.isSuccess shouldBe false
            error.error shouldBe "Duplicate feed"
        }
    }

    @Test
    fun `should create new category if not exists`() = runTest {
        viewModel.setUrl("https://example.com/feed.xml")
        viewModel.setTitle("Test Feed")
        viewModel.setCategory("New Category")

        viewModel.state.test {
            viewModel.addFeed()

            val success = awaitItem()
            success.isSuccess shouldBe true
            // Verify category was created in database
        }
    }
}
```

**Acceptance Criteria**:
- ✅ URL validation works
- ✅ Feed addition succeeds
- ✅ Duplicate detection works
- ✅ Category creation works

---

## Task 4.3: SearchViewModel Tests

**File**: `shared/src/commonTest/kotlin/.../presentation/SearchViewModelTest.kt`

```kotlin
package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import com.prof18.feedflow.shared.test.koin.TestModules
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test

class SearchViewModelTest : KoinTestBase() {

    override fun getTestModules(): List<Module> = listOf(
        TestModules.createTestDatabaseModule(),
        module {
            single { FeedActionsRepository(get(), get(), get()) }
            single { SearchViewModel(get()) }
        }
    )

    private val viewModel: SearchViewModel by inject()
    private val database: DatabaseHelper by inject()

    @Test
    fun `should search and emit results`() = runTest {
        val items = listOf(
            FeedItemGenerator.feedItemArb.sample().value.copy(title = "Kotlin News"),
            FeedItemGenerator.feedItemArb.sample().value.copy(title = "Java Update"),
        )
        items.forEach { database.insertFeedItem(it) }

        viewModel.state.test {
            viewModel.search("Kotlin")

            val results = awaitItem()
            results.results.size shouldBe 1
            results.results[0].title shouldBe "Kotlin News"
        }
    }

    @Test
    fun `should show empty state for no results`() = runTest {
        viewModel.state.test {
            viewModel.search("NonExistent")

            val state = awaitItem()
            state.results.isEmpty() shouldBe true
            state.isEmpty shouldBe true
        }
    }

    @Test
    fun `should apply filter to search results`() = runTest {
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

        viewModel.state.test {
            viewModel.setFilter(FeedFilter.OnlyUnread)
            viewModel.search("Kotlin")

            val results = awaitItem()
            results.results.size shouldBe 1
            results.results[0].title shouldBe "Unread Kotlin Article"
        }
    }

    @Test
    fun `should clear search`() = runTest {
        viewModel.state.test {
            viewModel.search("Kotlin")
            skipItems(1)

            viewModel.clearSearch()

            val cleared = awaitItem()
            cleared.query.isEmpty() shouldBe true
            cleared.results.isEmpty() shouldBe true
        }
    }
}
```

**Acceptance Criteria**:
- ✅ Search returns correct results
- ✅ Empty state shown for no results
- ✅ Filters applied to search
- ✅ Clear search works

---

## Task 4.4: ImportExportViewModel Tests

**File**: `shared/src/commonTest/kotlin/.../presentation/ImportExportViewModelTest.kt`

```kotlin
package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.opml.OpmlExporter
import com.prof18.feedflow.shared.domain.opml.OpmlParser
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.koin.TestModules
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test

class ImportExportViewModelTest : KoinTestBase() {

    override fun getTestModules(): List<Module> = listOf(
        TestModules.createTestDatabaseModule(),
        module {
            single { OpmlParser() }
            single { OpmlExporter() }
            single { ImportExportViewModel(get(), get(), get()) }
        }
    )

    private val viewModel: ImportExportViewModel by inject()
    private val database: DatabaseHelper by inject()

    @Test
    fun `should export OPML`() = runTest {
        val sources = List(5) { FeedSourceGenerator.feedSourceArb.sample().value }
        sources.forEach { database.insertFeedSource(it) }

        viewModel.state.test {
            viewModel.exportOpml()

            val exported = awaitItem()
            exported.exportedOpml.shouldNotBeNull()
            exported.exportedOpml?.contains("<opml") shouldBe true
        }
    }

    @Test
    fun `should import OPML`() = runTest {
        val opml = """
            <?xml version="1.0"?>
            <opml version="2.0">
                <body>
                    <outline text="Feed 1" xmlUrl="https://example.com/feed1.xml" />
                    <outline text="Feed 2" xmlUrl="https://example.com/feed2.xml" />
                </body>
            </opml>
        """.trimIndent()

        viewModel.state.test {
            viewModel.importOpml(opml)

            val imported = awaitItem()
            imported.isSuccess shouldBe true
        }

        val sources = database.getAllFeedSources()
        sources.size shouldBe 2
    }

    @Test
    fun `should handle invalid OPML`() = runTest {
        val invalidOpml = "not valid xml"

        viewModel.state.test {
            viewModel.importOpml(invalidOpml)

            val error = awaitItem()
            error.isSuccess shouldBe false
            error.error.shouldNotBeNull()
        }
    }
}
```

**Acceptance Criteria**:
- ✅ OPML export works
- ✅ OPML import works
- ✅ Invalid OPML handled

---

## Task 4.5: Sync ViewModels Tests

### FreshRSSViewModel Tests

**File**: `shared/src/commonTest/kotlin/.../presentation/sync/FreshRSSViewModelTest.kt`

```kotlin
package com.prof18.feedflow.shared.presentation.sync

import app.cash.turbine.test
import com.prof18.feedflow.core.model.AccountType
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.fakes.FakeAccountsRepository
import com.prof18.feedflow.shared.test.koin.TestModules
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test

class FreshRSSViewModelTest : KoinTestBase() {

    private val fakeAccountsRepository = FakeAccountsRepository()

    override fun getTestModules(): List<Module> = listOf(
        TestModules.createTestDatabaseModule(),
        module {
            single<AccountsRepository> { fakeAccountsRepository }
            single { FreshRSSViewModel(get(), get()) }
        }
    )

    private val viewModel: FreshRSSViewModel by inject()

    @Test
    fun `should login successfully`() = runTest {
        viewModel.state.test {
            viewModel.login(
                serverUrl = "https://freshrss.example.com",
                username = "user",
                password = "pass"
            )

            val success = awaitItem()
            success.isLoggedIn shouldBe true
        }
    }

    @Test
    fun `should handle login failure`() = runTest {
        viewModel.state.test {
            viewModel.login(
                serverUrl = "invalid",
                username = "user",
                password = "wrong"
            )

            val error = awaitItem()
            error.isLoggedIn shouldBe false
            error.error.shouldNotBeNull()
        }
    }

    @Test
    fun `should disconnect and clear data`() = runTest {
        fakeAccountsRepository.setAccount(AccountType.FreshRSS)

        viewModel.state.test {
            viewModel.disconnect()

            val disconnected = awaitItem()
            disconnected.isLoggedIn shouldBe false
        }

        fakeAccountsRepository.getCurrentAccount() shouldBe AccountType.Local
    }
}
```

**Similar tests for**:
- `MinifluxViewModelTest.kt`
- `FeedbinViewModelTest.kt`
- `BazquxViewModelTest.kt`

**Acceptance Criteria**:
- ✅ Login success/failure tested
- ✅ Disconnect clears data
- ✅ Account state reflected

---

## Summary

After completing Phase 4, you will have:

- ✅ HomeViewModel fully tested (all user interactions)
- ✅ AddFeedViewModel tested (validation, duplication)
- ✅ SearchViewModel tested (search, filters)
- ✅ ImportExportViewModel tested (OPML)
- ✅ Sync ViewModels tested (login/logout)

**Next Phase**: Phase 5 - Sync Provider Tests → See `06-sync-providers.md`
