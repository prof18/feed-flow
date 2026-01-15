# FeedFlow Testing Strategy

**Last Updated**: 2026-01-13

---

## Core Philosophy

**Real Implementations First, Minimal Fakes**

We prioritize using real implementations connected via Koin dependency injection to stress test actual code paths and integration points. Fakes are only used for external boundaries that cannot be controlled in tests.

### What We Use Real Implementations For

✅ **All Domain Logic**
- Repositories (FeedStateRepository, FeedActionsRepository, etc.)
- Use cases and business logic
- Mappers and data transformations
- ViewModels

✅ **All Data Layer**
- DatabaseHelper with in-memory SQLite
- SyncedDatabaseHelper
- Settings (in-memory implementation)

✅ **All Internal Services**
- State management
- Flow operators and transformations
- Dispatchers (TestDispatcher)

### What We Fake (External Boundaries Only)

❌ **Network Layer**
- HTTP clients (Ktor MockEngine for API responses)
- WebSocket connections

❌ **File System**
- OPML/CSV file reading (provide string content directly)
- Image downloads
- Log file writing

❌ **Platform-Specific APIs**
- Dropbox SDK
- Google Drive SDK
- iCloud directory access (macOS/iOS specific)
- System notifications
- Browser opening

❌ **Third-Party Services**
- Analytics
- Crash reporting
- Review prompts

### Why This Approach?

**Benefits**:
1. **Tests catch real bugs**: Integration issues surface immediately
2. **Refactoring confidence**: Changes to internal APIs break tests
3. **Less maintenance**: No need to keep fakes in sync with real implementations
4. **Better coverage**: Tests exercise actual code paths users encounter
5. **Simpler tests**: No complex mock setup, just configure Koin modules

**Trade-offs**:
- Tests are slightly slower (but still fast with in-memory DB)
- May need to configure more Koin modules
- Tests are more integration-like than pure unit tests

---

## Testing Tools & Libraries

### Core Testing Framework
- **Kotlin Test**: `kotlin.test` for assertions and test structure
- **Kotlinx Coroutines Test**: `runTest`, `TestDispatcher` for coroutine testing

### Property-Based Testing
- **Kotest Property**: Generate hundreds of test cases automatically
- **Kotest Assertions**: Fluent assertion library

```kotlin
checkAll(100, feedSourceArb) { feedSource ->
    repository.add(feedSource)
    val retrieved = repository.get(feedSource.id)
    retrieved shouldBe feedSource
}
```

### Dependency Injection
- **Koin Test**: `KoinTest` interface, test modules
- **Real implementations** via Koin modules (default approach)

```kotlin
override fun getTestModules() = listOf(
    TestModules.createTestDatabaseModule(),  // Real DatabaseHelper
    TestModules.createTestRepositoriesModule(),  // Real repositories
    module {
        single { HomeViewModel(get(), get(), get()) }  // Real ViewModel
    }
)
```

### Flow Testing
- **Turbine**: Test Kotlin Flows with `.test { }`

```kotlin
viewModel.state.test {
    val initial = awaitItem()
    viewModel.doAction()
    val updated = awaitItem()
    // assertions
}
```

### Network Mocking
- **Ktor MockEngine**: Simulate HTTP responses without real network calls

```kotlin
val httpClient = HttpClient(MockEngine) {
    engine {
        addHandler { request ->
            when (request.url.encodedPath) {
                "/api/feeds" -> respond(feedsJson, HttpStatusCode.OK)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
    }
}
```

### Database Testing
- **SQLDelight In-Memory Drivers**: Real SQL queries, no persistence
  - JVM: `JdbcSqliteDriver.IN_MEMORY`
  - iOS/macOS: `NativeSqliteDriver` with `:memory:`
  - Android: `AndroidSqliteDriver` with in-memory config

### Settings Testing
- **MapSettings**: From `multiplatform-settings-test` library
  - Real `Settings` implementation backed by `MutableMap`
  - Specifically designed for testing
  - No need to create your own fake

```kotlin
// In build.gradle.kts commonTest
implementation("com.russhwolf:multiplatform-settings-test:1.3.0")

// In test code
import com.russhwolf.settings.MapSettings

single<Settings> { MapSettings() }  // Real implementation for tests
```

---

## Test Organization

### Test Location Strategy

