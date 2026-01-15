# FeedFlow Testing Plan - Overview

## Current Status

✅ **Phase 0 Complete** - Full testing infrastructure in place (Commit: 82a6140)
- Dependencies: Kotest, Koin Test, Turbine, Settings Test, Ktor Mock
- Infrastructure: KoinTestBase, TestModules, in-memory drivers for all platforms
- Generators: FeedSource, FeedItem, Category, Account
- Sample tests demonstrating the pattern

## Testing Philosophy

### 1. Use Koin for Real Dependencies

Instead of mocking everything, inject real implementations via Koin test modules:
- Tests exercise actual code paths
- Catches integration issues early
- Only fake external boundaries (network, file system)

**Example:**
```kotlin
class FeedStateRepositoryTest : KoinTestBase() {
    override fun getTestModules() = listOf(
        TestModules.createTestDatabaseModule(),  // Real DB with in-memory driver
        module {
            single { FeedStateRepository(get(), get()) }  // Real repository
        }
    )

    private val repository: FeedStateRepository by inject()
}
```

### 2. Use Kotest Generators for Test Data

Replace manual fixtures with Kotest `Arb` generators for property-based testing:

**Benefits:**
- Generates hundreds of test cases with different inputs
- Finds edge cases we'd never think to write manually
- Less boilerplate than manual fixtures
- More comprehensive coverage

**Example:**
```kotlin
val feedSourceArb = arbitrary { rs ->
    FeedSource(
        id = Arb.uuid().bind(),
        title = Arb.string(10..50).bind(),
        url = Arb.domain().map { "https://$it/feed.xml" }.bind(),
        category = Arb.string(5..20).orNull().bind(),
    )
}

// Use in tests
checkAll(100, feedSourceArb) { feedSource ->
    // Test runs 100 times with different random inputs
    database.insertFeedSource(feedSource)
    val retrieved = database.getFeedSourceById(feedSource.id)
    retrieved shouldBe feedSource
}
```

### 3. Keep Provider E2E Tests for Later

Network-heavy sync provider tests (FreshRSS, Miniflux, Feedbin) will come in later phases after core business logic is solid.

### 4. KMP Compatibility

All tests run on `commonTest` to maximize coverage across platforms.

## Phase Breakdown

| Phase | Focus | Status |
|-------|-------|--------|
| **Phase 0** | Foundation - Dependencies, generators, Koin modules | ✅ Complete |
| **Phase 1** | Pure Logic - Utils, mappers, parsers | 🟡 Next |
| **Phase 2** | Database - CRUD, queries, sync metadata | 🔲 Not Started |
| **Phase 3** | Repositories - Business logic with Koin DI | 🔲 Not Started |
| **Phase 4** | ViewModels - UI state with Turbine | 🔲 Not Started |
| **Phase 5** | Sync Providers - Network mocking with Ktor | 🔲 Not Started |
| **Phase 6** | E2E - Optional Docker/manual tests | 🔲 Not Started |

## ✅ Dependencies Added (Phase 0)

All testing dependencies have been added and configured:

- **Kotest 6.0.7**: Assertions and property-based testing
- **Koin Test**: From Koin BOM 4.1.1
- **Turbine 1.2.0**: Flow testing
- **Multiplatform Settings Test**: MapSettings for in-memory settings
- **Ktor Client Mock**: Network mocking

See commit 82a6140 for implementation details.

## Recommended Execution Order

1. ✅ **Phase 0** (Foundation) - Required for everything else
2. **Phase 1** (Pure Logic) - Quick wins, builds confidence
3. **Phase 2** (Database) - Foundation for repositories
4. **Phase 3** (Repositories) - Core business logic
5. **Phase 4** (ViewModels) - UI state
6. **Phase 5** (Sync Providers) - Complex network tests
7. **Phase 6** (E2E) - Optional validation

Within each phase, tackle items marked **CRITICAL** first.

## Next Steps

✅ Phase 0 Complete - Testing infrastructure ready!

**Now**: Start **Phase 1: Pure Logic Tests** → See `02-pure-logic.md`
