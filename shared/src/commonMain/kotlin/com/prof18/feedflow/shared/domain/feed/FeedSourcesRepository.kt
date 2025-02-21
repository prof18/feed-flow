package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedSourceWithUnreadCount
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.onErrorSuspend
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.greader.GReaderRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.presentation.model.ErrorState
import com.prof18.feedflow.shared.presentation.model.SyncError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class FeedSourcesRepository(
    private val databaseHelper: DatabaseHelper,
    private val accountsRepository: AccountsRepository,
    private val feedSyncRepository: FeedSyncRepository,
    private val gReaderRepository: GReaderRepository,
    private val dispatcherProvider: DispatcherProvider,
) {

    private val errorMutableState: MutableSharedFlow<ErrorState> = MutableSharedFlow()
    val errorState = errorMutableState.asSharedFlow()

    fun getFeedSources(): Flow<List<FeedSource>> =
        databaseHelper.getFeedSourcesFlow()

    suspend fun deleteFeed(feedSource: FeedSource) {
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.deleteFeedSource(feedSource.id)
                    .onErrorSuspend {
                        errorMutableState.emit(SyncError)
                    }
            }

            else -> {
                databaseHelper.deleteFeedSource(feedSource.id)
                feedSyncRepository.deleteFeedSource(feedSource)
                feedSyncRepository.performBackup()
            }
        }
    }

    fun deleteAllFeeds() {
        databaseHelper.deleteAllFeeds()
        feedSyncRepository.deleteAllFeedSources()
    }

    fun observeFeedSourcesByCategoryWithUnreadCount(): Flow<Map<FeedSourceCategory?, List<FeedSourceWithUnreadCount>>> =
        databaseHelper.getFeedSourcesWithUnreadCountFlow()
            .map { feedSources ->
                val sourcesByCategory = feedSources.groupBy { it.feedSource.category }
                val sortedKeys = sourcesByCategory.keys.sortedBy { it?.title }
                sortedKeys.associateWith {
                    sourcesByCategory[it] ?: emptyList()
                }
            }

    suspend fun updateFeedSourceName(feedSourceId: String, newName: String) =
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.editFeedSourceName(feedSourceId, newName)
                    .onErrorSuspend {
                        errorMutableState.emit(SyncError)
                    }
            }
            else -> {
                databaseHelper.updateFeedSourceName(feedSourceId, newName)
                feedSyncRepository.updateFeedSourceName(feedSourceId, newName)
                feedSyncRepository.performBackup()
            }
        }

    suspend fun insertFeedSourcePreference(
        feedSourceId: String,
        preference: LinkOpeningPreference,
        isHidden: Boolean,
        isPinned: Boolean,
    ) = withContext(dispatcherProvider.io) {
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = feedSourceId,
            preference = preference,
            isHidden = isHidden,
            isPinned = isPinned,
        )
    }
}