```
shared/src/
├── commonTest/kotlin/com/prof18/feedflow/shared/
│   ├── test/                    # Test infrastructure
│   │   ├── generators/          # Kotest Arb generators
│   │   │   ├── FeedSourceGenerator.kt
│   │   │   ├── FeedItemGenerator.kt
│   │   │   ├── CategoryGenerator.kt
│   │   │   └── AccountGenerator.kt
│   │   ├── koin/                # Test Koin modules
│   │   │   └── TestModules.kt
│   │   ├── KoinTestBase.kt      # Base class for Koin tests
│   │   ├── KoinInfrastructureTest.kt
│   │   ├── InMemoryDriverFactory.kt
│   │   ├── FakeClock.kt         # Fake Clock for time-dependent tests
│   │   ├── TestDispatcherProvider.kt
│   │   └── TestLogger.kt
│   ├── domain/                  # Domain logic tests
│   │   ├── DateFormatterTest.kt
│   │   └── DateFormatterTimezoneTest.kt
│   ├── utils/                   # Utility tests
│   │   ├── LinkSanitizerTest.kt
│   │   ├── ArchiveISTest.kt
│   │   └── RetryHelpersTest.kt
│   └── FeedFlowDatabaseTest.kt
│
├── commonJvmAndroidTest/        # Shared JVM/Android tests
│   └── kotlin/.../
│       ├── domain/JvmHtmlParserTest.kt
│       └── utils/UrlUtilsTest.kt
│
├── jvmTest/                     # JVM-specific tests
│   └── kotlin/.../domain/opml/
│       ├── OPMLFeedParserTest.kt
│       ├── OpmlBOMParsingTest.kt
│       └── OpmlAmpersandTest.kt
│
├── iosTest/                     # iOS-specific tests
│   └── kotlin/.../domain/opml/
│       ├── OPMLFeedParserTest.kt
│       └── OpmlBOMParsingIosTest.kt
│
└── androidUnitTest/             # Android unit tests
    └── kotlin/.../
        ├── OPMLFeedParserTest.kt
        └── domain/opml/OpmlBOMParsingAndroidTest.kt
```

### Test File Naming

- **Test files**: `{ClassName}Test.kt`
  - Example: `FeedStateRepositoryTest.kt`
- **Test generators**: `{ModelName}Generator.kt`
  - Example: `FeedSourceGenerator.kt`
- **Test modules**: `TestModules.kt`
- **Base classes**: `{Type}TestBase.kt`
  - Example: `KoinTestBase.kt`, `DatabaseTestBase.kt`

---

## Testing Patterns

### Pattern 1: Repository Tests with Real Dependencies

**Goal**: Test business logic with real database and real domain services.

```kotlin
class FeedStateRepositoryTest : KoinTestBase() {

    override fun getTestModules() = listOf(
        TestModules.createTestDatabaseModule(),  // Real DatabaseHelper
        module {
            single { FeedStateRepository(get(), get()) }  // Real repository
        }
    )

    private val repository: FeedStateRepository by inject()
    private val database: DatabaseHelper by inject()  // Direct access for setup

    @Test
    fun `should emit feed items from database`() = runTest {
        // Arrange: Use real database
        val items = List(10) { FeedItemGenerator.feedItemArb.sample().value }
        items.forEach { database.insertFeedItem(it) }

        // Act: Use real repository
        repository.getFeeds().test {
            // Assert
            val state = awaitItem()
            state.feedItems.size shouldBe 10
        }
    }
}
```

**Key Points**:
- Real `DatabaseHelper` with in-memory SQLite
- Real `FeedStateRepository` with actual business logic
- Direct database access for test setup
- Turbine for Flow testing

---

### Pattern 2: ViewModel Tests with Real Repository Layer

**Goal**: Test UI state with real repositories and database.

```kotlin
class HomeViewModelTest : KoinTestBase() {

    override fun getTestModules() = listOf(
        TestModules.createTestDatabaseModule(),  // Real DB
        TestModules.createTestRepositoriesModule(),  // Real repositories
        module {
            single { HomeViewModel(get(), get(), get()) }  // Real ViewModel
        }
    )

    private val viewModel: HomeViewModel by inject()
    private val database: DatabaseHelper by inject()

    @Test
    fun `should update filter and reload feeds`() = runTest {
        // Setup data in real database
        val items = List(10) { FeedItemGenerator.feedItemArb.sample().value }
        items.forEach { database.insertFeedItem(it) }

        viewModel.homeState.test {
            skipItems(1)

            // Act on real ViewModel
            viewModel.updateFilter(FeedFilter.OnlyUnread)

            // Real state flows through real repository to real database
            val updated = awaitItem()
            updated.feedState.currentFilter shouldBe FeedFilter.OnlyUnread
        }
    }
}
```

