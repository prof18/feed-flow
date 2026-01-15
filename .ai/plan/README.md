# FeedFlow Testing Plan

**Welcome!** This directory contains a complete, step-by-step plan for adding comprehensive test coverage to FeedFlow.

---

## 🎯 Testing Philosophy: Real Implementations First

We prioritize **real implementations** connected via Koin dependency injection to stress test actual code paths. Fakes are only used for external boundaries we cannot control.

### What We Use Real

✅ All repositories (FeedStateRepository, FeedActionsRepository, etc.)
✅ All database operations (DatabaseHelper with in-memory SQLite)
✅ All domain logic (mappers, use cases)
✅ All settings (MapSettings from multiplatform-settings)
✅ All ViewModels

### What We Fake (External Boundaries Only)

❌ HTTP Client (Ktor MockEngine for API responses)
❌ File System (platform-specific file operations)
❌ Cloud Services (Dropbox, Google Drive, iCloud SDKs)

---

## 📚 Documentation

| Document | Purpose |
|----------|---------|
| **[TESTING-STRATEGY.md](../../docs/testing.md)** | Complete testing strategy, patterns, best practices - READ THIS FIRST! |
| **[PROGRESS.md](PROGRESS.md)** | Track your progress through the plan with checklists |
| **[00-overview.md](00-overview.md)** | High-level overview, dependencies, execution order |

---

## 🗺️ Plan Structure (7 Phases)

| Phase | File | Focus | Status |
|-------|------|-------|--------|
| **Phase 0** | [01-foundation.md](01-foundation.md) | Testing infrastructure (6 tasks) | ✅ Complete |
| **Phase 1** | [02-pure-logic.md](02-pure-logic.md) | Utils, mappers, parsers (4 tasks) | 🟡 In Progress (2/4) |
| **Phase 2** | [03-database.md](03-database.md) | Database CRUD, filters (7 tasks) | 🔲 Not Started |
| **Phase 3** | [04-repositories.md](04-repositories.md) | Business logic with Koin (6 tasks) | 🔲 Not Started |
| **Phase 4** | [05-viewmodels.md](05-viewmodels.md) | UI state with Turbine (5 tasks) | 🔲 Not Started |
| **Phase 5** | [06-sync-providers.md](06-sync-providers.md) | Network tests with MockEngine (5 tasks) | 🔲 Not Started |
| **Phase 6** | [07-e2e.md](07-e2e.md) | Optional manual E2E tests (6 tasks) | 🔲 Optional |

**Total**: 39 tasks across 6 active phases (Phase 6 is optional)

---

## 🚀 Getting Started

### Step 1: Read the Strategy

Open **[TESTING-STRATEGY.md](../../docs/testing.md)** to understand:
- Why we use real implementations
- What to fake and what not to fake
- Testing patterns and best practices
- Kotest property-based testing
- Koin test modules

### Step 2: Start Phase 0

Open **[01-foundation.md](01-foundation.md)** and complete Task 0.1:
- Add testing dependencies (Kotest, Koin Test, Turbine)
- Set up test infrastructure
- Create generators and base classes

### Step 3: Track Progress

Use **[PROGRESS.md](PROGRESS.md)** to:
- Check off completed tasks
- See overall progress
- Track blockers

### Step 4: Continue Through Phases

Work through phases 1-5 sequentially. Each phase builds on the previous one.

---

## 🔑 Key Tools & Libraries

| Tool | Purpose | Why |
|------|---------|-----|
| **Kotest Property** | Generate hundreds of test cases | Finds edge cases automatically |
| **Koin Test** | DI in tests | Use real implementations easily |
| **Turbine** | Test Kotlin Flows | Fluent API for StateFlow/Flow |
| **Ktor MockEngine** | Mock HTTP responses | Test network code without network |
| **SQLDelight In-Memory** | Test database | Real SQL queries, no persistence |
| **MapSettings** | In-memory settings | From multiplatform-settings-test library |

