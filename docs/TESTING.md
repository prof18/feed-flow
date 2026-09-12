# Testing Strategy

## Philosophy

Tests focus on **behavior verification** rather than implementation details. The goal is to test what the code does, not how it does it. Tests should:
- Verify state transitions and observable outputs
- Use real implementations where practical, with fakes only for external boundaries
- Be readable and serve as documentation for expected behavior

## Test Infrastructure

### Location

```
shared/src/
├── commonTest/kotlin/...                  # Common test code (base classes, fixtures, generators)
├── commonJvmAndroidTest/kotlin/...        # Shared JVM/Android test code
├── jvmTest/kotlin/...                     # JVM-specific tests
├── androidUnitTest/kotlin/...             # Android-specific tests
└── iosTest/kotlin/...                     # iOS-specific tests

feedSync/test-utils/src/
├── commonMain/kotlin/...                  # Mock engines, fixture loaders
├── commonMain/resources/fixtures/         # JSON fixtures for sync services
│   ├── feedbin/                           # Feedbin API fixtures
│   └── greader/                           # GReader API fixtures
│       ├── freshrss/
│       ├── miniflux/
│       └── bazqux/
├── jvmMain/kotlin/...                     # JVM fixture loader
├── androidMain/kotlin/...                 # Android fixture loader
└── iosMain/kotlin/...                     # iOS fixture loader
```

### KoinTestBase

All tests that need dependency injection extend `KoinTestBase`, which provides:
- Automatic Koin setup/teardown for each test
- Test dispatcher configuration (`UnconfinedTestDispatcher`)
- Access to test modules with fake dependencies

```kotlin
class MyViewModelTest : KoinTestBase() {
    private val viewModel: MyViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()

    @Test
    fun `test behavior`() = runTest(testDispatcher) {
        // test code
    }
}
```

### TestModules

Located at `shared/src/commonTest/.../test/koin/TestModules.kt`. Provides test implementations for:
- `SqlDriver` - In-memory database
- `DispatcherProvider` - Test dispatchers (`UnconfinedTestDispatcher`)
- `Settings` - `MapSettings` for in-memory preferences
- `FeedSyncWorker`, `FeedItemParserWorker`, etc. - No-op or controllable fakes
- `HtmlRetriever`, `HtmlParser` - Mock HTTP client implementations

**Adding a new fake:**
1. Create the fake class (implement the interface or extend the class)
2. Add to `TestModules.createTestOverridesModule()`:
   - Use `single<Interface> { FakeImpl() }` for shared state across a test
   - Use `factory<Interface> { FakeImpl() }` for fresh instance per injection

**Overriding modules in a specific test:**
```kotlin
class MyViewModelTest : KoinTestBase() {
    private val fakeRssParser = FakeRssParserWrapper()

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<RssParserWrapper> { fakeRssParser }
        factory<FeedSourceLogoRetriever> { FakeFeedSourceLogoRetriever() }
    }
}
```

### Fakes vs Mocks

Prefer **fakes** (simple implementations) over mocking libraries:

```kotlin
private class FakeRssParserWrapper : RssParserWrapper {
    private val channelByUrl = mutableMapOf<String, RssChannel>()
    var callCount: Int = 0
        private set

    fun setChannel(url: String, channel: RssChannel) {
        channelByUrl[url] = channel
    }

    override suspend fun getRssChannel(url: String): RssChannel {
        callCount += 1
        return requireNotNull(channelByUrl[url]) { "Missing channel for $url" }
    }
}
```

## Data Generators (Property-Based Testing)

