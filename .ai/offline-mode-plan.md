# Article Content Prefetching Implementation Plan

## Overview
Implement an article content prefetching system that automatically downloads and caches article content during RSS feed synchronization with smart batching (immediate + background) and platform-specific background processing.

## Requirements Summary
- Prefetch article content when RSS feeds are refreshed (only if `isPrefetchArticleContentEnabled()` setting is true)
- **Immediate batch**: First 40 articles fetched immediately as part of RSS sync (NOT queued)
- **Background batch**: After immediate batch completes, queue remaining articles and process in background
- **Platform-specific**: Android uses WorkManager, iOS/Desktop use coroutines
- **Best-effort**: Skip failed items permanently (no retries)
- **Persistence**: Database queue table to survive app restarts (only for background items)
- **Status tracking**: Boolean field `content_fetched` on `feed_item` table

## Implementation Phases

### ✅ Phase 1: Database Schema Changes

#### 1.1 Add `content_fetched` Column to Feed Item Table
**File**: `database/src/commonMain/sqldelight/com/prof18/feedflow/db/FeedItem.sq`

Add column after line 8 (after `content TEXT`):
```sql
content_fetched INTEGER AS Boolean NOT NULL DEFAULT 0,
```

Add new query at end of file:
```sql
updateContentFetchedStatus:
UPDATE feed_item
SET content_fetched = :contentFetched
WHERE url_hash = :urlHash;

selectUnfetchedItems:
SELECT feed_item.url_hash, feed_item.url
FROM feed_item
INNER JOIN feed_source ON feed_item.feed_source_id == feed_source.url_hash
LEFT JOIN feed_source_preferences ON feed_source.url_hash = feed_source_preferences.feed_source_id
WHERE content_fetched = 0
  AND is_blocked = 0
  AND (:isHidden IS NULL OR COALESCE(feed_source_preferences.is_hidden, 0) = :isHidden)
ORDER BY pub_date DESC
LIMIT :limit;
```

#### 1.2 Create Content Prefetch Queue Table
**File**: `database/src/commonMain/sqldelight/com/prof18/feedflow/db/ContentPrefetchQueue.sq` (NEW)

```sql
import kotlin.Boolean;

CREATE TABLE content_prefetch_queue (
    feed_item_id TEXT NOT NULL PRIMARY KEY,
    url TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (feed_item_id) REFERENCES feed_item(url_hash) ON DELETE CASCADE
);

CREATE INDEX idx_prefetch_created_at ON content_prefetch_queue(created_at);

insertQueueItem:
INSERT OR IGNORE INTO content_prefetch_queue(feed_item_id, url, created_at)
VALUES (?, ?, ?);

selectNextBatch:
SELECT feed_item_id, url
FROM content_prefetch_queue
ORDER BY created_at ASC
LIMIT :batchSize;

deleteQueueItem:
DELETE FROM content_prefetch_queue
WHERE feed_item_id = :feedItemId;

clearQueue:
DELETE FROM content_prefetch_queue;

countQueue:
SELECT COUNT(*) FROM content_prefetch_queue;
```

#### 1.3 Create Database Migration
**File**: `database/src/commonMain/sqldelight/migrations/<next_version>.sqm` (NEW)

Determine next migration version number and create:
```sql
ALTER TABLE feed_item ADD COLUMN content_fetched INTEGER AS Boolean NOT NULL DEFAULT 0;

CREATE TABLE content_prefetch_queue (
    feed_item_id TEXT NOT NULL PRIMARY KEY,
    url TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (feed_item_id) REFERENCES feed_item(url_hash) ON DELETE CASCADE
);

CREATE INDEX idx_prefetch_created_at ON content_prefetch_queue(created_at);
```

#### 1.4 Update DatabaseHelper
**File**: `database/src/commonMain/kotlin/com/prof18/feedflow/database/DatabaseHelper.kt`

Add data classes (after existing data classes around line 850+):
```kotlin
data class PrefetchQueueItem(
    val feedItemId: String,
    val url: String,
)

data class FeedItemToPrefetch(
    val feedItemId: String,
    val url: String,
)
```