**Key Points**:
- Full stack: ViewModel → Repository → Database
- All real implementations
- Tests exercise actual user flows

---

### Pattern 3: Network Tests with Ktor MockEngine

**Goal**: Test HTTP clients without real network calls.

```kotlin
class GReaderClientTest {

    private fun createMockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): GReaderClient {
        val httpClient = HttpClient(MockEngine) {
            engine { addHandler(handler) }
        }
        return GReaderClient(httpClient, baseUrl = "https://test.com")
    }

    @Test
    fun `should parse subscription list`() = runTest {
        val client = createMockClient { request ->
            when (request.url.encodedPath) {
                "/subscription/list" -> respond(
                    content = """{"subscriptions": [...]}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }

        val subs = client.getSubscriptions("token")
        subs.shouldNotBeEmpty()
    }
}
```

**Key Points**:
- Mock only the HTTP engine, not the client logic
- Real parsing, mapping, error handling
- Fast, deterministic, no network required

---

### Pattern 4: Property-Based Testing with Generators

**Goal**: Test with hundreds of random inputs to find edge cases.

```kotlin
class DatabaseHelperTest : DatabaseTestBase() {

    @Test
    fun `should handle any valid feed source`() = runTest {
        checkAll(100, FeedSourceGenerator.feedSourceArb) { feedSource ->
            database.insertFeedSource(feedSource)
            val retrieved = database.getFeedSourceById(feedSource.id)
            retrieved shouldBe feedSource
        }
    }

    @Test
    fun `should handle pagination with any offset`() = runTest {
        val items = List(100) { FeedItemGenerator.feedItemArb.sample().value }
        items.forEach { database.insertFeedItem(it) }

        checkAll(Arb.int(0..60)) { offset ->
            val page = database.getFeedItems(offset = offset, limit = 40)
            page.size shouldBe min(40, 100 - offset)
        }
    }
}
```

**Key Points**:
- Generates random but valid test data
- Runs 100+ iterations per test
- Catches edge cases (nulls, empty strings, large numbers, etc.)

---

## What to Fake (And How)

### 1. HTTP Client (Ktor MockEngine)

**Use When**: Testing API clients without network calls.

```kotlin
class FakeFeedbinClient {
    private val mockResponses = mutableMapOf<String, String>()

    fun setupSubscriptions(json: String) {
        mockResponses["/v2/subscriptions.json"] = json
    }

    fun createClient(): FeedbinClient {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val response = mockResponses[request.url.encodedPath]
                    if (response != null) {
                        respond(response, HttpStatusCode.OK)
                    } else {
                        respond("", HttpStatusCode.NotFound)
                    }
                }
            }
        }
        return FeedbinClient(httpClient)
    }
}
```

---

### 2. Platform-Specific File System

**Use When**: Testing file upload/download without real cloud services.

```kotlin
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

**Then use in repository tests**:
```kotlin
class DropboxSyncRepositoryTest : KoinTestBase() {
    override fun getTestModules() = listOf(
        TestModules.createTestDatabaseModule(),  // Real DB
        module {
            single<DropboxDataSource> { FakeDropboxDataSource() }  // Fake cloud
            single { DropboxSyncRepository(get(), get()) }  // Real repository
        }
    )
}
```

---

### 3. Settings (Use Library's Test Implementation)

**Use**: The official `multiplatform-settings-test` library provides `MapSettings`.

**No need to create your own** - the library already provides this:

```kotlin
// Add to build.gradle.kts commonTest dependencies
implementation("com.russhwolf:multiplatform-settings-test:1.3.0")
```

**Then use in Koin module**:
```kotlin
import com.russhwolf.settings.MapSettings

module {
    single<Settings> { MapSettings() }  // Official test implementation from library
    single { SettingsRepository(get()) }  // Real repository
}
```

`MapSettings` is a real `Settings` implementation backed by a `MutableMap`, specifically designed for testing.

---

## Test Base Classes

### KoinTestBase

**Purpose**: Automatic Koin lifecycle management.

```kotlin
abstract class KoinTestBase : KoinTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setupKoin() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(getTestModules())
        }
    }

    @AfterTest
    fun tearDownKoin() {
        stopKoin()
        Dispatchers.resetMain()
    }

    abstract fun getTestModules(): List<Module>
}
```

**Usage**:
```kotlin
class MyRepositoryTest : KoinTestBase() {
    override fun getTestModules() = listOf(
        TestModules.createTestDatabaseModule(),
        // ... other real implementations
    )
}
```

---

### DatabaseTestBase

**Purpose**: Automatic database cleanup between tests.

```kotlin
abstract class DatabaseTestBase : KoinTestBase() {

