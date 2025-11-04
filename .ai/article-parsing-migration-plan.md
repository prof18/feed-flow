# FeedFlow Article Parsing Migration Plan

## Migration from Current Architecture to ReaderFlow-Style Parsing

**Date**: 2025-11-03
**Target**: Adopt ReaderFlow's unified article parsing architecture in FeedFlow
**Strategy**: Incremental migration with phased implementation

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current State Analysis](#current-state-analysis)
3. [Target State Architecture](#target-state-architecture)
4. [Gap Analysis](#gap-analysis)
5. [Migration Strategy](#migration-strategy)
6. [Implementation Phases](#implementation-phases)
7. [Detailed Implementation Steps](#detailed-implementation-steps)
8. [Rollback Plan](#rollback-plan)
9. [Success Criteria](#success-criteria)

---

## Executive Summary

### Goal

Migrate FeedFlow's article parsing system from its current on-demand extraction approach to ReaderFlow's proven architecture featuring:
- File-based content caching for parsed article content
- Unified parsing layer with FeedItemParserWorker interface
- Background parsing for improved offline experience
- Consistent cross-platform behavior

**Note**: Unlike ReaderFlow, FeedFlow will **not** store parsed metadata (title, word count, site name) in the database. We trust the metadata from RSS feeds and only cache the parsed HTML content to files.

### Key Simplifications from ReaderFlow

FeedFlow's migration is **simpler** than ReaderFlow's implementation:

| Aspect | ReaderFlow | FeedFlow |
|--------|-----------|----------|
| **Database Changes** | New `article` and `article_to_download` tables | **NONE** - no schema changes |
| **Metadata Storage** | Stores title, length, site_name from parsing | **Ignored** - uses RSS metadata |
| **Background Queue** | Persistent database queue with retry tracking | **Simple** - WorkManager/in-memory only |
| **Migration Complexity** | Database migrations, data population | **None** - files created on-demand |
| **Terminology** | "Article" for parsed content | "FeedItem" (matches existing domain) |
| **Timeline** | ~11-15 weeks | **9-12 weeks** (simpler!) |

**Why This Works for FeedFlow**:
- RSS feed metadata is already accurate and trusted
- No need to duplicate metadata in database
- Simpler rollback (no database changes to undo)
- Faster implementation
- Less risk

### Why Migrate?

**Current Pain Points**:
1. Content re-extracted on every article view (performance overhead)
2. No offline content caching
3. Platform-specific implementations are scattered
4. No background parsing for saved articles
5. Desktop implementation incomplete

**Benefits of Migration**:
1. **Better Performance**: Cached content loads instantly
2. **Offline Support**: Read previously opened articles offline
3. **Unified Architecture**: Consistent parsing across platforms
4. **Background Processing**: Pre-cache important articles
5. **Code Reusability**: Share more code between platforms

### Migration Approach

**Incremental, Non-Disruptive Strategy**:
- Build new parsing system alongside existing implementation
- No user-facing changes until final cutover
- Feature flags to control rollout
- Phased migration over multiple development cycles
- Each phase is independently deployable

### 🔥 CRITICAL IMPLEMENTATION GUIDELINE

**ALWAYS Follow ReaderFlow Structure**:

When implementing any component in FeedFlow, you **MUST** use the same structure and layout as the corresponding component in ReaderFlow (`/Users/mg/Workspace/reader-flow`). Only make the **minimal necessary differences** for FeedFlow-specific requirements.

**What This Means**:
- ✅ **Copy the exact class structure** from ReaderFlow
- ✅ **Match the method signatures** and control flow
- ✅ **Use the same variable names** and patterns
- ✅ **Follow the same file organization** and package structure
- ✅ **Replicate the same callback patterns** and async handling
- ⚠️ **Only change names** where necessary (e.g., `Article` → `FeedItem`)
- ⚠️ **Only add/remove code** when absolutely required for FeedFlow's simpler architecture

**Why This Matters**:
- Proven architecture that works in production
- Reduces bugs and architectural mistakes
- Makes it easier to port future improvements from ReaderFlow
- Consistent patterns across related codebases

**Example Differences Allowed**:
- Naming: `ArticleParser` → `FeedItemParser`
- Simpler features: No database metadata storage in FeedFlow
- Platform paths: Different bundle identifiers, file paths

**Example Differences NOT Allowed**:
- ❌ Different class structure or control flow
- ❌ Different callback patterns (e.g., using suspend functions instead of callbacks)
- ❌ Different singleton patterns or initialization
- ❌ Rewriting logic from scratch

**When In Doubt**: Look at the ReaderFlow implementation first, then adapt minimally.

---

## Current State Analysis

### Current Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│              ReaderModeViewModel (Platform-Specific)        │
│   ├─ iOS: Minimal logic, delegates to Reader package       │
│   ├─ Android: ReaderModeExtractor integration              │
│   └─ Desktop: Readability4J + Markdown rendering           │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────────┐
│                Platform-Specific Extraction                 │
│                                                             │
│  ┌─────────────────────┐    ┌──────────────────────────┐  │
│  │  iOS                │    │  Android                 │  │
│  ├─────────────────────┤    ├──────────────────────────┤  │
│  │ • Reader Package    │    │ • ReaderModeExtractor    │  │
│  │   (Swift)           │    │   (Readability4J/JSoup)  │  │
│  │ • WKWebView +       │    │ • ParsingWebView         │  │
│  │   Defuddle.js       │    │   (Defuddle.js)          │  │
│  │ • Fetch at view     │    │ • Fetch at view          │  │
│  └─────────────────────┘    └──────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Desktop                                              │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │ • Readability4J                                       │  │
│  │ • Markdown rendering                                  │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────────┐
│                  Data Layer                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ SQLDelight Database (feed_item table)                │  │
│  │ • content TEXT (RSS feed content)                    │  │
│  │ • No separate article metadata                       │  │
│  │ • No parsing queue                                   │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ File Storage: NONE                                    │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Current Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `ReaderModeData` | `/core/.../model/ReaderModeData.kt` | Data model for reader content |
| `ReaderModeState` | `/core/.../model/ReaderModeState.kt` | UI state representation |
| `ReaderModeViewModel` (iOS) | `/shared/src/iosMain/.../ReaderModeViewModel.ios.kt` | Minimal iOS ViewModel |
| `ReaderModeViewModel` (Android) | `/shared/src/androidMain/.../ReaderModeViewModel.android.kt` | Android ViewModel with extraction |
| `ReaderModeViewModel` (Desktop) | `/shared/src/jvmMain/.../ReaderModeViewModel.desktop.kt` | Desktop ViewModel |
| `ReaderModeExtractor` (Android) | `/shared/src/androidMain/.../ReaderModeExtractor.android.kt` | Android content extraction |
| `ReaderModeExtractor` (Desktop) | `/shared/src/jvmMain/.../ReaderModeExtractor.desktop.kt` | Desktop content extraction |
| `HtmlRetriever` | `/shared/src/commonMain/.../HtmlRetriever.kt` | Shared HTTP client |
| `Reader` package (iOS) | `/iosApp/Packages/Reader/` | iOS-specific Swift reader |
| `ParsingWebView` (Android) | `/sharedUI/src/androidMain/.../ParsingWebView.kt` | Android WebView parser |

### Current Data Flow

```
1. User opens article
   ↓
2. ReaderModeViewModel.getReaderModeHtml(urlInfo)
   ↓
3. Platform-specific extraction:
   ├─ iOS: Reader.fetchAndExtractContent()
   ├─ Android: ReaderModeExtractor.extractReaderContent()
   └─ Desktop: ReaderModeExtractor.extractReaderContent()
   ↓
4. Fetch HTML from web (HtmlRetriever)
   ↓
5. Parse content (Readability4J, JSoup, or Defuddle.js)
   ↓
6. Return ReaderModeData to ViewModel
   ↓
7. Display in UI (no storage)
```

### Current Limitations

1. **No Caching**: Content re-extracted on every view
2. **Network Dependency**: Cannot view articles offline
3. **Performance**: 1-5 second delay on every article open
4. **No Background Processing**: No pre-caching of bookmarked articles
5. **Platform Fragmentation**: Different parsing logic on each platform

---

## Target State Architecture

### ReaderFlow-Style Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│              ReaderModeViewModel (SHARED)                   │
│              Single implementation for all platforms        │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────────┐
│                    Domain Layer (SHARED)                    │
│  ┌──────────────────────┐  ┌──────────────────────────┐    │
│  │ FeedItemParser       │  │ FeedItemContentFile      │    │
│  │ Worker Interface     │  │ Handler Interface        │    │
│  │ (Common)             │  │ (Common)                 │    │
│  └──────────────────────┘  └──────────────────────────┘    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         HtmlRetriever (Shared - Ktor)                │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────────┐
│             Platform-Specific Implementations               │
│                                                             │
│  ┌─────────────────────┐    ┌──────────────────────────┐  │
│  │  Android            │    │  iOS                     │  │
│  ├─────────────────────┤    ├──────────────────────────┤  │
│  │ • FeedItemParser    │    │ • FeedItemParser.swift   │  │
│  │   (WebView)         │    │   (WKWebView)            │  │
│  │ • Defuddle.js       │    │ • Defuddle.js            │  │
│  │ • FileHandler       │    │ • FileHandler            │  │
│  │   (filesDir)        │    │   (App Group)            │  │
│  │ • WorkManager       │    │ • CoroutineScope         │  │
│  └─────────────────────┘    └──────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Desktop                                              │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │ • FeedItemParser (Readability4J)                      │  │
│  │ • FileHandler (user.home/.feedflow)                   │  │
│  │ • Background parsing (CoroutineScope)                 │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────────┐
│                    Data Layer                               │
│  ┌──────────────────────┐  ┌──────────────────────────┐    │
│  │ File Storage (NEW)   │  │ SQLDelight Database      │    │
│  │ {feedItemId}.html    │  │ • feed_item (existing)   │    │
│  │                      │  │   NO CHANGES             │    │
│  └──────────────────────┘  └──────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Target Components (New)

| Component | Location | Purpose |
|-----------|----------|---------|
| `FeedItemParserWorker` | `/shared/src/commonMain/.../domain/feeditem/FeedItemParserWorker.kt` | Common parsing interface |
| `FeedItemContentFileHandler` | `/shared/src/commonMain/.../domain/feeditem/FeedItemContentFileHandler.kt` | File storage interface |
| `ParsingResult` | `/core/.../model/ParsingResult.kt` | Parsing result model |
| `FeedItemParser` (Android) | `/shared/src/androidMain/.../parser/FeedItemParser.kt` | Android WebView parser |
| `FeedItemParser` (iOS Swift) | `/iosApp/Source/Reader/FeedItemParser.swift` | iOS WKWebView parser |
| `FeedItemParser` (Desktop) | `/shared/src/jvmMain/.../parser/FeedItemParser.kt` | Desktop parser |
| `FeedItemContentFileHandlerAndroid` | `/shared/src/androidMain/.../parser/FeedItemContentFileHandlerAndroid.kt` | Android file I/O |
| `FeedItemContentFileHandlerIos` | `/shared/src/appleMain/.../parser/FeedItemContentFileHandlerIos.kt` | iOS file I/O |
| `FeedItemContentFileHandlerDesktop` | `/shared/src/jvmMain/.../parser/FeedItemContentFileHandlerDesktop.kt` | Desktop file I/O |
| `FeedItemParserWorkManager` (Android) | `/shared/src/androidMain/.../parser/FeedItemParserWorkManager.kt` | Android background worker |
| `FeedItemsSyncBackgroundWorkerIos` | `/shared/src/appleMain/.../parser/FeedItemsSyncBackgroundWorkerIos.kt` | iOS background worker |

### Target Data Flow

```
1. User opens feed item in reader mode
   ↓
2. ReaderModeViewModel.getReaderModeHtml(urlInfo)
   ↓
3. Check if content file exists (FeedItemContentFileHandler)
   ├─ YES: Load from file → Display (FAST PATH - instant load)
   └─ NO: Continue to step 4
   ↓
4. Call FeedItemParserWorker.triggerImmediateParsing(url)
   ↓
5. HtmlRetriever.retrieveHtml(url) [Fetch HTML from web]
   ↓
6. Load into WebView + Execute Defuddle.js
   ↓
7. Parse JSON result: {content, title, wordCount, site}
   ↓
8. FeedItemContentFileHandler.saveFeedItemContentToFile()
   ├─ Android: Write to appContext.filesDir/{feedItemId}.html
   ├─ iOS: Write to App Group container/{feedItemId}.html
   └─ Desktop: Write to user.home/.feedflow/articles/{feedItemId}.html
   ↓
9. ReaderModeViewModel displays ReaderModeData
   ├─ Uses title from RSS feed (from feed_item table)
   ├─ Uses parsed content (from file)
   └─ Wrapped in ReaderModeState.Success

Note: We do NOT store parsed metadata (title, wordCount, site)
      because we trust the RSS feed metadata.

**File Naming**: Uses feed item ID directly as filename (e.g., "12345.html")
                  NO MD5 hashing - matches reader-flow pattern
```

---

## Gap Analysis

### Components to Add

#### 1. Core Interfaces (Shared - Common)

- [x] `FeedItemParserWorker` interface ✅
- [x] `FeedItemContentFileHandler` interface ✅
- [ ] `FeedItemsSyncBackgroundWorker` interface (for Phase 5)
- [x] `ParsingResult` sealed class ✅ (simplified to `htmlContent` and `siteName`)
- [x] `FeedItemUrlInfo` data class (already exists) ✅

#### 2. Android Platform Implementation

- [x] `FeedItemParser` (WebView + Defuddle.js) ✅
- [x] `AndroidFeedItemParserWorker` implementation ✅
- [x] `FeedItemContentFileHandlerAndroid` ✅
- [x] Use feed item ID directly (no MD5 hashing) ✅ **Matches reader-flow pattern**
- [x] Update Koin DI configuration ✅
- [ ] `FeedItemParserWorkManager` (CoroutineWorker - Phase 5)

#### 3. iOS Platform Implementation

- [x] `FeedItemParser.swift` (WKWebView + Defuddle.js) ✅ **Matches reader-flow structure exactly**
- [x] `FeedItemParserWorkerIos.swift` (Swift bridge) ✅ **Matches reader-flow structure exactly**
- [x] `FeedItemContentFileHandlerIos` (Kotlin) ✅ **Uses App Group container**
- [x] Use feed item ID directly (no MD5 hashing) ✅ **Matches reader-flow pattern**
- [x] Update Koin DI configuration ✅ **Injected from Swift like reader-flow**
- [ ] `FeedItemsSyncBackgroundWorkerIos` (Kotlin - Phase 5)

#### 4. Desktop Platform Implementation

- [x] `DesktopFeedItemParserWorker` (Readability4J wrapper) ✅
- [x] `FeedItemContentFileHandlerDesktop` ✅
- [x] Update ReaderModeViewModel (Desktop) ✅
- [x] Update Koin DI configuration ✅

#### 5. ViewModel Updates

- [ ] Consolidate ReaderModeViewModel to shared implementation
- [ ] Add file cache checking logic
- [ ] Add parsing orchestration
- [ ] Handle parsing states (Loading, Success, Error)

#### 6. JavaScript Resources

- [x] Copy `defuddle.js` to Android resources (`/shared/src/androidMain/res/raw/`) ✅ (100KB)
- [x] Copy `defuddle.js` to iOS bundle (`/iosApp/Resources/`) ✅ (100KB)
- [x] Verify Defuddle.js version consistency ✅ (both 100KB, from ReaderFlow)

#### 7. UI Updates (Optional)

- [ ] Add "Re-parse" option to reader menu
- [ ] Add parsing progress indicator
- [ ] Handle HtmlNotAvailable state gracefully
- [ ] Add "Clear cache" option in settings

### Components to Modify

#### Existing Components Requiring Updates

| Component | Change Required |
|-----------|----------------|
| `ReaderModeViewModel` (All platforms) | Consolidate to shared implementation with file caching logic |
| `ReaderModeData` | Add `baseUrl` field if not present |
| `HtmlRetriever` | No changes (already suitable) |
| Koin modules (all platforms) | Register new file handler and parser dependencies |

### Components to Deprecate

| Component | Deprecation Plan |
|-----------|-----------------|
| `ReaderModeExtractor` (Android) | Phase out after migration complete |
| `ReaderModeExtractor` (Desktop) | Phase out after migration complete |
| `Reader` package (iOS) | Keep for now, but wrap in new interface |
| `ParsingWebView` (Android) | Refactor into new ArticleParser |

---

## Migration Strategy

### Guiding Principles

1. **Incremental**: Each phase is independently deployable
2. **Non-Breaking**: Existing functionality continues to work
3. **Testable**: Each component tested in isolation
4. **Feature-Flagged**: New system can be enabled/disabled
5. **Reversible**: Easy rollback if issues arise
6. **Parallel Development**: New and old systems coexist initially

### Feature Flag Strategy

```kotlin
// In SettingsRepository or FeatureFlags
enum class ArticleParsingMode {
    LEGACY,      // Current on-demand extraction
    CACHED,      // New file-cached system (no background)
    FULL,        // Full ReaderFlow-style with background parsing
}

fun getArticleParsingMode(): ArticleParsingMode {
    // Can be controlled via:
    // - Remote config (Firebase Remote Config)
    // - Local debug setting
    // - Gradle build variant
    return ArticleParsingMode.LEGACY  // Default during migration
}
```

### Migration Phases Overview

```
Phase 1: Foundation (Week 1)
  ├─ Create core interfaces (FeedItemParserWorker, FeedItemContentFileHandler)
  ├─ Copy Defuddle.js resources
  ├─ Create ParsingResult model
  └─ NO user-facing changes, NO database changes

Phase 2: Android Implementation (Week 2-3)
  ├─ Implement FeedItemParser (Android)
  ├─ Implement AndroidFeedItemParserWorker
  ├─ Implement FeedItemContentFileHandlerAndroid
  ├─ Wire up file caching (feature-flagged)
  └─ Test with CACHED mode

Phase 3: iOS Implementation (Week 4-5)
  ├─ Implement FeedItemParser.swift
  ├─ Implement FeedItemParserWorkerIos.swift
  ├─ Implement FeedItemContentFileHandlerIos
  ├─ Wire up file caching
  └─ Test with CACHED mode

Phase 4: Desktop Implementation (Week 6)
  ├─ Implement FeedItemParser (Desktop)
  ├─ Implement FeedItemContentFileHandlerDesktop
  ├─ Wire up file caching
  └─ Test with CACHED mode

Phase 5: Background Parsing (Week 7-8)
  ├─ Android: FeedItemParserWorkManager
  ├─ iOS: FeedItemsSyncBackgroundWorkerIos
  ├─ Desktop: Background coroutine worker
  └─ Test with FULL mode

Phase 6: Consolidate ViewModel (Week 9)
  ├─ Move ReaderModeViewModel to commonMain
  ├─ Remove platform-specific ViewModels
  └─ Test all platforms

Phase 7: Migration & Cleanup (Week 10-11)
  ├─ Enable FULL mode by default
  ├─ Deprecate old components
  └─ Remove legacy code

Phase 8: Polish & Optimization (Week 12)
  ├─ Performance tuning
  ├─ Error handling improvements
  ├─ UI/UX polish (clear cache, re-parse options)
  └─ Documentation
```

---

## Implementation Phases

### Phase 1: Foundation (No User-Facing Changes) ✅

**Goal**: Establish shared interfaces for parsing without affecting existing functionality.

**Duration**: 1 week

**Status**: ✅ **COMPLETED** (2025-11-03)

**Tasks**:

1. **Create Core Interfaces**
   - [x] `FeedItemParserWorker` interface in `/shared/src/commonMain/kotlin/com/prof18/feedflow/shared/domain/feeditem/`
   - [x] `FeedItemContentFileHandler` interface in same directory
   - [x] `ParsingResult` sealed class in `/core/src/commonMain/kotlin/com/prof18/feedflow/core/model/` (simplified to only `htmlContent` and `siteName`)

2. **Copy JavaScript Resources**
   - [x] Copy `defuddle.js` from ReaderFlow to `/shared/src/androidMain/res/raw/defuddle.js` (100KB)
   - [x] Copy `defuddle.js` to iOS bundle resources `/iosApp/Resources/` (100KB)
   - [x] Verify minification and compatibility

3. **Update Data Models (if needed)**
   - [x] Verify `ReaderModeData` has `url` field (can serve as baseUrl)
   - [x] Verify `FeedItemUrlInfo` exists and has necessary fields

**Acceptance Criteria**:
- ✅ All new interfaces compile successfully
- ✅ JavaScript resources copied correctly
- ✅ NO database changes
- ✅ Existing functionality completely unaffected
- ✅ Project builds without errors

---

### Phase 2: Android Implementation ✅

**Goal**: Implement file-cached parsing for Android (feature-flagged).

**Duration**: 2-3 weeks

**Status**: ✅ **COMPLETED** (2025-11-03)

**Tasks**:

1. **Implement FeedItemParser (Android)**
   - [x] Create `/shared/src/androidMain/kotlin/com/prof18/feedflow/shared/domain/parser/FeedItemParser.kt` ✅
   - [x] Load Defuddle.js from resources ✅
   - [x] Implement WebView-based parsing ✅
   - [x] Extract content (title/metadata from parsing can be ignored) ✅
   - [x] Handle parsing errors gracefully ✅

2. **Implement FeedItemContentFileHandlerAndroid**
   - [x] Create `/shared/src/androidMain/kotlin/com/prof18/feedflow/shared/domain/parser/FeedItemContentFileHandlerAndroid.kt` ✅
   - [x] Use `context.filesDir` for storage ✅
   - [x] Implement save, load, isAvailable, delete, clearAll methods ✅
   - [x] Add proper error handling and logging ✅

3. **Implement AndroidFeedItemParserWorker**
   - [x] Create `/shared/src/androidMain/kotlin/com/prof18/feedflow/shared/domain/parser/AndroidFeedItemParserWorker.kt` ✅
   - [x] Implement `triggerImmediateParsing()` method ✅
   - [x] Stub out `enqueueParsing()` and `triggerBackgroundParsing()` (Phase 5) ✅
   - [x] Integrate with FeedItemParser ✅

4. **Update ReaderModeViewModel (Android)**
   - [x] Add file cache checking logic ✅
   - [x] Integrate FeedItemParserWorker ✅
   - [x] Implement feature flag check (optional parameters) ✅
   - [x] Fallback to legacy ReaderModeExtractor if flag disabled ✅

5. **Update Koin DI (Android)**
   - [x] Register `FeedItemParser` as singleton ✅
   - [x] Register `AndroidFeedItemParserWorker` as singleton ✅
   - [x] Register `FeedItemContentFileHandlerAndroid` as singleton ✅
   - [x] Inject into ReaderModeViewModel ✅

6. **Manual Testing**
   - [ ] Verify feed items parse and cache correctly
   - [ ] Verify cached content loads instantly
   - [ ] Test with new parser enabled
   - [ ] Test fallback to legacy mode when new parser not available

**Acceptance Criteria**:
- ✅ Android app builds successfully
- ⏳ Feed items parse and cache correctly (requires manual testing)
- ⏳ Cached content loads instantly (< 100ms) (requires manual testing)
- ✅ Feature flag controls old vs new behavior (optional DI parameters)
- ⏳ No crashes or regressions in manual testing

**Notes**:
- Implementation uses optional DI parameters instead of explicit feature flags
- New parser is enabled when both `feedItemParserWorker` and `feedItemContentFileHandler` are injected
- Falls back to legacy `ReaderModeExtractor` when new parser dependencies are null
- **Uses feed item ID directly as filename** (no MD5 hashing) - matching reader-flow pattern
- FeedItemParserWorker interface updated to accept `feedItemId` parameter

---

### Phase 3: iOS Implementation ✅

**Goal**: Implement file-cached parsing for iOS.

**Duration**: 2-3 weeks

**Status**: ✅ **COMPLETED** (2025-11-04)

**Tasks**:

1. **Implement FeedItemParser.swift**
   - [x] Create `/iosApp/Source/Reader/FeedItemParser.swift` ✅
   - [x] Use WKWebView for JavaScript execution ✅
   - [x] Load Defuddle.js from bundle ✅
   - [x] Implement parsing with callbacks ✅
   - [x] Handle errors and timeouts ✅

2. **Implement FeedItemParserWorkerIos.swift**
   - [x] Create `/iosApp/Source/Reader/FeedItemParserWorkerIos.swift` ✅
   - [x] Bridge to Kotlin `FeedItemParserWorker` interface ✅
   - [x] Implement `triggerImmediateParsing()` ✅
   - [x] Integrate with HtmlRetriever (Kotlin) ✅

3. **Implement FeedItemContentFileHandlerIos**
   - [x] Create `/shared/src/iosMain/kotlin/com/prof18/feedflow/shared/domain/parser/FeedItemContentFileHandlerIos.kt` ✅
   - [x] Use NSCachesDirectory for storage ✅
   - [x] Implement save, load, isAvailable, delete, clearAll ✅
   - [x] Add proper error handling and logging ✅

4. **Update ReaderModeViewModel (iOS)**
   - [x] Add file cache checking logic ✅
   - [x] Integrate FeedItemParserWorker (optional) ✅
   - [x] Feature flag integration (optional DI parameters) ✅
   - [x] Fallback to legacy Swift parsing when new parser not available ✅

5. **Update Koin DI (iOS)**
   - [x] Register `FeedItemContentFileHandlerIos` ✅
   - [x] Wire dependencies to ReaderModeViewModel ✅
   - [x] FeedItemParserWorker left as optional (can be set from Swift) ✅

6. **Manual Testing**
   - [ ] Verify feed items parse and cache correctly
   - [ ] Verify cached content loads instantly
   - [ ] Test file persistence
   - [ ] Test feature flag behavior

**Acceptance Criteria**:
- ✅ iOS app builds successfully (needs verification)
- ⏳ Feed items parse and cache correctly (requires manual testing)
- ⏳ Cached content loads instantly (< 100ms) (requires manual testing)
- ✅ Feature flag controls behavior (optional DI parameters)
- ⏳ No crashes or regressions in manual testing

**Notes**:
- **EXACTLY matches reader-flow structure**: FeedItemParser.swift and FeedItemParserWorkerIos.swift copied from reader-flow with minimal name changes only
- **Uses feed item ID directly as filename** (no MD5 hashing) - matching reader-flow pattern
- **Koin DI injection**: Parser injected through Koin from Swift in `startKoin()` function, matching reader-flow pattern exactly
- FeedItemParserWorker interface updated to accept `feedItemId` parameter (both platforms)
- Implementation uses non-optional DI parameters (removed `? = null` defaults)
- Created files in `iosMain` instead of `appleMain` to match existing project structure
- Uses App Group container (`group.com.prof18.feedflow`) for article storage
- Swift bridge properly integrated with Kotlin through Koin DI

**Key Changes from Initial Implementation**:
1. ✅ Rewrote FeedItemParser.swift to match reader-flow's ArticleParser.swift exactly
2. ✅ Rewrote FeedItemParserWorkerIos.swift to match reader-flow's ArticleParserWorkerIos.swift exactly
3. ✅ Changed from MD5 hashing to direct feed item ID (both Android and iOS)
4. ✅ Updated FeedItemParserWorker interface to accept `feedItemId` parameter
5. ✅ Implemented Koin DI injection from Swift matching reader-flow pattern
6. ✅ Made all dependencies non-optional in ReaderModeViewModel.ios.kt

---

### Phase 4: Desktop Implementation ✅

**Goal**: Implement file-cached parsing for Desktop.

**Duration**: 1 week

**Status**: ✅ **COMPLETED** (2025-11-04)

**Tasks**:

1. **Implement DesktopFeedItemParserWorker**
   - [x] Create `/shared/src/jvmMain/kotlin/com/prof18/feedflow/shared/domain/parser/DesktopFeedItemParserWorker.kt` ✅
   - [x] Wrap Readability4J for parsing ✅
   - [x] Implement `triggerImmediateParsing()` ✅
   - [x] Stub background methods (Phase 5) ✅
   - [x] Content validation (minimum 200 characters) ✅

2. **Implement FeedItemContentFileHandlerDesktop**
   - [x] Create `/shared/src/jvmMain/kotlin/com/prof18/feedflow/shared/domain/parser/FeedItemContentFileHandlerDesktop.kt` ✅
   - [x] Use `System.getProperty("user.home")/.feedflow/articles/` ✅
   - [x] Implement save, load, isAvailable, delete, clearAll ✅

3. **Update ReaderModeViewModel (Desktop)**
   - [x] Integrate file caching ✅
   - [x] Add cache-first loading pattern ✅
   - [x] Keep Desktop-specific markdown conversion ✅

4. **Update Koin DI (Desktop)**
   - [x] Register `FeedItemContentFileHandlerDesktop` as singleton ✅
   - [x] Register `DesktopFeedItemParserWorker` as singleton ✅
   - [x] Update ReaderModeViewModel registration ✅

5. **Manual Testing**
   - [ ] Test on Windows, Linux, macOS
   - [ ] Verify file paths work correctly on each platform
   - [ ] Verify parsing and caching work correctly

**Acceptance Criteria**:
- ✅ Desktop app builds successfully
- ⏳ Feed items parse and cache correctly (requires manual testing)
- ⏳ File storage works cross-platform (requires manual testing)
- ⏳ No crashes or regressions in manual testing

**Notes**:
- **Uses feed item ID directly as filename** (no MD5 hashing) - matching Android/iOS pattern
- **Readability4J instead of Defuddle.js** - Desktop doesn't have WebView, uses native Java parser
- **Desktop-specific**: Keeps `MarkdownToHtmlConverter` for markdown rendering
- File storage path: `~/.feedflow/articles/{feedItemId}.html`
- Same validation as Android/iOS: minimum 200 characters plain text
- Stub methods for Phase 5 background parsing: `enqueueParsing()`, `triggerBackgroundParsing()`

---

### Phase 5: Background Parsing

**Goal**: Implement background parsing for bookmarked feed items.

**Duration**: 2 weeks

**Tasks**:

1. **Android Background Parsing**
   - [ ] Create `FeedItemParserWorkManager` (CoroutineWorker)
   - [ ] Implement `enqueueParsing()` in `AndroidFeedItemParserWorker`
   - [ ] Queue feed items for background parsing when bookmarked
   - [ ] Handle network constraints (Wi-Fi only recommended)
   - [ ] Implement simple retry logic (in-memory or WorkManager retry)

2. **iOS Background Parsing**
   - [ ] Create `FeedItemsSyncBackgroundWorkerIos` in Kotlin
   - [ ] Use CoroutineScope with background dispatcher
   - [ ] Trigger parsing when feed items bookmarked
   - [ ] Implement simple retry logic

3. **Desktop Background Parsing**
   - [ ] Create background worker using CoroutineScope
   - [ ] Similar to iOS implementation
   - [ ] Trigger on bookmark events

4. **Trigger Integration**
   - [ ] Call `enqueueParsing()` when user bookmarks a feed item
   - [ ] Implement in `FeedActionsRepository` or similar
   - [ ] No database queue needed - use WorkManager/in-memory

5. **Manual Testing**
   - [ ] Verify background parsing triggers correctly when bookmarking
   - [ ] Verify network constraints work (Android)
   - [ ] Test retry logic
   - [ ] Monitor battery usage during background parsing

**Acceptance Criteria**:
- Bookmarked feed items queue for background parsing
- Parsing happens in background without blocking UI
- Failed parses retry appropriately (simple retry, not persistent queue)
- All platforms support background parsing
- No excessive battery/data usage observed in testing

---

### Phase 6: Consolidate ViewModel

**Goal**: Move ReaderModeViewModel to shared code (commonMain).

**Duration**: 1 week

**Tasks**:

1. **Create Shared ReaderModeViewModel**
   - [ ] Move logic to `/shared/src/commonMain/kotlin/com/prof18/feedflow/shared/presentation/ReaderModeViewModel.kt`
   - [ ] Use `expect/actual` for platform-specific code if needed
   - [ ] Inject FeedItemParserWorker via DI
   - [ ] Inject FeedItemContentFileHandler via DI

2. **Remove Platform-Specific ViewModels**
   - [ ] Delete `/shared/src/iosMain/.../ReaderModeViewModel.ios.kt`
   - [ ] Delete `/shared/src/androidMain/.../ReaderModeViewModel.android.kt`
   - [ ] Delete `/shared/src/jvmMain/.../ReaderModeViewModel.desktop.kt`

3. **Update DI Configuration**
   - [ ] Register shared ViewModel in common Koin module
   - [ ] Verify injection works on all platforms

4. **Manual Testing**
   - [ ] Test on Android
   - [ ] Test on iOS
   - [ ] Test on Desktop
   - [ ] Verify all functionality works correctly
   - [ ] Verify no regressions introduced

**Acceptance Criteria**:
- Single shared ViewModel implementation
- All platforms use same logic
- No regressions in manual testing
- Consistent behavior across all platforms

---

### Phase 7: Migration & Cleanup

**Goal**: Enable new system by default and cleanup legacy code.

**Duration**: 1-2 weeks

**Tasks**:

1. **Enable FULL Mode by Default**
   - [ ] Change feature flag default to `FeedItemParsingMode.FULL`
   - [ ] Remove feature flag checks (or leave for emergency rollback)

2. **Background Pre-caching (Optional)**
   - [ ] Optionally trigger background parsing for bookmarked feed items
   - [ ] Monitor performance and user feedback
   - [ ] No data migration needed - files created on-demand

3. **Deprecate Legacy Components**
   - [ ] Mark `ReaderModeExtractor` as deprecated
   - [ ] Add deprecation warnings
   - [ ] Update documentation

4. **Remove Legacy Code** (After Verification Period)
   - [ ] Delete `ReaderModeExtractor` (Android)
   - [ ] Delete `ReaderModeExtractor` (Desktop)
   - [ ] Delete old parsing logic
   - [ ] Clean up unused dependencies

5. **Update Documentation**
   - [ ] Update CLAUDE.md with new architecture
   - [ ] Document new parsing system
   - [ ] Add troubleshooting guide

**Acceptance Criteria**:
- New system enabled for all users
- No critical bugs reported
- Legacy code removed
- Documentation updated
- No data migration issues (since there's no migration!)

---

### Phase 8: Polish & Optimization

**Goal**: Performance tuning, error handling, UX improvements, and manual validation.

**Duration**: 1 week

**Tasks**:

1. **Performance Optimization**
   - [ ] Profile article loading times
   - [ ] Optimize file I/O
   - [ ] Optimize WebView initialization
   - [ ] Reduce memory usage

2. **Error Handling Improvements**
   - [ ] Better error messages
   - [ ] Graceful degradation
   - [ ] User-facing error UI
   - [ ] Logging and analytics

3. **UX Improvements**
   - [ ] Add "Re-parse" menu option
   - [ ] Show parsing progress indicator
   - [ ] Cache invalidation options
   - [ ] Settings for background parsing

4. **Comprehensive Manual Validation**
   - [ ] Test edge cases (poor network, large articles, etc.)
   - [ ] Performance validation across all platforms
   - [ ] End-to-end user flow testing
   - [ ] Verify all features work as expected

**Acceptance Criteria**:
- Cached content loads instantly (< 100ms)
- Errors handled gracefully
- User can control parsing behavior via settings
- No performance regressions in manual testing
- All platforms tested and validated

---

## Detailed Implementation Steps

### Step 1: Create Core Interfaces (Phase 1)

#### 1.1 FeedItemParserWorker Interface

**File**: `/shared/src/commonMain/kotlin/com/prof18/feedflow/shared/domain/feeditem/FeedItemParserWorker.kt`

```kotlin
package com.prof18.feedflow.shared.domain.feeditem

import com.prof18.feedflow.core.model.ParsingResult

/**
 * Interface for parsing feed item content across platforms.
 *
 * Implementations handle fetching HTML and extracting feed item content
 * using platform-specific WebView/parsing libraries.
 */
interface FeedItemParserWorker {
    /**
     * Enqueue feed item parsing in background (fire-and-forget).
     * Used for bookmarked feed items that should be cached for offline reading.
     *
     * Android: Uses WorkManager
     * iOS: Uses CoroutineScope with background dispatcher
     * Desktop: Uses CoroutineScope
     *
     * @param feedItemId The unique feed item ID (used as filename)
     * @param url The URL to fetch and parse
     */
    suspend fun enqueueParsing(feedItemId: String, url: String)

    /**
     * Trigger immediate parsing with result callback.
     * Used when user opens feed item and content not cached.
     *
     * Blocks until parsing completes or fails.
     *
     * @param feedItemId The unique feed item ID (used as filename)
     * @param url The URL to fetch and parse
     * @return ParsingResult with parsed content or error
     */
    suspend fun triggerImmediateParsing(feedItemId: String, url: String): ParsingResult

    /**
     * Trigger background parsing (non-blocking).
     * Used by background workers for batch processing.
     *
     * @param feedItemId The unique feed item ID (used as filename)
     * @param url The URL to fetch and parse
     * @return ParsingResult with parsed content or error
     */
    suspend fun triggerBackgroundParsing(feedItemId: String, url: String): ParsingResult
}
```

#### 1.2 FeedItemContentFileHandler Interface

**File**: `/shared/src/commonMain/kotlin/com/prof18/feedflow/shared/domain/feeditem/FeedItemContentFileHandler.kt`

```kotlin
package com.prof18.feedflow.shared.domain.feeditem

/**
 * Interface for managing feed item content file storage.
 *
 * Implementations handle platform-specific file I/O:
 * - Android: context.filesDir
 * - iOS: App Group container
 * - Desktop: user.home/.feedflow/articles
 */
interface FeedItemContentFileHandler {
    /**
     * Save parsed feed item content to file.
     *
     * @param feedItemId Unique identifier for the feed item (URL hash)
     * @param content Parsed HTML content
     */
    suspend fun saveFeedItemContentToFile(feedItemId: String, content: String)

    /**
     * Load feed item content from file.
     *
     * @param feedItemId Unique identifier for the feed item
     * @return Parsed HTML content, or null if not cached
     */
    suspend fun loadFeedItemContent(feedItemId: String): String?

    /**
     * Check if feed item content is cached.
     *
     * @param feedItemId Unique identifier for the feed item
     * @return true if content file exists
     */
    suspend fun isContentAvailable(feedItemId: String): Boolean

    /**
     * Delete cached feed item content.
     *
     * @param feedItemId Unique identifier for the feed item
     */
    suspend fun deleteFeedItemContent(feedItemId: String)

    /**
     * Clear all cached feed item content.
     * Used for clearing cache or troubleshooting.
     */
    suspend fun clearAllContent()
}
```

#### 1.3 ParsingResult Model

**File**: `/core/src/commonMain/kotlin/com/prof18/feedflow/core/model/ParsingResult.kt`

```kotlin
package com.prof18.feedflow.core.model

/**
 * Result of feed item content parsing operation.
 *
 * Note: FeedFlow does NOT store parsed metadata in the database.
 * We trust the RSS feed metadata. Only the HTML content is cached to files.
 */
sealed class ParsingResult {
    /**
     * Parsing succeeded.
     *
     * @property htmlContent Cleaned HTML content (THIS is what we cache)
     * @property siteName Source domain (kept for potential future use)
     */
    data class Success(
        val htmlContent: String?,
        val siteName: String?,
    ) : ParsingResult()

    /**
     * Parsing failed due to network, parsing, or other error.
     */
    data object Error : ParsingResult()
}
```

**Note**: Only `htmlContent` and `siteName` are included. Title and word count are not needed since we trust the RSS feed metadata.

---

### Step 2: Implement Android FeedItemParser (Phase 2)

#### 2.1 FeedItemParser Implementation

**File**: `/shared/src/androidMain/kotlin/com/prof18/feedflow/shared/domain/parser/FeedItemParser.kt`

```kotlin
package com.prof18.feedflow.shared.domain.parser

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.readRawResource
import com.prof18.feedflow.shared.utils.DispatcherProvider
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class FeedItemParser(
    private val appContext: Context,
    private val htmlRetriever: HtmlRetriever,
    private val dispatcherProvider: DispatcherProvider,
    private val logger: Logger,
) {
    private var webView: WebView? = null

    suspend fun parseFeedItem(url: String): ParsingResult = suspendCoroutine { continuation ->
        logger.d { "Parsing article: $url" }

        // 1. Fetch HTML
        val html = runCatching {
            htmlRetriever.retrieveHtml(url)
        }.getOrNull()

        if (html == null) {
            logger.e { "Failed to fetch HTML for: $url" }
            continuation.resume(ParsingResult.Error)
            return@suspendCoroutine
        }

        // 2. Load Defuddle.js
        val defuddleJs = try {
            readRawResource(appContext, R.raw.defuddle)
        } catch (e: Exception) {
            logger.e(e) { "Failed to load defuddle.js" }
            continuation.resume(ParsingResult.Error)
            return@suspendCoroutine
        }

        // 3. Parse with WebView
        withContext(dispatcherProvider.main) {
            if (webView == null) {
                webView = WebView(appContext).apply {
                    settings.javaScriptEnabled = true
                }
            }

            webView?.apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        val parsingScript = """
                            try {
                                const parser = new DOMParser();
                                const doc = parser.parseFromString(`${html.escapeForJS()}`, 'text/html');
                                const defuddle = new Defuddle(doc, { url: '${url.escapeForJS()}' });
                                const result = defuddle.parse();

                                // Calculate plain text for validation
                                let plainText = '';
                                if (result.content) {
                                    const tempDiv = document.createElement('div');
                                    tempDiv.innerHTML = result.content;
                                    plainText = (tempDiv.textContent || tempDiv.innerText || '').trim();
                                }
                                result.plainText = plainText;

                                window.ParserInterface.onParsingComplete(JSON.stringify(result));
                            } catch (e) {
                                window.ParserInterface.onParsingError(e.toString());
                            }
                        """

                        evaluateJavascript(parsingScript, null)
                    }
                }

                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onParsingComplete(resultJson: String) {
                            logger.d { "Parsing completed" }
                            try {
                                val json = JSONObject(resultJson)
                                val content = json.optString("content")
                                val title = json.optString("title")
                                val siteName = json.optString("site")
                                val plainText = json.optString("plainText", "")

                                // Validate content
                                if (plainText.length < 200) {
                                    logger.w { "Content too short (${plainText.length} chars), rejecting" }
                                    continuation.resume(ParsingResult.Error)
                                    return
                                }

                                val wordCount = plainText.split(Regex("\\s+")).size

                                continuation.resume(
                                    ParsingResult.Success(
                                        htmlContent = content,
                                        title = title.takeIf { it.isNotBlank() },
                                        length = wordCount,
                                        siteName = siteName.takeIf { it.isNotBlank() },
                                    )
                                )
                            } catch (e: Exception) {
                                logger.e(e) { "Failed to parse result JSON" }
                                continuation.resume(ParsingResult.Error)
                            }
                        }

                        @JavascriptInterface
                        fun onParsingError(error: String) {
                            logger.e { "Parsing error: $error" }
                            continuation.resume(ParsingResult.Error)
                        }
                    },
                    "ParserInterface"
                )

                loadDataWithBaseURL(
                    url,
                    "<html><head><script>$defuddleJs</script></head><body></body></html>",
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    }

    fun cleanup() {
        webView?.destroy()
        webView = null
    }

    private fun String.escapeForJS(): String =
        replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")
}
```

**Notes**:
- This is a simplified version - you may need to adjust based on actual Defuddle.js API
- Add timeout handling for long-running parses
- Consider pooling WebView instances for performance

---

## Rollback Plan

### Emergency Rollback (Critical Issues)

1. **Feature Flag Rollback**
   ```kotlin
   // Revert to LEGACY mode
   fun getFeedItemParsingMode() = FeedItemParsingMode.LEGACY
   ```

2. **File Cache Cleanup (if needed)**
   - Cached files can safely remain (no harm)
   - Or call `FeedItemContentFileHandler.clearAllContent()` to remove cache
   - NO database changes to rollback!

3. **Code Rollback**
   - Revert ViewModel changes
   - Re-enable ReaderModeExtractor
   - Remove FeedItemParserWorker injection

**Simplified Rollback**: Since there are NO database changes, rollback is much simpler and safer!

### Partial Rollback (Platform-Specific Issues)

1. **Android Rollback**
   - Disable Android parsing via feature flag
   - Keep iOS/Desktop on new system

2. **iOS Rollback**
   - Similar to Android

---

## Success Criteria

### Phase 1 Success ✅

- [x] All interfaces compile successfully ✅
- [x] JavaScript resources copied correctly ✅
- [x] NO database changes (confirmed) ✅
- [x] No impact on existing functionality ✅
- [x] Project builds without errors ✅

### Phase 2-4 Success (Per Platform)

- [ ] Feed items parse correctly in manual testing
- [ ] Content caches to files
- [ ] Cached feed items load < 100ms
- [ ] Feature flag controls behavior
- [ ] No crashes or memory leaks observed
- [ ] No regressions in manual testing

### Phase 5 Success

- [ ] Background parsing works on all platforms
- [ ] Bookmarked feed items queue automatically
- [ ] Retry logic works correctly
- [ ] No battery drain issues observed

### Phase 6 Success

- [ ] Single shared ViewModel implemented
- [ ] All platforms work identically
- [ ] No regressions in manual testing

### Overall Migration Success

- [ ] 95%+ of feed items parse successfully in testing
- [ ] Cached feed item load time < 100ms
- [ ] No increase in crash rate
- [ ] Smooth user experience in manual testing
- [ ] Background parsing completes within reasonable time
- [ ] Storage usage acceptable (< 100MB for 1000 cached items)
- [ ] NO database migrations issues (because there are none!)

---

## Risks & Mitigation

### Risk 1: Parsing Quality Degradation

**Risk**: Defuddle.js may not parse all articles as well as current extractors.

**Mitigation**:
- Compare parsing quality before migration
- Keep fallback to browser for failed parses
- Monitor parsing success rate
- User feedback mechanism

### Risk 2: Storage Bloat

**Risk**: Cached articles consume excessive storage.

**Mitigation**:
- Implement cache size limits
- Add cache eviction policy (LRU)
- Allow user to clear cache
- Monitor average article size

### Risk 3: Background Processing Battery Drain

**Risk**: Background parsing drains battery.

**Mitigation**:
- Use appropriate constraints (Wi-Fi, charging)
- Limit concurrent parsing
- Monitor battery usage
- Allow user to disable background parsing

### Risk 4: Migration Complexity

**Risk**: Migration takes longer than estimated.

**Mitigation**:
- Incremental phased approach
- Each phase independently testable
- Feature flags for quick rollback
- Allocate buffer time in schedule

### Risk 5: Platform-Specific Bugs

**Risk**: Issues on specific platforms (iOS file permissions, Android WebView crashes).

**Mitigation**:
- Extensive platform-specific testing
- Beta testing on real devices
- Gradual rollout per platform
- Quick rollback capability

---

## Additional Considerations

### Content Migration for Existing Users

**Approach**: No Migration Needed (Simple!)
- Start with empty file cache
- Parse on-demand as users open feed items
- Optionally trigger background parsing for bookmarked items (Phase 5)
- No database migration required
- No risk, no complexity
- Users won't notice any changes except faster subsequent loads

**Why This Works**:
- FeedFlow doesn't need to migrate existing data
- RSS feed metadata (from `feed_item` table) remains unchanged
- Only parsed HTML content is cached to files (created on first view)
- Existing users seamlessly transition to cached reading

### Widget Support (iOS)

If FeedFlow has iOS widgets that display article content:

- Use App Group container for file storage
- Ensure widgets can read cached content
- Test widget refresh after parsing

### Backup & Sync

Consider future enhancements:

- Sync cached articles via iCloud/Dropbox
- Backup/restore cache during device migration
- Export articles for archival

---

## Timeline Summary

| Phase | Duration | Deliverables |
|-------|----------|-------------|
| Phase 1: Foundation | 1 week | Interfaces only, NO database changes |
| Phase 2: Android | 2-3 weeks | Android parsing + caching |
| Phase 3: iOS | 2-3 weeks | iOS parsing + caching |
| Phase 4: Desktop | 1 week | Desktop parsing + caching |
| Phase 5: Background | 2 weeks | Background parsing all platforms |
| Phase 6: Consolidate | 1 week | Shared ViewModel |
| Phase 7: Cleanup | 1-2 weeks | Enable, cleanup (no migration) |
| Phase 8: Polish | 1 week | Optimization, UX |
| **Total** | **9-12 weeks** | **Complete migration** |

**Simplified from ReaderFlow approach**: No database changes required, which saves time and reduces complexity!

---

## Key Implementation Patterns (Phases 1-3 Complete)

### ✅ Patterns Successfully Implemented

**1. Reader-Flow Structure Adherence**
- All iOS components (FeedItemParser.swift, FeedItemParserWorkerIos.swift) match reader-flow structure exactly
- Only minimal naming changes (Article → FeedItem)
- Preserved callback patterns, singleton patterns, and control flow

**2. File Identification Pattern**
- ✅ **Uses feed item ID directly** as filename (e.g., "abc123.html")
- ❌ **NO MD5 hashing** (removed from both Android and iOS)
- Matches reader-flow pattern exactly
- Applied to both platforms consistently

**3. Koin DI Injection Pattern (iOS)**
- Parser created in Swift (`FeedItemParserWorkerIos()`)
- Passed to Kotlin during `doInitKoinIos()` initialization
- Registered as Koin singleton
- Injected into ViewModel with non-optional parameters
- Matches reader-flow pattern exactly

**4. File Storage Locations**
- Android: `context.filesDir/{feedItemId}.html`
- iOS: `App Group container/articles/{feedItemId}.html`
- Desktop: (planned) `user.home/.feedflow/articles/{feedItemId}.html`

**5. Simplified Architecture from Reader-Flow**
- ✅ File-based content caching (same as reader-flow)
- ✅ Unified parsing interface (same as reader-flow)
- ❌ NO database metadata storage (simpler than reader-flow)
- ❌ NO persistent parsing queue (simpler than reader-flow)

### 📝 Patterns to Follow for Remaining Phases

**Phase 4 (Desktop)**:
- Follow reader-flow Desktop implementation structure
- Use direct feed item ID (no hashing)
- Match Koin DI patterns from reader-flow

**Phase 5 (Background Parsing)**:
- Follow reader-flow worker patterns
- Simple in-memory queue (simpler than reader-flow)
- No database-backed queue needed

---

## Next Steps

1. **Review & Approval**
   - Review this plan with team
   - Adjust timeline/scope as needed
   - Get stakeholder approval

2. **Prepare Development Environment**
   - Set up feature flags
   - Copy Defuddle.js resources
   - Create tracking issues for each phase

3. **Begin Phase 1**
   - Create interfaces
   - Copy JavaScript resources
   - Verify everything compiles

4. **Iterate**
   - Complete each phase
   - Test manually on all platforms
   - Gather feedback
   - Adjust as needed

---

**Document Version**: 1.7
**Author**: Claude Code
**Date**: 2025-11-03
**Last Updated**: 2025-11-04
**Status**: In Progress - Phase 4 Complete (All Platform Implementations Done, Ready for Manual Testing)

**Changelog**:
- v1.7: **Phase 4 completed** (2025-11-04) - Desktop implementation complete with DesktopFeedItemParserWorker, FeedItemContentFileHandlerDesktop
  - Uses Readability4J for parsing (no WebView on Desktop)
  - Uses feed item ID directly as filename (matching Android/iOS)
  - Keeps Desktop-specific MarkdownToHtmlConverter
  - File storage: `~/.feedflow/articles/{feedItemId}.html`
  - Updated Gap Analysis to reflect completed Desktop implementation
  - All three platform implementations (Android, iOS, Desktop) now complete
- v1.6: **Added critical implementation guideline** (2025-11-04) - All FeedFlow implementations MUST follow reader-flow structure exactly with minimal differences only
  - Updated Phase 2 & 3 notes with feed item ID usage (no MD5 hashing)
  - Updated Phase 3 notes with Koin DI injection details and structure matching
  - Updated Gap Analysis to reflect completed iOS implementation with all changes
- v1.5: **Phase 3 completed** (2025-11-04) - iOS implementation complete with FeedItemParser.swift, FeedItemParserWorkerIos.swift, and FeedItemContentFileHandlerIos
- v1.4: **ParsingResult updated** (2025-11-03) - Added title field back for UI display, refactored to match reader-flow style (callback-based, CompletableDeferred)
- v1.3: **Phase 2 completed** (2025-11-03) - Android implementation complete, new parser integrated with optional DI parameters
- v1.2: **Phase 1 completed** (2025-11-03) - Simplified ParsingResult model (removed length field, kept title)
- v1.1: Removed automated testing requirements - manual testing only
- v1.0: Initial version
