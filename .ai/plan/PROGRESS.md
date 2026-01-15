# Testing Progress Tracker

Use this file to track your progress through the testing roadmap.

---

## Overall Status

| Phase | Status | Progress |
|-------|--------|----------|
| **Phase 0** | ✅ Complete | 6/6 tasks |
| **Phase 1** | 🟡 In Progress | 2/4 tasks |
| **Phase 2** | ✅ Complete | 7/7 tasks |
| **Phase 3** | 🔲 Not Started | 0/6 tasks |
| **Phase 4** | 🔲 Not Started | 0/5 tasks |
| **Phase 5** | 🔲 Not Started | 0/5 tasks |
| **Phase 6** | 🔲 Not Started | 0/6 tasks |

**Legend**: 🔲 Not Started | 🟡 In Progress | ✅ Complete

---

## Phase 0: Foundation

- [x] **0.1**: Add dependencies (Kotest, Koin Test, Turbine)
- [x] **0.2**: Create test utilities directory structure
- [x] **0.3**: Create Kotest generators (FeedSource, FeedItem, Category, Account)
- [x] **0.4**: Create Koin test module helpers
- [x] **0.5**: Create test base classes (KoinTestBase, DatabaseTestBase)
- [x] **0.6**: Create in-memory driver factory

**Last Completed**: Phase 2 tasks 2.1-2.7 - DatabaseHelper CRUD, pagination, filters, mark read, cleanup, search, plus SyncedDatabaseHelper metadata tests.

**Current Task**: Phase 3 - Repository tests
**Blockers**: None

---

## Phase 1: Pure Logic Tests

- [x] **1.1**: Utils tests (UrlUtils, RetryHelpers, ArchiveIS)
- [x] **1.2**: Mapper tests (RssChannelMapper, GReader DTOs, Feedbin DTOs)
- [x] **1.3**: HtmlParser tests (JvmHtmlParserTest)
- [x] **1.4**: CSV and OPML edge cases

**Current Task**: Phase 2 - Database tests
**Blockers**: None

**Completed in commit bf9fd3e8**:
- `UrlUtilsTest.kt` - URL validation and manipulation tests
- `RetryHelpersTest.kt` - Retry logic with exponential backoff tests
- `ArchiveISTest.kt` - Archive.is URL generation tests
- `JvmHtmlParserTest.kt` - HTML parsing tests for JVM
- `OPMLFeedParserTest.kt` - Expanded OPML parsing tests

---

## Phase 2: Database Tests

- [x] **2.1**: DatabaseHelper CRUD tests
- [x] **2.2**: Pagination tests
- [x] **2.3**: Filter tests (read, bookmarked, category, source)
- [x] **2.4**: Mark read operations tests
- [x] **2.5**: Cleanup operations tests
- [x] **2.6**: Search tests
- [x] **2.7**: Sync metadata tests

**Current Task**: Phase 3 - Repository tests
**Blockers**: None

---

## Phase 3: Repository Tests

- [ ] **3.1**: Create test fakes (AccountsRepository, FeedParser, NetworkSettings)
- [ ] **3.2**: FeedStateRepository tests (CRITICAL)
- [ ] **3.3**: FeedActionsRepository tests (CRITICAL)
- [ ] **3.4**: FeedFetcherRepository tests (CRITICAL)
- [ ] **3.5**: FeedSourcesRepository tests
- [ ] **3.6**: FeedCategoryRepository tests

**Current Task**: -
**Blockers**: Phases 0-2 must complete first

---

## Phase 4: ViewModel Tests

- [ ] **4.1**: HomeViewModel tests (CRITICAL)
- [ ] **4.2**: AddFeedViewModel tests
- [ ] **4.3**: SearchViewModel tests
- [ ] **4.4**: ImportExportViewModel tests
- [ ] **4.5**: Sync ViewModels tests (FreshRSS, Miniflux, Feedbin, Bazqux)

**Current Task**: -
**Blockers**: Phases 0-3 must complete first

---

## Phase 5: Sync Provider Tests (Complex)

- [ ] **5.1**: GReaderClient tests (Ktor MockEngine)
- [ ] **5.2**: GReaderRepository tests
- [ ] **5.3**: FeedbinClient tests (Ktor MockEngine)
- [ ] **5.4**: FeedbinRepository tests
- [ ] **5.5**: Full sync integration tests

**Current Task**: -
**Blockers**: Phases 0-4 should complete first (optional phase)

---

## Phase 6: E2E Tests (Optional - Manual)

- [ ] **6.1**: Docker-based FreshRSS tests
- [ ] **6.2**: Docker-based Miniflux tests
- [ ] **6.3**: Bazqux contract tests (mock server)
- [ ] **6.4**: Feedbin tests (manual with credentials)
- [ ] **6.5**: Dropbox/Google Drive tests (fake data sources)
- [ ] **6.6**: iCloud tests (simulated)