Add methods to DatabaseHelper class (before companion object):
```kotlin
suspend fun updateContentFetchedStatus(feedItemId: String, fetched: Boolean) =
    dbRef.transactionWithContext(backgroundDispatcher) {
        dbRef.feedItemQueries.updateContentFetchedStatus(
            contentFetched = fetched,
            urlHash = feedItemId,
        )
    }

suspend fun insertPrefetchQueueItems(items: List<PrefetchQueueItem>, currentTimeMillis: Long) =
    dbRef.transactionWithContext(backgroundDispatcher) {
        items.forEach { item ->
            dbRef.contentPrefetchQueueQueries.insertQueueItem(
                feed_item_id = item.feedItemId,
                url = item.url,
                created_at = currentTimeMillis,
            )
        }
    }

suspend fun getNextPrefetchBatch(batchSize: Long): List<PrefetchQueueItem> =
    withContext(backgroundDispatcher) {
        dbRef.contentPrefetchQueueQueries
            .selectNextBatch(batchSize = batchSize)
            .executeAsList()
            .map { result ->
                PrefetchQueueItem(
                    feedItemId = result.feed_item_id,
                    url = result.url,
                )
            }
    }

suspend fun removePrefetchQueueItem(feedItemId: String) =
    dbRef.transactionWithContext(backgroundDispatcher) {
        dbRef.contentPrefetchQueueQueries.deleteQueueItem(feedItemId)
    }

suspend fun clearPrefetchQueue() =
    dbRef.transactionWithContext(backgroundDispatcher) {
        dbRef.contentPrefetchQueueQueries.clearQueue()
    }

suspend fun countPrefetchQueue(): Long =
    withContext(backgroundDispatcher) {
        dbRef.contentPrefetchQueueQueries
            .countQueue()
            .executeAsOne()
    }

suspend fun getUnfetchedItems(limit: Long): List<FeedItemToPrefetch> =
    withContext(backgroundDispatcher) {
        dbRef.feedItemQueries
            .selectUnfetchedItems(limit = limit, isHidden = 0)
            .executeAsList()
            .map { FeedItemToPrefetch(feedItemId = it.url_hash, url = it.url) }
    }
```

---

### ✅ Phase 2: Core Prefetch Repository

#### 2.1 Create ContentPrefetchRepository
**File**: `shared/src/commonMain/kotlin/com/prof18/feedflow/shared/domain/contentprefetch/ContentPrefetchRepository.kt` (NEW)

**Key Flow:**
1. `onFeedSyncCompleted()` is called after RSS sync
2. Fetch first 40 unfetched items and process them immediately (NOT queued)
3. After immediate batch completes, fetch remaining unfetched items and add to queue
4. Trigger background processing to work through queue

