package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.SyncDownloadError
import com.prof18.feedflow.core.model.SyncResult

internal interface FeedSyncWorker {
    /**
     * The job will be enqueued not done immediately
     */
    fun upload()

    /**
     * It's always immediate, without waiting
     */
    suspend fun uploadImmediate()

    suspend fun download(isFirstSync: Boolean = false): SyncResult

    suspend fun syncFeedSources(): SyncResult

    suspend fun syncFeedItems(): SyncResult
}

internal fun syncDownloadErrorForAccount(account: SyncAccounts): SyncDownloadError =
    when (account) {
        SyncAccounts.GOOGLE_DRIVE -> SyncDownloadError.GoogleDriveDownloadFailed
        SyncAccounts.ICLOUD -> SyncDownloadError.ICloudDownloadFailed
        SyncAccounts.DROPBOX,
        SyncAccounts.LOCAL,
        SyncAccounts.FRESH_RSS,
        SyncAccounts.MINIFLUX,
        SyncAccounts.BAZQUX,
        SyncAccounts.FEEDBIN,
        -> SyncDownloadError.DropboxDownloadFailed
    }
