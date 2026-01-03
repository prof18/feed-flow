package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.ArticleExportFilter
import com.prof18.feedflow.core.model.FeedItemImportData
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.onError
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.database.domain.toFeedSource
import com.prof18.feedflow.feedsync.feedbin.domain.FeedbinRepository
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.shared.domain.csv.CsvInput
import com.prof18.feedflow.shared.domain.csv.CsvOutput
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.model.NotValidFeedSources
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import de.halfbit.csv.CsvHeaderRow
import de.halfbit.csv.CsvWithHeader
import de.halfbit.csv.buildCsv
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
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX, SyncAccounts.BAZQUX -> {
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

    suspend fun exportArticlesAsCsv(
        csvOutput: CsvOutput,
        filter: ArticleExportFilter,
    ) = withContext(dispatcherProvider.io) {
        val feedItems = databaseHelper.getFeedItemsForExport(filter)
        val csv = buildCsv {
            header {
                articleCsvHeaders.forEach { column(it) }
            }
            feedItems.forEach { item ->
                data {
                    value(item.url_hash)
                    value(item.url)
                    value(item.title.orEmpty())
                    value(item.subtitle.orEmpty())
                    value(item.image_url.orEmpty())
                    value(item.feed_source_id)
                    value(item.is_read.toString())
                    value(item.is_bookmarked.toString())
                    value(item.pub_date?.toString().orEmpty())
                    value(item.comments_url.orEmpty())
                    value(item.notification_sent.toString())
                    value(item.is_blocked.toString())
                }
            }
        } as CsvWithHeader

        csvOutput.writeText(csv.toCsvText())
    }

    @Suppress("MagicNumber")
    suspend fun importArticlesFromCsv(
        csvInput: CsvInput,
    ) = withContext(dispatcherProvider.io) {
        val csvText = csvInput.readText()
        val csv = CsvWithHeader.fromCsvText(csvText) as CsvWithHeader
        val header = csv.header

        val urlHashIndex = header.columnIndex(articleCsvHeaders[0])
        val urlIndex = header.columnIndex(articleCsvHeaders[1])
        val titleIndex = header.columnIndex(articleCsvHeaders[2])
        val subtitleIndex = header.columnIndex(articleCsvHeaders[3])
        val imageUrlIndex = header.columnIndex(articleCsvHeaders[4])
        val feedSourceIdIndex = header.columnIndex(articleCsvHeaders[5])
        val isReadIndex = header.columnIndex(articleCsvHeaders[6])
        val isBookmarkedIndex = header.columnIndex(articleCsvHeaders[7])
        val pubDateIndex = header.columnIndex(articleCsvHeaders[8])
        val commentsUrlIndex = header.columnIndex(articleCsvHeaders[9])
        val notificationSentIndex = header.columnIndex(articleCsvHeaders[10])
        val isBlockedIndex = header.columnIndex(articleCsvHeaders[11])

        val feedItems = csv.data.map { row ->
            FeedItemImportData(
                urlHash = row.valueAt(urlHashIndex),
                url = row.valueAt(urlIndex),
                title = row.valueAtOrNull(titleIndex),
                subtitle = row.valueAtOrNull(subtitleIndex),
                imageUrl = row.valueAtOrNull(imageUrlIndex),
                feedSourceId = row.valueAt(feedSourceIdIndex),
                isRead = row.valueAt(isReadIndex).toBoolean(),
                isBookmarked = row.valueAt(isBookmarkedIndex).toBoolean(),
                pubDateMillis = row.valueAtOrNull(pubDateIndex)?.trim()?.toLongOrNull(),
                commentsUrl = row.valueAtOrNull(commentsUrlIndex),
                notificationSent = row.valueAt(notificationSentIndex).toBoolean(),
                isBlocked = row.valueAt(isBlockedIndex).toBoolean(),
                contentFetched = false,
            )
        }

        val feedSourceIds = databaseHelper.getAllFeedSourceIds().toSet()
        databaseHelper.importFeedItemsFromCsv(
            feedItems = feedItems,
            existingFeedSourceIds = feedSourceIds,
        )
    }
}

private val articleCsvHeaders = listOf(
    "url_hash",
    "url",
    "title",
    "subtitle",
    "image_url",
    "feed_source_id",
    "is_read",
    "is_bookmarked",
    "pub_date",
    "comments_url",
    "notification_sent",
    "is_blocked",
)

private fun List<String>.valueAt(index: Int): String =
    getOrNull(index) ?: error("Missing CSV column at index $index")

private fun List<String>.valueAtOrNull(index: Int): String? =
    getOrNull(index)?.takeIf { it.isNotBlank() }

private fun CsvHeaderRow.columnIndex(name: String): Int {
    val column = columnByName(name)
    requireNotNull(column) { "Missing CSV column: $name" }
    return column.index
}