```kotlin
package com.prof18.feedflow.shared.domain.contentprefetch

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.database.PrefetchQueueItem
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class ContentPrefetchRepository(
    private val databaseHelper: DatabaseHelper,
    private val feedItemParserWorker: FeedItemParserWorker,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val settingsRepository: SettingsRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val dateFormatter: DateFormatter,
    private val logger: Logger,
) {
    private val _prefetchInProgress = MutableStateFlow(false)
    val prefetchInProgress: StateFlow<Boolean> = _prefetchInProgress.asStateFlow()

    /**
     * Called after RSS feed sync completes
     * 1. Process first 40 items immediately (NOT queued)
     * 2. After immediate batch completes, queue remaining items for background processing
     */
    suspend fun onFeedSyncCompleted() {
        if (!settingsRepository.isPrefetchArticleContentEnabled()) {
            logger.d { "Content prefetch is disabled" }
            return
        }

        try {
            logger.d { "Starting prefetch after feed sync" }

            // Get first 40 unfetched items - process immediately
            val immediateItems = databaseHelper.getUnfetchedItems(limit = IMMEDIATE_BATCH_SIZE)
            logger.d { "Found ${immediateItems.size} items for immediate prefetch" }

            if (immediateItems.isEmpty()) {
                logger.d { "No items to prefetch" }
                return
            }

            // Process immediate batch directly (not queued)
            processImmediateBatch(immediateItems)

            // After immediate batch, queue remaining items for background processing
            queueRemainingItems()

        } catch (e: Exception) {
            logger.e(e) { "Error in onFeedSyncCompleted" }
        }
    }

    /**
     * Process first 40 items immediately as part of RSS sync (sequential, like ReaderFlow)
     */
    private suspend fun processImmediateBatch(items: List<com.prof18.feedflow.database.FeedItemToPrefetch>) {
        _prefetchInProgress.value = true

        try {
            logger.d { "Processing immediate batch of ${items.size} items sequentially" }

            val queueItems = items.map { item ->
                PrefetchQueueItem(
                    feedItemId = item.feedItemId,
                    url = item.url,
                )
            }

            // Process all items sequentially (one at a time)
            processBatchSequentially(queueItems)

            logger.d { "Immediate batch processing complete" }

        } catch (e: Exception) {
            logger.e(e) { "Error processing immediate batch" }
        } finally {
            _prefetchInProgress.value = false
        }
    }

    /**
     * Queue remaining unfetched items (beyond first 40) for background processing
     */
    private suspend fun queueRemainingItems() {
        if (!settingsRepository.isPrefetchArticleContentEnabled()) {
            logger.d { "Prefetch disabled, skipping queue population" }
            return
        }

        try {
            // Check if queue already has items
            val existingCount = databaseHelper.countPrefetchQueue()
            if (existingCount > 0L) {
                logger.d { "Queue already has $existingCount items, skipping population" }
                return
            }

            // Get all unfetched items beyond first 40
            val allUnfetched = databaseHelper.getUnfetchedItems(limit = Long.MAX_VALUE)
            val remainingItems = allUnfetched.drop(IMMEDIATE_BATCH_SIZE.toInt())

            if (remainingItems.isEmpty()) {
                logger.d { "No remaining items to queue" }
                return
            }

            val queueItems = remainingItems.map { item ->
                PrefetchQueueItem(
                    feedItemId = item.feedItemId,
                    url = item.url,
                )
            }

            databaseHelper.insertPrefetchQueueItems(
                queueItems,
                dateFormatter.currentTimeMillis(),
            )
            logger.d { "Queued ${queueItems.size} items for background prefetch" }

        } catch (e: Exception) {
            logger.e(e) { "Error queueing remaining items" }
        }
    }

    /**
     * Process background queue items (called by platform-specific workers)
     * Processes items sequentially in batches
     */
    suspend fun triggerBackgroundPrefetch() {
        if (!settingsRepository.isPrefetchArticleContentEnabled()) {
            logger.d { "Prefetch disabled" }
            return
        }

        try {
            // Get a batch of items to process (limit batch size to avoid processing too many at once)
            val batch = databaseHelper.getNextPrefetchBatch(batchSize = BACKGROUND_BATCH_SIZE)

            if (batch.isEmpty()) {
                logger.d { "No queued items to process" }
                return
            }

            logger.d { "Processing background batch of ${batch.size} items sequentially" }
            processBatchSequentially(batch)

            // Log remaining count
            val remainingCount = databaseHelper.countPrefetchQueue()
            if (remainingCount > 0) {
                logger.d { "$remainingCount items remaining in queue" }
            } else {
                logger.d { "Background prefetch queue empty" }
            }

        } catch (e: Exception) {
            logger.e(e) { "Error in background prefetch" }
        }
    }

    /**
     * Process a batch of items sequentially (one at a time, like ReaderFlow)
     */
    private suspend fun processBatchSequentially(batch: List<PrefetchQueueItem>) {
        // Process items one by one in a simple loop (sequential, not concurrent)
        for (item in batch) {
            if (!settingsRepository.isPrefetchArticleContentEnabled()) {
                logger.d { "Prefetch disabled mid-batch" }
                return
            }

            try {
                prefetchSingleItem(item)
            } catch (e: Exception) {
                logger.e(e) { "Error prefetching ${item.feedItemId}" }
                // Remove from queue on error (best-effort, skip permanently)
                databaseHelper.removePrefetchQueueItem(item.feedItemId)
            }
        }
    }

    /**
     * Prefetch content for a single item
     */
    private suspend fun prefetchSingleItem(item: PrefetchQueueItem) {
        logger.d { "Prefetching: ${item.feedItemId}" }

        try {
            val result = feedItemParserWorker.triggerBackgroundParsing(
                feedItemId = item.feedItemId,
                url = item.url,
            )

            when (result) {
                is ParsingResult.Success -> {
                    val content = result.htmlContent
                    if (content != null) {
                        feedItemContentFileHandler.saveFeedItemContentToFile(item.feedItemId, content)
                        logger.d { "Prefetched successfully: ${item.feedItemId}" }
                    } else {
                        logger.w { "Content null for: ${item.feedItemId}" }
                    }
                    // Mark as fetched regardless of content availability
                    databaseHelper.updateContentFetchedStatus(item.feedItemId, fetched = true)
                    databaseHelper.removePrefetchQueueItem(item.feedItemId)
                }

                is ParsingResult.Error -> {
                    logger.w { "Parse failed for: ${item.feedItemId}, skipping permanently" }
                    // Mark as fetched to skip permanently (best-effort approach)
                    databaseHelper.updateContentFetchedStatus(item.feedItemId, fetched = true)
                    databaseHelper.removePrefetchQueueItem(item.feedItemId)
                }
            }

        } catch (e: Exception) {
            logger.e(e) { "Exception prefetching: ${item.feedItemId}" }
            throw e // Let caller handle removal
        }
    }

    /**
     * Clear all queued items (called when user disables setting)
     */
    suspend fun clearQueue() {
        try {
            databaseHelper.clearPrefetchQueue()
            _prefetchInProgress.value = false
            logger.d { "Cleared prefetch queue" }
        } catch (e: Exception) {
            logger.e(e) { "Error clearing queue" }
        }
    }

    companion object {
        private const val IMMEDIATE_BATCH_SIZE = 40L
        // Process background items in smaller batches to avoid long-running operations
        private const val BACKGROUND_BATCH_SIZE = 10L
    }
}
```

