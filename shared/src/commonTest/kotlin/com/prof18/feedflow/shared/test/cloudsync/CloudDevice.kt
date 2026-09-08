package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import org.koin.core.KoinApplication
import kotlin.time.Clock
import kotlin.time.Instant

internal expect fun createCloudDevice(
    provider: CloudProvider,
    store: CloudStore,
    id: String,
    root: String? = null,
): CloudDevice

internal object CloudHarnessClock : Clock {
    private var timestamp = 1000L
    override fun now(): Instant = Instant.fromEpochMilliseconds(timestamp++)
}

internal class CloudDevice(
    val application: KoinApplication,
    val root: String,
    val closeResources: () -> Unit = {},
) {
    val database: DatabaseHelper get() = application.koin.get()
    val syncDatabase: SyncedDatabaseHelper get() = application.koin.get()
    val settings: SettingsRepository get() = application.koin.get()
    val repository: FeedSyncRepository get() = application.koin.get()
    private val actions: FeedActionsRepository get() = application.koin.get()
    private var closed = false

    suspend fun seed() {
        database.insertFeedSourceWithCategory(source)
        seedArticles()
    }

    suspend fun seedArticles() {
        database.insertFeedItems(
            listOf(
                buildFeedItem("article-one", "First fixture", 2000, source),
                buildFeedItem("article-two", "Second fixture", 1000, source),
            ),
            lastSyncTimestamp = 0,
        )
    }

    suspend fun read(id: String, value: Boolean) = actions.updateReadStatus(FeedItemId(id), value)
    suspend fun bookmark(id: String, value: Boolean) = actions.updateBookmarkStatus(FeedItemId(id), value)
    suspend fun addSubscription() = application.koin.get<FeedSourcesRepository>().addFeedSourceWithoutFetching(
        feedUrl = "https://fixture.invalid/second.xml",
        feedTitle = "Additional fixture",
        category = FeedSourceCategory("second-category", "Second category"),
        logoUrl = null,
    )

    suspend fun markAllRead() = actions.markAllFeedAsRead(FeedFilter.Timeline)
    suspend fun backup() = repository.performBackup(forceBackup = true)

    suspend fun refresh() {
        repository.syncFeedSources()
        // Deterministic RSS content arrives between source and article-state reconciliation, as in refresh.
        seedArticles()
        repository.syncFeedItems()
    }

    suspend fun flags(): Map<String, Pair<Boolean, Boolean>> = database.getFeedItems(
        feedFilter = FeedFilter.Timeline,
        pageSize = 10,
        showReadItems = true,
        sortOrder = FeedOrder.NEWEST_FIRST,
    ).associate { it.url_hash to (it.is_read to it.is_bookmarked) }

    suspend fun close() {
        if (closed) return
        closed = true
        try {
            syncDatabase.closeScope()
            database.close()
        } finally {
            try {
                application.close()
            } finally {
                closeResources()
            }
        }
    }

    companion object {
        val category = FeedSourceCategory("fixture-category", "Fixture category")
        val source = FeedSource(
            id = "fixture-source",
            url = "https://fixture.invalid/feed.xml",
            title = "Fixture source",
            category = category,
            lastSyncTimestamp = null,
            logoUrl = null,
            websiteUrl = null,
            fetchFailed = false,
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHiddenFromTimeline = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )
    }
}
