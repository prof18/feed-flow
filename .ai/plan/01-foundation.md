# Phase 0: Foundation (Testing Infrastructure)

**Status**: ✅ COMPLETE (Commit: 82a6140)

**Goal**: Set up testing dependencies, utilities, and base classes that all other tests will use.

---

## ✅ Completion Summary

All tasks completed successfully:
- Dependencies added (Kotest 6.0.7, Koin Test, Turbine 1.2.0, Settings Test, Ktor Mock)
- Test directory structure created
- Kotest generators implemented (FeedSource, FeedItem, Category, Account)
- Koin test modules with real implementations
- Base test classes (KoinTestBase with test dispatcher support)
- In-memory driver factory for all platforms (Android/JVM/iOS)
- Sample tests validating the infrastructure

**Key Implementation Details**:
- `TestModules.createCompleteTestModule()` provides full DI setup for tests
- Kotest generators use property-based testing for comprehensive coverage
- Platform-specific `CurrentOS` and in-memory drivers for all targets
- iOS configured with `-lsqlite3` linker options

---

## Task 0.1: Add Dependencies

**Files to modify**:
- `libs.versions.toml`
- `shared/build.gradle.kts`

### Update libs.versions.toml

```toml
[versions]
kotest = "6.0.0"  # or latest
koin-test = "4.0.1"  # or latest matching your koin version
multiplatform-settings = "1.3.0"  # Should match your current version

[libraries]
kotest-assertions = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }
kotest-property = { module = "io.kotest:kotest-property", version.ref = "kotest" }
koin-test = { module = "io.insert-koin:koin-test", version.ref = "koin-test" }
multiplatform-settings-test = { module = "com.russhwolf:multiplatform-settings-test", version.ref = "multiplatform-settings" }
```

### Update shared/build.gradle.kts

```kotlin
kotlin {
    sourceSets {
        commonTest {
            dependencies {
                implementation(libs.kotest.assertions)
                implementation(libs.kotest.property)
                implementation(libs.koin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.multiplatform.settings.test)  // Official test utilities
            }
        }
    }
}
```

**Note**: `multiplatform-settings-test` provides `MapSettings` - a real `Settings` implementation backed by a `MutableMap`, specifically designed for testing. No need to create your own fake!

**Acceptance Criteria**:
- ✅ Build succeeds with `./gradlew build -q --console=plain`
- ✅ New dependencies are available in test code
- ✅ MapSettings from multiplatform-settings-test is available

---

## Task 0.2: Create Test Utilities Directory

**Location**: `shared/src/commonTest/kotlin/com/prof18/feedflow/shared/test/`

Create subdirectories:
```
shared/src/commonTest/kotlin/com/prof18/feedflow/shared/test/
├── generators/     # Kotest Arb generators for domain models
├── koin/          # Koin test modules and helpers
└── helpers/       # Test utilities and extensions
```

**Acceptance Criteria**:
- ✅ Directory structure exists

---

## Task 0.3: Create Kotest Generators for Core Models

Create generators for the main domain models using Kotest's property-based testing.

### FeedSource Generator

**File**: `shared/src/commonTest/kotlin/.../test/generators/FeedSourceGenerator.kt`

```kotlin
package com.prof18.feedflow.shared.test.generators

import com.prof18.feedflow.core.model.FeedSource
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.domain
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uuid

object FeedSourceGenerator {

    val feedSourceArb = arbitrary {
        FeedSource(
            id = Arb.uuid().bind().toString(),
            title = Arb.string(5..50).bind(),
            url = Arb.domain().map { "https://$it/feed.xml" }.bind(),
            category = Arb.string(5..20).orNull(0.3).bind(),
            lastSyncTimestamp = System.currentTimeMillis(),
            logoUrl = Arb.domain().map { "https://$it/logo.png" }.orNull(0.5).bind(),
        )
    }

    fun feedSourceWithCategory(category: String) = arbitrary {
        feedSourceArb.bind().copy(category = category)
    }
}
```

### FeedItem Generator

**File**: `shared/src/commonTest/kotlin/.../test/generators/FeedItemGenerator.kt`