---

### ✅ Phase 3: Platform-Specific Background Workers

#### 3.1 Android WorkManager Implementation

**File**: `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/domain/contentprefetch/ContentPrefetchWorker.kt` (NEW)

```kotlin
package com.prof18.feedflow.shared.domain.contentprefetch

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger

class ContentPrefetchWorker internal constructor(
    private val contentPrefetchRepository: ContentPrefetchRepository,
    private val logger: Logger,
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            logger.d { "ContentPrefetchWorker started" }
            contentPrefetchRepository.triggerBackgroundPrefetch()
            logger.d { "ContentPrefetchWorker completed" }
            Result.success()
        } catch (e: Exception) {
            logger.e(e) { "ContentPrefetchWorker failed" }
            Result.failure()
        }
    }
}
```

**File**: `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/domain/contentprefetch/ContentPrefetchWorkerEnqueuer.kt` (NEW)

```kotlin
package com.prof18.feedflow.shared.domain.contentprefetch

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import co.touchlab.kermit.Logger

class ContentPrefetchWorkerEnqueuer internal constructor(
    private val context: Context,
    private val logger: Logger,
) {
    fun enqueueBackgroundPrefetch() {
        logger.d { "Enqueueing background content prefetch" }

        val workRequest = OneTimeWorkRequestBuilder<ContentPrefetchWorker>()
            .addTag(WORKER_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                WORKER_TAG,
                ExistingWorkPolicy.REPLACE,
                workRequest,
            )
    }

    fun cancelBackgroundPrefetch() {
        logger.d { "Cancelling background content prefetch" }
        WorkManager.getInstance(context).cancelUniqueWork(WORKER_TAG)
    }

    private companion object {
        const val WORKER_TAG = "ContentPrefetchWorker"
    }
}
```

#### 3.2 iOS/Desktop Coroutine-Based Implementation

**File**: `shared/src/iosMain/kotlin/com/prof18/feedflow/shared/domain/contentprefetch/ContentPrefetchBackgroundProcessor.kt` (NEW)

