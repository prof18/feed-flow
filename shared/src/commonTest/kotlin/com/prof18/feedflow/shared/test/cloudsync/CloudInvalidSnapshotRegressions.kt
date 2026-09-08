package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal suspend fun runCloudInvalidSnapshotRegressions(provider: CloudProvider) = coroutineScope {
    val store = CloudStore()
    val device1 = createCloudDevice(provider, store, "validation-device1")
    val device2 = createCloudDevice(provider, store, "validation-device2")
    val outcomes = mutableListOf<SyncResult>()
    val collector = launch(start = CoroutineStart.UNDISPATCHED) {
        device2.application.koin.get<FeedSyncMessageQueue>().messageQueue.collect { outcomes.add(it) }
    }
    try {
        device1.seed()
        device1.read("article-one", true)
        device1.backup()
        store.propagate("validation-device1")
        device2.refresh()
        val expectedMain = device2.flags()
        val expectedSync = device2.syncDatabase.getAllFeedItems()
        val validBytes = store.snapshot().single().bytes
        val futureVersion = validBytes.copyOf().apply { this[SQLITE_VERSION_LOW_BYTE] = 2 }
        val invalidSnapshots = listOf(
            byteArrayOf(),
            "not a SQLite database".encodeToByteArray(),
            validBytes.copyOf(SQLITE_HEADER_SIZE),
            futureVersion,
        )
        for (bytes in invalidSnapshots) {
            outcomes.clear()
            store.downloadedBytes = bytes
            device2.repository.syncFeedSources()
            device2.repository.syncFeedItems()
            assertTrue(outcomes.isNotEmpty())
            assertTrue(outcomes.all { it.isError() }, "Invalid download reported success: $outcomes")
            assertEquals(expectedMain, device2.flags())
            assertEquals(expectedSync, device2.syncDatabase.getAllFeedItems(), "The last good sync DB must survive")
        }
        // A complete legacy version-zero snapshot is normalized without discarding its rows.
        store.downloadedBytes = validBytes.copyOf().apply { this[SQLITE_VERSION_LOW_BYTE] = 0 }
        device2.refresh()
        assertEquals(expectedMain, device2.flags())
        assertEquals(expectedSync, device2.syncDatabase.getAllFeedItems())
    } finally {
        collector.cancel()
        try {
            device1.close()
        } finally {
            device2.close()
        }
    }
}

private const val SQLITE_VERSION_LOW_BYTE = 63
private const val SQLITE_HEADER_SIZE = 100
