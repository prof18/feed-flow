package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.onError
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.database.domain.toFeedSource
import com.prof18.feedflow.feedsync.feedbin.domain.FeedbinRepository
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.model.NotValidFeedSources
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import kotlinx.coroutines.withContext

internal class FeedImportExportRepository(
    private val databaseHelper: DatabaseHelper,
    private val opmlFeedHandler: OpmlFeedHandler,
    private val dispatcherProvider: DispatcherProvider,
    private val feedSyncRepository: FeedSyncRepository,
    private val accountsRepository: AccountsRepository,
    private val gReaderRepository: GReaderRepository,
    private val feedbinRepository: FeedbinRepository,
) {
    suspend fun addFeedsFromFile(
        opmlInput: OpmlInput,
    ): NotValidFeedSources = withContext(dispatcherProvider.io) {
        val feeds = opmlFeedHandler.generateFeedSources(opmlInput)
        val categories = feeds.mapNotNull { it.category }.distinct()

        val feedSourcesWithError = mutableListOf<ParsedFeedSource>()
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX -> {
                for (feed in feeds) {
                    gReaderRepository.addFeedSource(
                        url = feed.url,
                        categoryName = feed.category,
                        isNotificationEnabled = false,
                    ).onError {
                        feedSourcesWithError.add(feed)
                    }
                }
                return@withContext NotValidFeedSources(
                    feedSources = emptyList(),
                    feedSourcesWithError = feedSourcesWithError,
                )
            }

            SyncAccounts.FEEDBIN -> {
                for (feed in feeds) {
                    feedbinRepository.addFeedSource(
                        url = feed.url,
                        categoryName = feed.category,
                        isNotificationEnabled = false,
                    ).onError {
                        feedSourcesWithError.add(feed)
                    }
                }
                return@withContext NotValidFeedSources(
                    feedSources = emptyList(),
                    feedSourcesWithError = feedSourcesWithError,
                )
            }

            SyncAccounts.LOCAL,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.ICLOUD,
            -> {
                databaseHelper.insertCategories(categories)
                databaseHelper.insertFeedSource(feeds)

                feedSyncRepository.addSourceAndCategories(feeds.map { it.toFeedSource() }, categories)
                feedSyncRepository.performBackup()

                return@withContext NotValidFeedSources(
                    feedSources = emptyList(),
                    feedSourcesWithError = emptyList(),
                )
            }
        }
    }

    suspend fun exportFeedsAsOpml(opmlOutput: OpmlOutput) {
        val feeds = databaseHelper.getFeedSources()
        val feedsByCategory = feeds.groupBy { it.category }
        opmlFeedHandler.exportFeed(opmlOutput, feedsByCategory)
    }
}