FeedFlow uses [Kotest Property](https://kotest.io/docs/proptest/property-based-testing.html) for generating test data. Generators are located in `shared/src/commonTest/.../test/generators/`:

- `FeedSourceGenerator` - Generates random `FeedSource` objects
- `FeedItemGenerator` - Generates random `FeedItem` objects
- `RssChannelGenerator` - Generates random `RssChannel` objects
- `RssItemGenerator` - Generates random `RssItem` objects
- `CategoryGenerator` - Generates random category objects
- `AccountGenerator` - Generates random account objects

**Using generators:**
```kotlin
import io.kotest.property.arbitrary.next

private fun createFeedSource(
    id: String,
    title: String,
    category: FeedSourceCategory? = null,
): FeedSource = FeedSourceGenerator.feedSourceArb.next().copy(
    id = id,
    url = "https://example.com/$id/rss.xml",
    title = title,
    category = category,
)
```

**Generator pattern:**
```kotlin
object FeedSourceGenerator {
    val feedSourceArb = arbitrary {
        FeedSource(
            id = Uuid.random().toString(),
            title = Arb.string(5..50).bind(),
            url = Arb.domain().map { "https://$it/feed.xml" }.bind(),
            category = CategoryGenerator.categoryArb.orNull(0.3).bind(),
            // ...
        )
    }
}
```

## Testing Sync Services

### feedSync/test-utils Module

For testing sync services (Feedbin, FreshRSS, Miniflux, Bazqux), use the dedicated test-utils module:

```kotlin
class FeedActionsRepositoryFeedbinTest : KoinTestBase() {
    private val feedActionsRepository: FeedActionsRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + getFeedSyncTestModules(
            feedbinBaseURL = "https://api.feedbin.com/",
            feedbinConfig = {
                configureFeedbinMocks()
            },
        )

    fun setupFeedbinAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.FEEDBIN)
        settings.setSyncUsername("testuser")
        settings.setSyncPwd("testpassword")
    }
}
```

### Mock Engines

Mock HTTP engines are configured per sync service:

- `FeedbinMockEngine` - Mock responses for Feedbin API
- `GReaderMockEngine` - Mock responses for Google Reader API (FreshRSS, Miniflux, Bazqux)

### JSON Fixtures

API response fixtures are stored in `feedSync/test-utils/src/commonMain/resources/fixtures/`:

```
fixtures/
├── feedbin/
│   ├── authentication_success.json
│   ├── subscriptions.json
│   ├── entries.json
│   └── ...
└── greader/
    ├── freshrss/
    ├── miniflux/
    └── bazqux/
```

**Loading fixtures:**
```kotlin
// Platform-specific loaders in LoadFixture.{jvm,android,ios}.kt
val jsonResponse = loadFixture("feedbin/subscriptions.json")
```

## Writing ViewModel Tests

### Basic Pattern

```kotlin
class MyViewModelTest : KoinTestBase() {
    private val viewModel: MyViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()

    @Test
    fun `action produces expected state`() = runTest(testDispatcher) {
        // Arrange
        val feedSource = createFeedSource("source-1", "Test Feed")
        insertFeedSources(feedSource)

        // Act
        val viewModel = getViewModel()
        advanceUntilIdle()

        // Assert
        assertEquals(expectedState, viewModel.state.value)
    }

    private fun getViewModel(): MyViewModel = get()
}
```

### Testing StateFlows with Turbine

Use [Turbine](https://github.com/cashapp/turbine) for Flow testing:

```kotlin
viewModel.feedOperationState.test {
    assertEquals(FeedOperation.None, awaitItem())

    viewModel.markAllRead()

    assertEquals(FeedOperation.MarkingAllRead, awaitItem())
    assertEquals(FeedOperation.None, awaitItem())
}
```

**Common Turbine operations:**
```kotlin
viewModel.stateFlow.test {
    assertEquals(expected, awaitItem())  // Assert emissions
    expectNoEvents()                      // Assert no more emissions
    skipItems(1)                          // Skip emissions
}
```

### Testing with Database

Tests use an in-memory SQLite database. Populate test data via `DatabaseHelper`:

```kotlin
private val databaseHelper: DatabaseHelper by inject()

@Test
fun `search returns matching feeds`() = runTest(testDispatcher) {
    val feedSource = createFeedSource("source-1", "Test Feed")
    databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))

    val items = listOf(
        buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 2000, source = feedSource),
        buildFeedItem(id = "item-2", title = "Item 2", pubDateMillis = 1000, source = feedSource),
    )
    databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)

    // verify results
}
```

### Database Test Helpers

Located in `shared/src/commonTest/.../test/DatabaseTestFixtures.kt`:

```kotlin
fun FeedSource.toParsedFeedSource(): ParsedFeedSource
suspend fun DatabaseHelper.insertFeedSourceWithCategory(feedSource: FeedSource)
fun buildFeedItem(id: String, title: String, pubDateMillis: Long, source: FeedSource): FeedItem
fun buildFeedItemsForSource(source: FeedSource, count: Int): List<FeedItem>
```

## Running Tests

```bash
# Run all shared tests
./gradlew :shared:allTests --quiet --console=plain

# Run specific test class
./gradlew :shared:allTests --tests "com.prof18.feedflow.shared.presentation.HomeViewModelTest" --quiet --console=plain

# Run specific test method (use exact test name with backticks escaped)
./gradlew :shared:allTests --tests "com.prof18.feedflow.shared.presentation.HomeViewModelTest.drawer state uses uncategorized sources when only null category exists" --quiet --console=plain

# Run all tests (Android, Desktop, Shared)
./gradlew test --quiet --console=plain

# Run only JVM tests
./gradlew :shared:jvmTest --quiet --console=plain

# Run only Android unit tests
./gradlew :shared:testDebugUnitTest --quiet --console=plain

# Run only iOS tests
./gradlew :shared:iosSimulatorArm64Test --quiet --console=plain
```

## Best Practices

1. **No `lateinit var`** - Use Koin injection with `by inject()`
2. **Use `runTest(testDispatcher)`** - Always pass the test dispatcher to `runTest`
3. **Call `advanceUntilIdle()`** - After async operations to let coroutines complete
4. **Test observable behavior** - Verify state changes, not internal implementation
5. **Use real implementations** - Only fake external boundaries (network, file system, platform APIs)
6. **Descriptive test names** - Use backtick syntax: `` `action produces expected result` ``
7. **One assertion focus** - Each test should verify one logical behavior
8. **Use generators for test data** - Leverage `FeedSourceGenerator.feedSourceArb.next()` with `.copy()` to customize

## Test Naming Conventions

### Test Files
- Pattern: `*Test.kt`
- Examples: `HomeViewModelTest.kt`, `FeedActionsRepositoryFeedbinTest.kt`
- Platform-specific: `OpmlBOMParsingAndroidTest.kt`, `OpmlBOMParsingJvmTest.kt`

### Test Classes
- Naming: `<ClassName>Test`
- Sync service tests: `<ClassName><ServiceName>Test` (e.g., `FeedActionsRepositoryFeedbinTest`)

### Test Methods
- Use backtick-quoted descriptive names
- Format: `` `description of expected behavior` ``
- Examples:
  - `` `drawer state uses uncategorized sources when only null category exists` ``
  - `` `markAsRead should call feedbinRepository and update state` ``
  - `` `updateBookmarkStatus should star item when isBookmarked is true` ``

## Example Test Structure

Here's a complete example showing all patterns:

```kotlin
class HomeViewModelTest : KoinTestBase() {
    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()

    private val fakeRssParser = FakeRssParserWrapper()

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<RssParserWrapper> { fakeRssParser }
    }

    @Test
    fun `markAllRead updates state and database`() = runTest(testDispatcher) {
        // Arrange
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val items = listOf(
            buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 2000, source = feedSource),
            buildFeedItem(id = "item-2", title = "Item 2", pubDateMillis = 1000, source = feedSource),
        )
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        // Act & Assert with Turbine
        viewModel.feedOperationState.test {
            assertEquals(FeedOperation.None, awaitItem())

            viewModel.markAllRead()
            assertEquals(FeedOperation.MarkingAllRead, awaitItem())
            assertEquals(FeedOperation.None, awaitItem())
        }

        // Verify database state
        val dbItems = getDbItems()
        assertTrue(dbItems.all { it.is_read })
    }

    private fun getViewModel(): HomeViewModel = get()

    private suspend fun insertFeedSources(vararg sources: FeedSource) {
        val categories = sources.mapNotNull { it.category }.distinctBy { it.id }
        if (categories.isNotEmpty()) {
            databaseHelper.insertCategories(categories)
        }
        databaseHelper.insertFeedSource(sources.map { it.toParsedFeedSource() })
    }

    private fun createFeedSource(
        id: String,
        title: String,
        category: FeedSourceCategory? = null,
    ): FeedSource = FeedSourceGenerator.feedSourceArb.next().copy(
        id = id,
        url = "https://example.com/$id/rss.xml",
        title = title,
        category = category,
        lastSyncTimestamp = null,
        logoUrl = null,
    )

    private suspend fun getDbItems() = databaseHelper.getFeedItems(
        feedFilter = FeedFilter.Timeline,
        pageSize = 100,
        offset = 0,
        showReadItems = true,
        sortOrder = FeedOrder.NEWEST_FIRST,
    )

    private class FakeRssParserWrapper : RssParserWrapper {
        private val channelByUrl = mutableMapOf<String, RssChannel>()
        var callCount: Int = 0
            private set

        fun setChannel(url: String, channel: RssChannel) {
            channelByUrl[url] = channel
        }

        override suspend fun getRssChannel(url: String): RssChannel {
            callCount += 1
            return requireNotNull(channelByUrl[url]) { "Missing channel for $url" }
        }
    }
}
```