**Current Task**: -
**Blockers**: Optional - run manually before major releases

---

## How to Use This File

1. **Check off tasks** as you complete them: `- [ ]` → `- [x]`
2. **Update current task** to show what you're working on
3. **Note blockers** if something is preventing progress
4. **Update status icons**:
   - 🔲 Not Started
   - 🟡 In Progress
   - ✅ Complete

---

## Quick Commands

```bash
# Run all tests
./gradlew test -q --console=plain

# Run specific module tests
./gradlew shared:test -q --console=plain
./gradlew database:test -q --console=plain
./gradlew feedSync:greader:test -q --console=plain

# Run with coverage (optional)
./gradlew koverHtmlReport

# Run detekt before committing
./gradlew detekt -q --console=plain
```

---

## Notes

- **Estimated time per phase**:
  - Phase 0: 2-3 hours
  - Phase 1: 3-4 hours
  - Phase 2: 4-5 hours
  - Phase 3: 5-6 hours
  - Phase 4: 4-5 hours
  - Phase 5: 6-8 hours
  - Phase 6: Variable (manual)

- **Priority**: Phases 0-4 provide the most value. Phase 5 is complex but important. Phase 6 is optional for release validation.

- **Coverage goal**: 70-80% code coverage with high confidence in critical paths.

---

## Implementation Notes

### Phase 0 Completion Summary (Commit: 82a6140)

Created comprehensive testing infrastructure with:

1. **Dependencies Added**:
   - Kotest 6.0.7 (assertions and property testing)
   - Koin Test (from Koin BOM 4.1.1)
   - Turbine 1.2.0 (Flow testing)
   - Multiplatform Settings Test
   - Ktor Client Mock

2. **Test Infrastructure**:
   - `KoinTestBase`: Base class providing Koin DI setup/teardown with test dispatcher
   - `TestModules`: Complete test module factory with real implementations
   - In-memory SQLite driver for all platforms (Android/JVM/iOS)
   - Platform-specific `CurrentOS` implementations for tests

3. **Kotest Generators**:
   - `FeedSourceGenerator`: Property-based test data for feed sources
   - `FeedItemGenerator`: Property-based test data for feed items with variants (unread, bookmarked, by source)
   - `CategoryGenerator`: Property-based test data for categories
   - `AccountGenerator`: Enum-based generator for sync accounts

4. **Platform Support**:
   - Android: JdbcSqliteDriver with Robolectric
   - JVM: JdbcSqliteDriver for desktop tests
   - iOS: Native in-memory driver

5. **Test Modules Philosophy**:
   - Use real implementations (DatabaseHelper, repositories)
   - Fake only external boundaries (network via Ktor MockEngine, settings via MapSettings)
   - Includes complete module factory for most test scenarios

6. **Sample Tests**:
   - `KoinInfrastructureTest`: Validates Koin DI works
   - `FeedFlowDatabaseTest`: Demonstrates database testing pattern

**Configuration**: SQLite driver linked with `-lsqlite3` on iOS, proper test source sets configured for all platforms.

---

### Phase 1 Progress Summary (Commit: bf9fd3e8)

Added pure logic tests and refactored test utilities:

1. **Utils Tests (Task 1.1 - Complete)**:
   - `UrlUtilsTest.kt`: URL validation, normalization, domain extraction
   - `RetryHelpersTest.kt`: Exponential backoff retry logic tests
   - `ArchiveISTest.kt`: Archive.is URL generation tests

2. **HtmlParser Tests (Task 1.3 - Complete)**:
   - `JvmHtmlParserTest.kt`: Comprehensive HTML parsing tests for JVM platform
     - Title extraction, content cleaning, image extraction
     - Edge cases: malformed HTML, empty content, special characters

3. **OPML Tests (Task 1.4 - Partial)**:
   - `OPMLFeedParserTest.kt`: Expanded with additional edge cases
   - Platform-specific tests for BOM handling (Android, iOS, JVM)

4. **Test Utilities Refactored**:
   - `FakeClock.kt`: New utility for time-dependent tests
   - `TestDispatcherProvider.kt`: Moved to test package
   - `TestLogger.kt`: Moved to test package

**Remaining for Phase 1**:
- Task 1.2: Mapper tests (RssChannelMapper, GReader DTOs, Feedbin DTOs)
- Task 1.4: Complete CSV edge cases

---

## Next Steps

**Current priority**: Complete Phase 1 → Mapper tests (Task 1.2)

1. **Immediate**: Add `RssChannelMapperTest.kt` for RSS feed mapping
2. **Then**: Add GReader DTO mapping tests
3. **Then**: Add Feedbin DTO mapping tests
4. **After Phase 1**: Begin Phase 2 → Database tests

See `02-pure-logic.md` for detailed instructions on remaining tasks.