    protected val database: DatabaseHelper by inject()

    override fun getTestModules() = listOf(
        TestModules.createTestDatabaseModule()
    ) + additionalModules()

    open fun additionalModules(): List<Module> = emptyList()

    @BeforeTest
    fun clearDatabase() {
        database.deleteAllFeedSources()
        database.deleteAllFeedItems()
        database.deleteAllCategories()
    }
}
```

**Usage**:
```kotlin
class FeedActionsRepositoryTest : DatabaseTestBase() {
    override fun additionalModules() = listOf(
        module {
            single { FeedActionsRepository(get(), get(), get()) }
        }
    )

    private val repository: FeedActionsRepository by inject()

    @Test
    fun `test with clean database`() = runTest {
        // Database is automatically cleared before this test
    }
}
```

---

## Test Utilities

### FakeClock

**Purpose**: Control time in tests for deterministic behavior.

```kotlin
import kotlin.time.Clock
import kotlin.time.Instant

class FakeClock(
    private val instant: Instant,
) : Clock {
    override fun now(): Instant = instant

    companion object {
        val DEFAULT = FakeClock(Instant.parse("2025-06-15T10:30:00Z"))
    }
}
```

**Usage**:
```kotlin
@Test
fun `should format date correctly`() {
    val clock = FakeClock(Instant.parse("2025-01-15T14:30:00Z"))
    val formatter = DateFormatter(clock)
    
    val result = formatter.formatRelative(someTimestamp)
    result shouldBe "2 hours ago"
}
```

---

### TestDispatcherProvider

**Purpose**: Provide test dispatchers for coroutine testing.

```kotlin
object TestDispatcherProvider {
    val testDispatcher = StandardTestDispatcher()
}
```

**Usage**: Used in `TestModules` to provide consistent dispatchers for database operations and other async work.

---

### TestLogger

**Purpose**: Silent logger for tests that doesn't output to console.

```kotlin
// Used via Koin in TestModules.createTestLoggingModule()
val baseLogger = Logger(
    config = StaticConfig(logWriterList = emptyList()),
    tag = "FeedFlowTest",
)
```

**Usage**: Automatically injected via `TestModules.createTestLoggingModule()` - no manual setup needed.

---

## Property-Based Testing Guidelines

### When to Use Property-Based Tests

✅ **Use for**:
- Data transformations (mappers, parsers)
- CRUD operations (insert any valid input)
- Validation logic (test with all possible valid/invalid inputs)
- Pagination, filtering, sorting
- Mathematical operations
- String manipulation

❌ **Don't use for**:
- Complex business workflows (use example-based tests)
- Tests that require specific sequences of events
- UI interaction tests

### Writing Good Generators

**Keep generators simple and focused**:

```kotlin
object FeedSourceGenerator {
    val feedSourceArb = arbitrary {
        FeedSource(
            id = Arb.uuid().bind().toString(),
            title = Arb.string(5..100).bind(),
            url = Arb.domain().map { "https://$it/feed.xml" }.bind(),
            category = Arb.string(5..30).orNull(0.3).bind(),  // 30% chance of null
            lastSyncTimestamp = Arb.long(
                min = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
                max = System.currentTimeMillis()
            ).bind(),
        )
    }

    // Specialized generator for specific test cases
    fun unreadFeedItemArb() = arbitrary {
        feedItemArb.bind().copy(isRead = false)
    }
}
```

**Common Arb patterns**:
- `Arb.string(min..max)` - strings with length constraints
- `Arb.int(min..max)` - numbers in range
- `Arb.boolean()` - true/false
- `Arb.orNull(probability)` - nullable with probability
- `Arb.domain()` - valid domain names
- `Arb.uuid()` - UUIDs
- `.bind()` - sample from generator in arbitrary context

---

## Testing Best Practices

### 1. Test Names Should Describe Behavior

**Good**:
```kotlin
@Test
fun `should mark items as read when user scrolls past them`()

@Test
fun `should reload feeds when filter changes from all to unread`()
```

**Bad**:
```kotlin
@Test
fun testMarkAsRead()