```kotlin
package com.prof18.feedflow.shared.domain.contentprefetch

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ContentPrefetchBackgroundProcessor(
    private val contentPrefetchRepository: ContentPrefetchRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val logger: Logger,
    private val coroutineScope: CoroutineScope,
) {
    private var backgroundJob: Job? = null

    fun startBackgroundProcessing() {
        if (backgroundJob?.isActive == true) {
            logger.d { "Background processing already active" }
            return
        }

        backgroundJob = coroutineScope.launch(dispatcherProvider.io) {
            logger.d { "Starting iOS background prefetch loop" }

            while (isActive) {
                try {
                    contentPrefetchRepository.triggerBackgroundPrefetch()
                    delay(PROCESSING_INTERVAL_MS)
                } catch (e: Exception) {
                    logger.e(e) { "Error in background prefetch loop" }
                    delay(ERROR_DELAY_MS)
                }
            }
        }
    }

    fun stopBackgroundProcessing() {
        backgroundJob?.cancel()
        backgroundJob = null
        logger.d { "Stopped background prefetch processing" }
    }

    private companion object {
        const val PROCESSING_INTERVAL_MS = 30_000L // 30 seconds
        const val ERROR_DELAY_MS = 60_000L // 1 minute on error
    }
}
```

**File**: `shared/src/jvmMain/kotlin/com/prof18/feedflow/shared/domain/contentprefetch/ContentPrefetchBackgroundProcessor.kt` (NEW)

Same as iOS implementation (copy the file above).

---

### ✅ Phase 4: Integration Points

#### 4.1 Integrate with FeedFetcherRepository
**File**: `shared/src/commonMain/kotlin/com/prof18/feedflow/shared/domain/feed/FeedFetcherRepository.kt`

1. Add constructor parameter (after line 46):
```kotlin
private val contentPrefetchRepository: ContentPrefetchRepository,
```

2. In `fetchFeedsWithRssParser()` method, after `feedStateRepository.getFeeds()` call (around line 138):
```kotlin
contentPrefetchRepository.onFeedSyncCompleted()
```

3. In `fetchFeedsWithGReader()` method, after `feedStateRepository.getFeeds()` call (around line 100):
```kotlin
contentPrefetchRepository.onFeedSyncCompleted()
```

#### 4.2 Update SettingsViewModel
**File**: `shared/src/commonMain/kotlin/com/prof18/feedflow/shared/presentation/SettingsViewModel.kt`

1. Add constructor parameters:
```kotlin
private val contentPrefetchRepository: ContentPrefetchRepository,
private val contentPrefetchWorkerEnqueuer: ContentPrefetchWorkerEnqueuer, // Android only, null on other platforms
private val contentPrefetchBackgroundProcessor: ContentPrefetchBackgroundProcessor?, // iOS/Desktop, null on Android
```

2. Update `setPrefetchArticleContent()` method:
```kotlin
fun setPrefetchArticleContent(enabled: Boolean) {
    settingsRepository.setPrefetchArticleContent(enabled)

    if (!enabled) {
        viewModelScope.launch {
            contentPrefetchRepository.clearQueue()
            contentPrefetchWorkerEnqueuer?.cancelBackgroundPrefetch()
            contentPrefetchBackgroundProcessor?.stopBackgroundProcessing()
        }
    }
}
```

#### 4.3 Start Background Processing on App Launch

**Android**: In `FeedFlowApp.kt` or main activity, after feed sync is set up, add:
```kotlin
// Enqueue background prefetch worker if setting is enabled
if (settingsRepository.isPrefetchArticleContentEnabled()) {
    contentPrefetchWorkerEnqueuer.enqueueBackgroundPrefetch()
}
```

**iOS/Desktop**: In app initialization (after Koin setup), add:
```kotlin
if (settingsRepository.isPrefetchArticleContentEnabled()) {
    contentPrefetchBackgroundProcessor.startBackgroundProcessing()
}
```

---

### ✅ Phase 5: Dependency Injection Setup

#### 5.1 Update Common Koin Module
**File**: `shared/src/commonMain/kotlin/com/prof18/feedflow/shared/di/Koin.kt`

In `getCoreModule()`, add ContentPrefetchRepository:
```kotlin
single {
    ContentPrefetchRepository(
        databaseHelper = get(),
        feedItemParserWorker = get(),
        feedItemContentFileHandler = get(),
        settingsRepository = get(),
        dispatcherProvider = get(),
        dateFormatter = get(),
        logger = getWith("ContentPrefetchRepository"),
    )
}
```

Update FeedFetcherRepository injection to include ContentPrefetchRepository:
```kotlin
single {
    FeedFetcherRepository(
        // ... existing parameters ...
        contentPrefetchRepository = get(),
    )
}
```