---

## 📊 Expected Coverage

After completing phases 0-4:
- **Critical paths**: 90-100% (repositories, ViewModels, sync logic)
- **Domain logic**: 80-90% (mappers, use cases)
- **UI layer**: 70-80% (ViewModels, state management)
- **Database**: 80-90% (queries, transactions)
- **Overall**: 70-80%

---

## ⏱️ Estimated Timeline

| Phase | Time | Priority |
|-------|------|----------|
| Phase 0 (Foundation) | 2-3 hours | **Required** |
| Phase 1 (Pure Logic) | 3-4 hours | **High** - Quick wins |
| Phase 2 (Database) | 4-5 hours | **High** |
| Phase 3 (Repositories) | 5-6 hours | **Critical** |
| Phase 4 (ViewModels) | 4-5 hours | **High** |
| Phase 5 (Sync Providers) | 6-8 hours | **Medium** - Complex |
| Phase 6 (E2E) | Variable | **Optional** - Manual validation |

**Total for core testing (Phases 0-4)**: ~20-25 hours

---

## 🎯 Quick Reference

### Run Tests

```bash
# All tests
./gradlew test -q --console=plain

# Specific module
./gradlew shared:test -q --console=plain
./gradlew database:test -q --console=plain

# Specific test class
./gradlew shared:test --tests "FeedStateRepositoryTest" -q --console=plain
```

### Test Structure Example

```kotlin
class FeedStateRepositoryTest : KoinTestBase() {

    // ALL REAL IMPLEMENTATIONS via Koin
    override fun getTestModules() = listOf(
        TestModules.createTestDatabaseModule(),  // Real DB
        module {
            single { FeedStateRepository(get(), get()) }  // Real repository
        }
    )

    private val repository: FeedStateRepository by inject()
    private val database: DatabaseHelper by inject()

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

---

## ❓ Common Questions

### Q: Why not use MockK or similar?

**A**: We prefer real implementations via Koin because:
1. Tests catch real integration bugs
2. Less maintenance (no keeping mocks in sync)
3. Refactoring confidence (breaking changes caught by tests)
4. Tests closer to production behavior

### Q: When should I fake something?

**A**: Only fake external boundaries you cannot control:
- Network calls (HTTP client)
- File system operations
- Cloud service SDKs (Dropbox, Google Drive, iCloud)
- Platform-specific APIs

### Q: What about test speed?

**A**: In-memory implementations are fast:
- In-memory SQLite: Fast as regular objects
- MapSettings: Just a HashMap
- TestDispatcher: No real delays

Tests should run in seconds, not minutes.

### Q: Can I use property-based testing for everything?

**A**: No, use it selectively:
- ✅ Pure functions with many inputs
- ✅ CRUD operations
- ✅ Validation logic
- ❌ Complex workflows (use example-based)
- ❌ Specific sequences of events

---

## 🤝 Contributing

When adding new tests:
1. **Read [TESTING-STRATEGY.md](../../docs/testing.md)** first
2. **Use real implementations** via Koin
3. **Fake only external boundaries**
4. **Use property-based testing** for pure functions
5. **Update [PROGRESS.md](PROGRESS.md)** when completing tasks

---

## 📝 Notes

- **Database infrastructure**: ✅ Already complete (in-memory drivers set up)
- **Dependencies to add**: Kotest, Koin Test, Turbine (Phase 0, Task 0.1)
- **Current coverage**: ~20-25 tests (utilities, OPML parsing)
- **Target coverage**: 70-80% after Phase 4

---

## 🎉 Ready to Start?

1. Open [TESTING-STRATEGY.md](../../docs/testing.md) - understand the philosophy
2. Open [01-foundation.md](01-foundation.md) - start with Phase 0
3. Open [PROGRESS.md](PROGRESS.md) - track your progress

**Let's build a robust test suite with real implementations!** 🚀