```kotlin
package com.prof18.feedflow.shared.test.generators

import com.prof18.feedflow.core.model.FeedItem
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.domain
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uuid

object FeedItemGenerator {

    val feedItemArb = arbitrary {
        FeedItem(
            id = Arb.uuid().bind().toString(),
            feedSourceId = Arb.uuid().bind().toString(),
            title = Arb.string(10..100).bind(),
            url = Arb.domain().map { "https://$it/article/${Arb.uuid().bind()}" }.bind(),
            subtitle = Arb.string(50..500).orNull(0.3).bind(),
            content = Arb.string(100..2000).orNull(0.5).bind(),
            imageUrl = Arb.domain().map { "https://$it/image.jpg" }.orNull(0.4).bind(),
            pubDate = Arb.long(
                min = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,  // 30 days ago
                max = System.currentTimeMillis()
            ).bind(),
            isRead = Arb.boolean().bind(),
            isBookmarked = Arb.boolean().bind(),
        )
    }

    fun unreadFeedItemArb() = arbitrary {
        feedItemArb.bind().copy(isRead = false)
    }

    fun bookmarkedFeedItemArb() = arbitrary {
        feedItemArb.bind().copy(isBookmarked = true)
    }

    fun feedItemsForSource(feedSourceId: String, count: Int = 10) =
        List(count) { feedItemArb.sample().value.copy(feedSourceId = feedSourceId) }
}
```

### Category Generator

**File**: `shared/src/commonTest/kotlin/.../test/generators/CategoryGenerator.kt`

```kotlin
package com.prof18.feedflow.shared.test.generators

import com.prof18.feedflow.core.model.FeedSourceCategory
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uuid

object CategoryGenerator {

    val categoryArb = arbitrary {
        FeedSourceCategory(
            id = Arb.uuid().bind().toString(),
            name = Arb.string(5..30).bind(),
        )
    }
}
```

### Account Generator

**File**: `shared/src/commonTest/kotlin/.../test/generators/AccountGenerator.kt`

```kotlin
package com.prof18.feedflow.shared.test.generators

import com.prof18.feedflow.core.model.AccountType
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum

object AccountGenerator {

    val accountTypeArb = Arb.enum<AccountType>()
}
```

**Acceptance Criteria**:
- ✅ Can generate 100 random instances of each model: `feedSourceArb.sample().value`
- ✅ Generators compile without errors

---

## Task 0.4: Create Koin Test Module Helper

**File**: `shared/src/commonTest/kotlin/.../test/koin/TestModules.kt`

```kotlin
package com.prof18.feedflow.shared.test.koin

import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.account.AccountsRepository
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feed.FeedCategoryRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.test.createInMemoryDriver
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Test modules that provide REAL IMPLEMENTATIONS with in-memory or test-friendly configurations.
 *
 * Philosophy: Minimize fakes, use real implementations wherever possible.
 * Only external boundaries (HTTP, file system, cloud services) are faked.
 */
object TestModules {

    /**
     * Real DatabaseHelper with in-memory SQLite driver
     */
    fun createTestDatabaseModule(): Module = module {
        single { createInMemoryDriver() }
        single { DatabaseHelper(get()) }
    }

    /**
     * Real repositories - use with createTestDatabaseModule()
     */
    fun createTestRepositoriesModule(): Module = module {
        single { FeedStateRepository(get(), get()) }
        single { FeedActionsRepository(get(), get(), get()) }
        single { FeedSourcesRepository(get()) }
        single { FeedCategoryRepository(get()) }
        // Add other repositories as needed
    }

    /**
     * Real Settings implementation using MapSettings (in-memory)
     * From multiplatform-settings-test library
     *
     * MapSettings is provided by the multiplatform-settings-test artifact
     * specifically for testing. It's a real Settings implementation backed
     * by a MutableMap.
     */
    fun createTestSettingsModule(): Module = module {
        single<Settings> { MapSettings() }  // From multiplatform-settings-test
    }

    /**
     * Real AccountsRepository with configurable account type for tests
     */
    fun createTestAccountModule(): Module = module {
        single<Settings> { MapSettings() }  // From multiplatform-settings-test
        single {
            // Real AccountsRepository, starts with Local account by default
            AccountsRepository(get()).apply {
                // Can be configured in tests via switchAccount()
            }
        }
    }

    /**
     * Complete test setup for most repository tests
     * Includes: Database, Settings, Accounts, Repositories
     */
    fun createCompleteTestModule(): Module = module {
        includes(
            createTestDatabaseModule(),
            createTestSettingsModule(),
            createTestAccountModule(),
            createTestRepositoriesModule()
        )
    }
}
```

