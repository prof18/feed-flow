package com.prof18.feedflow.feedsync.googledrive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GoogleDriveDataSourceJvmImplTest {

    @Test
    fun `requireGoogleDriveBackupFileId returns id when present`() {
        val resolvedId = requireGoogleDriveBackupFileId(
            fileId = "backup-123",
            fileName = "FeedFlowFeedSyncDB.db",
        )

        assertEquals("backup-123", resolvedId)
    }

    @Test
    fun `requireGoogleDriveBackupFileId throws when id is missing`() {
        val exception = assertFailsWith<GoogleDriveDownloadException> {
            requireGoogleDriveBackupFileId(
                fileId = null,
                fileName = "FeedFlowFeedSyncDB.db",
            )
        }

        assertEquals(
            "No Google Drive backup file found for 'FeedFlowFeedSyncDB.db'",
            exception.message,
        )
    }

    @Test
    fun `buildLocalServerReceiver uses dynamic free port`() {
        val receiver = buildLocalServerReceiver()

        assertEquals(-1, receiver.port)
    }
}
