package com.prof18.feedflow.core.model

/**
 * Sealed hierarchy for error codes used throughout the application.
 */
sealed interface ErrorCode {
    val code: String
}

data object NoCode : ErrorCode {
    override val code: String = ""
}

// Sync Upload Errors (SU)
sealed class SyncUploadError(override val code: String) : ErrorCode {
    data object DatabaseFileGeneration : SyncUploadError("SU1")
    data object DropboxUploadFailed : SyncUploadError("SU2")
    data object DropboxAPIError : SyncUploadError("SU3")
    data object DropboxClientRestoreError : SyncUploadError("SU4")
    data object GoogleDriveClientRestoreError : SyncUploadError("SU5")
}

// Sync Download Errors (SD)
sealed class SyncDownloadError(override val code: String) : ErrorCode {
    data object DropboxDownloadFailed : SyncDownloadError("SD1")
    data object ICloudDownloadFailed : SyncDownloadError("SD2")
    data object GoogleDriveDownloadFailed : SyncDownloadError("SD3")
    data object GoogleDriveNeedsReAuth : SyncDownloadError("SD4")
}

// Sync Feed Errors (SF)
sealed class SyncFeedError(override val code: String) : ErrorCode {
    data object FeedSourcesSyncFailed : SyncFeedError("SF1")
    data object FeedItemsSyncFailed : SyncFeedError("SF2")
}

// Sync iCloud Errors (SI)
sealed class SyncICloudError(override val code: String) : ErrorCode {
    data object ServiceNotAvailable : SyncICloudError("SI1")
    data object URLNotAvailable : SyncICloudError("SI2")
    data object FileNotFound : SyncICloudError("SI3")
    data object CopyOperationFailed : SyncICloudError("SI4")
    data object FileAlreadyExists : SyncICloudError("SI5")
    data object DestinationUrlNull : SyncDownloadError("SI7")
}

// Database Errors (DB)
sealed class DatabaseError(override val code: String) : ErrorCode {
    data object FeedRetrievalFailed : DatabaseError("DB1")
    data object PaginationFailed : DatabaseError("DB2")
}

// Feed Sync Errors (FS) - FreshRSS/GReader
sealed class FeedSyncError(override val code: String) : ErrorCode {
    data object DeleteFeedSourceFailed : FeedSyncError("FS1")
    data object EditFeedSourceNameFailed : FeedSyncError("FS2")
    data object MarkItemsAsReadFailed : FeedSyncError("FS3")
    data object MarkAllFeedsAsReadFailed : FeedSyncError("FS4")
    data object UpdateBookmarkStatusFailed : FeedSyncError("FS5")
    data object UpdateReadStatusFailed : FeedSyncError("FS6")
    data object FetchFeedSourcesAndCategoriesFailed : FeedSyncError("FS8")
    data object EditCategoryNameFailed : FeedSyncError("FS9")
    data object DeleteCategoryFailed : FeedSyncError("FS9")
    data object SyncFeedsFailed : FeedSyncError("FS10")
}
