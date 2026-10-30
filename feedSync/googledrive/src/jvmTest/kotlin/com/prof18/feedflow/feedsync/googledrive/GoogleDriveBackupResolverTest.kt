package com.prof18.feedflow.feedsync.googledrive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GoogleDriveBackupResolverTest {

    @Test
    fun `empty candidates resolve to null`() {
        assertNull(GoogleDriveBackupResolver.resolve(emptyList()))
    }

    @Test
    fun `single candidate is kept without orphans`() {
        val resolution = GoogleDriveBackupResolver.resolve(listOf(GoogleDriveBackupCandidate("only", 10)))

        assertEquals(GoogleDriveBackupResolution(keptId = "only", orphanIds = emptyList()), resolution)
    }

    @Test
    fun `newest candidate is kept and the others are sorted orphans`() {
        val resolution = GoogleDriveBackupResolver.resolve(
            listOf(
                GoogleDriveBackupCandidate("c", 10),
                GoogleDriveBackupCandidate("newest", 30),
                GoogleDriveBackupCandidate("a", 20),
            ),
        )

        assertEquals(GoogleDriveBackupResolution(keptId = "newest", orphanIds = listOf("a", "c")), resolution)
    }

    @Test
    fun `equal modified times keep the smallest id`() {
        val resolution = GoogleDriveBackupResolver.resolve(
            listOf(
                GoogleDriveBackupCandidate("b", 1_700_000_000_123),
                GoogleDriveBackupCandidate("a", 1_700_000_000_123),
            ),
        )

        assertEquals(GoogleDriveBackupResolution(keptId = "a", orphanIds = listOf("b")), resolution)
    }

    @Test
    fun `duplicate ids appear only once`() {
        val resolution = GoogleDriveBackupResolver.resolve(
            listOf(
                GoogleDriveBackupCandidate("kept", 20),
                GoogleDriveBackupCandidate("kept", 20),
                GoogleDriveBackupCandidate("orphan", 10),
                GoogleDriveBackupCandidate("orphan", 10),
            ),
        )

        assertEquals(GoogleDriveBackupResolution(keptId = "kept", orphanIds = listOf("orphan")), resolution)
    }

    @Test
    fun `orphan name appends the suffix and file id`() {
        assertEquals("FeedFlow.db.orphan-abc", GoogleDriveBackupResolver.orphanName("FeedFlow.db", "abc"))
    }
}
