package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal suspend fun runCloudTransferRegressions(provider: CloudProvider) = coroutineScope {
    val store = CloudStore()
    val device1 = createCloudDevice(provider, store, "device1")
    val device2 = createCloudDevice(provider, store, "device2")
    val outcomes = mutableListOf<SyncResult>()
    val collector = launch(start = CoroutineStart.UNDISPATCHED) {
        device1.application.koin.get<FeedSyncMessageQueue>().messageQueue.collect { outcomes.add(it) }
    }
    val userMessages = mutableListOf<SyncResult>()
    val notificationCollectors = listOf(device1, device2).map { device ->
        launch(start = CoroutineStart.UNDISPATCHED) {
            device.application.koin.get<FeedSyncMessageQueue>().userMessages.collect { userMessages.add(it) }
        }
    }
    try {
        device1.seed()
        device1.read("article-one", true)
        device1.repository.firstSync()
        store.propagate("device1")
        assertEquals(1, store.fileCount(provider))
        val remote = store.snapshot().single().bytes

        device1.bookmark("article-two", true)
        outcomes.clear()
        store.uploadFailure = IllegalStateException("Injected upload failure")
        device1.backup()
        store.propagate("device1")
        assertTrue(device1.settings.getIsSyncUploadRequired())
        assertContentEquals(remote, store.snapshot().single().bytes)
        assertTrue(outcomes.isNotEmpty())
        assertTrue(outcomes.all { it.isError() }, "Failed upload reported success: $outcomes")
        assertTrue(userMessages.isEmpty(), "Failed upload interrupted the user: $userMessages")

        device2.seed()
        device2.bookmark("article-one", true)
        val expectedFlags = device2.flags()
        val uploads = store.events.count { it.operation == CloudStoreOperation.UPLOAD }
        store.uploadFailure = null
        store.downloadFailure = IllegalStateException("Injected download failure")
        device2.repository.firstSync()
        device2.refresh()
        assertEquals(uploads, store.events.count { it.operation == CloudStoreOperation.UPLOAD })
        assertContentEquals(remote, store.snapshot().single().bytes)
        assertEquals(expectedFlags, device2.flags(), "Failed download must stop reconciliation")
        assertTrue(device2.settings.getIsSyncUploadRequired())
        assertTrue(userMessages.isEmpty(), "Failed download interrupted the user: $userMessages")

        store.downloadFailure = null
        device1.backup()
        assertFalse(device1.settings.getIsSyncUploadRequired())
        store.propagate("device1")
        device2.refresh()
        assertEquals(false to true, device2.flags()["article-two"])
        if (provider == CloudProvider.ICLOUD) {
            val cloudBeforeFailedDiscovery = store.snapshot().single().bytes
            device2.bookmark("article-one", false)
            val localAfterEdit = device2.flags()
            val uploadsBeforeFailedDiscovery = store.events.count { it.operation == CloudStoreOperation.UPLOAD }
            store.iCloudDiscoveryFailure = IllegalStateException("Injected iCloud discovery failure")
            device2.backup()
            assertEquals(
                uploadsBeforeFailedDiscovery,
                store.events.count { it.operation == CloudStoreOperation.UPLOAD },
            )
            assertContentEquals(cloudBeforeFailedDiscovery, store.snapshot().single().bytes)
            assertEquals(localAfterEdit, device2.flags())
            assertTrue(device2.settings.getIsSyncUploadRequired())

            store.iCloudDiscoveryFailure = null
            store.downloadFailure = IllegalStateException("Injected iCloud download failure")
            device2.backup()
            assertEquals(
                uploadsBeforeFailedDiscovery,
                store.events.count { it.operation == CloudStoreOperation.UPLOAD },
            )
            assertContentEquals(cloudBeforeFailedDiscovery, store.snapshot().single().bytes)
            assertEquals(localAfterEdit, device2.flags())
            assertTrue(device2.settings.getIsSyncUploadRequired())
            store.downloadFailure = null
        }
        if (provider == CloudProvider.GOOGLE_DRIVE) {
            assertEquals(
                store.snapshot().single().fileId,
                device2.application.koin.get<GoogleDriveSettings>().getBackupFileId(),
            )
        }
    } finally {
        notificationCollectors.forEach { it.cancel() }
        collector.cancel()
        try {
            device1.close()
        } finally {
            device2.close()
        }
    }
    assertTrue(userMessages.isEmpty(), "Transfer failure or retry interrupted the user: $userMessages")
}

internal suspend fun runICloudStagedReadRegression() = coroutineScope {
    val store = CloudStore()
    val device1 = createCloudDevice(CloudProvider.ICLOUD, store, "staged-device1")
    val device2 = createCloudDevice(CloudProvider.ICLOUD, store, "staged-device2")
    try {
        device1.seed()
        device1.repository.firstSync()
        store.propagate("staged-device1")

        device1.read("article-one", true)
        device1.backup()
        assertFalse(device1.settings.getIsSyncUploadRequired())
        device1.bookmark("article-two", true)
        device1.backup()
        store.propagate("staged-device1")

        device2.refresh()
        assertEquals(true to false, device2.flags()["article-one"])
        assertEquals(false to true, device2.flags()["article-two"])
    } finally {
        device1.close()
        device2.close()
    }
}
