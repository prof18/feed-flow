package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.SyncDownloadError
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedSyncJvmWorkerTest {

    @Test
    fun `syncDownloadErrorForAccount returns Google Drive error for Google Drive account`() {
        val error = syncDownloadErrorForAccount(SyncAccounts.GOOGLE_DRIVE)

        assertEquals(SyncDownloadError.GoogleDriveDownloadFailed, error)
    }

    @Test
    fun `syncDownloadErrorForAccount returns iCloud error for iCloud account`() {
        val error = syncDownloadErrorForAccount(SyncAccounts.ICLOUD)

        assertEquals(SyncDownloadError.ICloudDownloadFailed, error)
    }

    @Test
    fun `syncDownloadErrorForAccount returns Dropbox error for Dropbox account`() {
        val error = syncDownloadErrorForAccount(SyncAccounts.DROPBOX)

        assertEquals(SyncDownloadError.DropboxDownloadFailed, error)
    }
}
