package com.prof18.feedflow.feedsync.googledrive

import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.prof18.feedflow.core.utils.DesktopOS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

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

    @Test
    fun `buildLocalServerReceiver binds to the loopback IP literal, not the localhost hostname`() {
        val receiver = buildLocalServerReceiver()

        assertEquals("127.0.0.1", receiver.host)
    }

    @Test
    fun `buildVerificationCodeReceiver uses blocking receiver on Windows`() {
        assertIs<BlockingLoopbackReceiver>(buildVerificationCodeReceiver(DesktopOS.WINDOWS))
    }

    @Test
    fun `buildVerificationCodeReceiver keeps Google receiver on macOS and Linux`() {
        assertIs<LocalServerReceiver>(buildVerificationCodeReceiver(DesktopOS.MAC))
        assertIs<LocalServerReceiver>(buildVerificationCodeReceiver(DesktopOS.LINUX))
    }
}