@Test
fun test2()
```

---

### 2. Arrange-Act-Assert Pattern

Use the structure but DON'T add the Arrange, Act, Assert comments.

```kotlin
@Test
fun `should delete category and reset feed source categories`() = runTest {
    val category = CategoryGenerator.categoryArb.sample().value
    database.insertCategory(category)
    val feedSource = FeedSourceGenerator.feedSourceArb.sample().value.copy(
        category = category.name
    )
    database.insertFeedSource(feedSource)
        
    repository.deleteCategory(category.id)
        
    val deletedCategory = database.getCategoryById(category.id)
    deletedCategory.shouldBeNull()

    val updatedSource = database.getFeedSourceById(feedSource.id)
    updatedSource?.category.shouldBeNull()
}
```

---

### 3. Use kotlin tests assertions

**Bad**:
```kotlin
assertTrue(result != null)
assertEquals(10, result.feedItems.size)
assertEquals(true, result.isSuccess)
```

---

### 4. Test One Thing Per Test

**Good**:
```kotlin
@Test
fun `should insert feed source`()

@Test
fun `should retrieve feed source by id`()

@Test
fun `should update existing feed source`()
```

**Bad**:
```kotlin
@Test
fun `should handle all CRUD operations`() {
    // insert, retrieve, update, delete all in one test
}
```

---

### 5. Use Turbine for Flow Testing

```kotlin
@Test
fun `should emit loading then success states`() = runTest {
    viewModel.state.test {
        // Initial state
        val initial = awaitItem()
        initial.isLoading shouldBe false

        // Trigger action
        viewModel.refreshFeeds()

        // Loading state
        val loading = awaitItem()
        loading.isLoading shouldBe true

        // Success state
        val success = awaitItem()
        success.isLoading shouldBe false
        success.error.shouldBeNull()

        awaitComplete()
    }
}
```

---

## What NOT to Test

### Don't Test Framework Code

❌ **Don't test**:
- Koin module definitions
- Data class equality/toString
- Simple getters/setters
- Delegated properties

✅ **Do test**:
- Business logic
- Data transformations
- State management
- Integration between components

---

### Don't Test External Libraries

❌ **Don't test**:
- SQLDelight query generation
- Ktor HTTP client functionality
- Kotlinx serialization

✅ **Do test**:
- Your usage of these libraries
- Integration with your domain models
- Error handling around library calls

---

## Test Coverage Goals

### Coverage Targets

- **Critical paths**: 90-100% (repositories, ViewModels, sync logic)
- **Domain logic**: 80-90% (mappers, use cases)
- **UI layer**: 70-80% (ViewModels, state management)
- **Database**: 80-90% (queries, transactions)
- **Overall**: 70-80%

### What Drives Coverage

Focus on **behavior coverage**, not **line coverage**:
- All user-facing features tested
- All error paths tested
- All data transformations tested
- All integration points tested

---

## Running Tests

### Local Development

```bash
# Run all tests
./gradlew test -q --console=plain

# Run specific module
./gradlew shared:test -q --console=plain
./gradlew database:test -q --console=plain

# Run specific test class
./gradlew shared:test --tests "FeedStateRepositoryTest" -q --console=plain
```

### CI/CD

Tests should be fast and deterministic:
- Use in-memory databases
- Mock network calls
- Avoid Thread.sleep or delays
- Use TestDispatcher for coroutines
- No reliance on system time (use injected clock if needed)

---

## Summary: Decision Tree

When writing a test, ask:

1. **Is this an external dependency I can't control?**
   - YES → Use a fake (HTTP client, file system, cloud service)
   - NO → Use real implementation via Koin

2. **Am I testing domain logic?**
   - YES → Use real repository + real database
   - NO → Consider if test is needed

3. **Should I use property-based testing?**
   - Pure function with many inputs → YES
   - Complex stateful workflow → NO (use example-based)

4. **How do I set up dependencies?**
   - Use Koin test modules with real implementations
   - Only inject fakes for external boundaries

5. **How do I verify Flow/StateFlow?**
   - Use Turbine's `.test { }` API

---

## Maintenance

**Update this document when**:
- Adding new testing patterns
- Changing testing philosophy
- Adding new tools/libraries
- Finding better practices

**Review this document**:
- Before starting new test suites
- During code reviews for tests
- When onboarding new contributors

---

**Last Updated**: 2026-01-11