**Note**: We use `MapSettings` from the `multiplatform-settings-test` artifact. This is the official testing implementation provided by the library - a real Settings implementation backed by a MutableMap, perfect for tests.

**Acceptance Criteria**:
- ✅ All modules use real implementations
- ✅ MapSettings from multiplatform-settings-test library
- ✅ Real AccountsRepository with test configuration
- ✅ Modules compile without errors

---

## Task 0.5: Create Test Base Classes

### Koin Test Base

**File**: `shared/src/commonTest/kotlin/.../test/KoinTestBase.kt`

```kotlin
package com.prof18.feedflow.shared.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base class for tests that need Koin dependency injection
 */
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

    /**
     * Override to provide Koin modules for testing
     */
    abstract fun getTestModules(): List<Module>
}
```

### Database Test Base

**File**: `shared/src/commonTest/kotlin/.../test/DatabaseTestBase.kt`

```kotlin
package com.prof18.feedflow.shared.test

import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.koin.TestModules
import org.koin.core.module.Module
import org.koin.test.inject
import kotlin.test.BeforeTest

/**
 * Base class for tests that need database access
 */
abstract class DatabaseTestBase : KoinTestBase() {

    protected val database: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> = listOf(
        TestModules.createTestDatabaseModule()
    ) + additionalModules()

    /**
     * Override to add additional Koin modules
     */
    open fun additionalModules(): List<Module> = emptyList()

    @BeforeTest
    fun clearDatabase() {
        // Clear all tables before each test
        database.deleteAllFeedSources()
        database.deleteAllFeedItems()
        // Add other clear operations as needed
    }
}
```

**Acceptance Criteria**:
- ✅ Tests can extend these base classes
- ✅ Koin starts/stops correctly between tests
- ✅ Database clears between tests

---

## Task 0.6: Create In-Memory Driver Helper

**File**: `shared/src/commonTest/kotlin/.../test/InMemoryDriverFactory.kt`

```kotlin
package com.prof18.feedflow.shared.test

import app.cash.sqldelight.db.SqlDriver

/**
 * Creates an in-memory SQLite driver for testing
 * Platform-specific implementations in jvmTest, iosTest, androidUnitTest
 */
expect fun createInMemoryDriver(): SqlDriver
```

**File**: `shared/src/jvmTest/kotlin/.../test/InMemoryDriverFactory.jvm.kt`

```kotlin
package com.prof18.feedflow.shared.test

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.prof18.feedflow.database.FeedFlowDatabase

actual fun createInMemoryDriver(): SqlDriver {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    FeedFlowDatabase.Schema.create(driver)
    return driver
}
```

**Acceptance Criteria**:
- ✅ In-memory driver creates successfully
- ✅ Can create database with schema

---

## Summary

After completing Phase 0, you will have:

- ✅ Testing dependencies installed (Kotest, Koin Test)
- ✅ Kotest generators for all core models
- ✅ Koin test modules for DI in tests
- ✅ Base test classes for Koin and Database tests
- ✅ In-memory database driver factory

**Next Phase**: ✅ Phase 1 - Pure Logic Tests → See `02-pure-logic.md`

---

## Actual Implementation (Commit: 82a6140)

The implementation closely followed the plan with these refinements:

1. **Dependencies**: Used Kotest 6.0.7 and Koin from BOM 4.1.1
2. **Generators**:
   - FeedSource and FeedItem use full models (not simplified)
   - Added helper methods like `unreadFeedItemArb()`, `bookmarkedFeedItemArb()`
   - Used `kotlin.uuid.Uuid` for ID generation
3. **Test Modules**:
   - Included all major repositories in `createCompleteTestModule()`
   - Added proper logger configuration with `getWith()` helper
   - AccountsRepository includes all sync provider settings
4. **KoinTestBase**: Includes StandardTestDispatcher setup/teardown
5. **Platform Coverage**: Full Android, JVM, and iOS support with correct drivers
6. **Sample Tests**:
   - `KoinInfrastructureTest`: Validates DI works
   - `FeedFlowDatabaseTest`: Demonstrates database testing pattern with categories