#### 5.2 Android-Specific Koin Module
**File**: `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/di/KoinAndroid.kt`

Add to existing module:
```kotlin
single {
    ContentPrefetchWorkerEnqueuer(
        context = androidContext(),
        logger = getWith("ContentPrefetchWorkerEnqueuer"),
    )
}

worker { (workerParams: WorkerParameters) ->
    ContentPrefetchWorker(
        contentPrefetchRepository = get(),
        logger = getWith("ContentPrefetchWorker"),
        appContext = androidContext(),
        workerParams = workerParams,
    )
}
```

#### 5.3 iOS/Desktop Koin Modules

Create scope for background processing in iOS/Desktop Koin setup:
```kotlin
single {
    ContentPrefetchBackgroundProcessor(
        contentPrefetchRepository = get(),
        dispatcherProvider = get(),
        logger = getWith("ContentPrefetchBackgroundProcessor"),
        coroutineScope = get(), // Pass appropriate scope
    )
}
```

---

## Critical Files Summary

**Database:**
- `database/src/commonMain/sqldelight/com/prof18/feedflow/db/FeedItem.sq` - Add `content_fetched` column + query
- `database/src/commonMain/sqldelight/com/prof18/feedflow/db/ContentPrefetchQueue.sq` (NEW) - Queue table
- `database/src/commonMain/sqldelight/migrations/<version>.sqm` (NEW) - Migration
- `database/src/commonMain/kotlin/com/prof18/feedflow/database/DatabaseHelper.kt` - Queue operations

**Core Logic:**
- `shared/src/commonMain/kotlin/com/prof18/feedflow/shared/domain/contentprefetch/ContentPrefetchRepository.kt` (NEW) - Main orchestration
- `shared/src/commonMain/kotlin/com/prof18/feedflow/shared/domain/feed/FeedFetcherRepository.kt` - Integration hook

**Platform Workers:**
- `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/domain/contentprefetch/ContentPrefetchWorker.kt` (NEW)
- `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/domain/contentprefetch/ContentPrefetchWorkerEnqueuer.kt` (NEW)
- `shared/src/iosMain/kotlin/com/prof18/feedflow/shared/domain/contentprefetch/ContentPrefetchBackgroundProcessor.kt` (NEW)
- `shared/src/jvmMain/kotlin/com/prof18/feedflow/shared/domain/contentprefetch/ContentPrefetchBackgroundProcessor.kt` (NEW)

**DI & Integration:**
- `shared/src/commonMain/kotlin/com/prof18/feedflow/shared/di/Koin.kt` - DI setup
- `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/di/KoinAndroid.kt` - Android DI
- `shared/src/commonMain/kotlin/com/prof18/feedflow/shared/presentation/SettingsViewModel.kt` - Settings integration

---

## Implementation Notes

### Processing Strategy (Following ReaderFlow Pattern)
- **Sequential processing**: Items processed one at a time in a simple for-loop
- **No concurrent fetches**: Avoids WebView initialization overhead and complexity
- **Batch sizes**:
  - Immediate: All 40 items processed sequentially during RSS sync
  - Background: 10 items per batch processed sequentially
- **WebView reuse**: Parser can reuse same WebView instance across items (if applicable)

### Error Handling
- All parsing failures mark item as `content_fetched = true` to skip permanently
- No retry logic (best-effort approach per requirements)
- Network errors logged but don't stop batch processing
- Always check `isPrefetchArticleContentEnabled()` before operations

### Queue Management
- **No priority system**: Queue contains only background items (items after first 40)
- First 40 items are processed immediately during RSS sync (NOT queued)
- Remaining items are added to queue after immediate batch completes
- Queue persists across app restarts via database
- ON DELETE CASCADE ensures queue cleanup when articles are deleted
- FIFO processing: Items processed in order of creation timestamp

### Platform Behavior
- **Android**: WorkManager ensures background processing even when app is closed
- **iOS/Desktop**: Processing only while app is active (no background equivalent)
- Both respect the same queue and processing logic

### Testing Considerations
- Test with setting enabled/disabled mid-process
- Verify queue persistence across app restarts
- Test with network failures (items should be skipped)
- Verify first 40 articles are fetched immediately vs background
- Check that duplicate queue entries are prevented (INSERT OR IGNORE)
