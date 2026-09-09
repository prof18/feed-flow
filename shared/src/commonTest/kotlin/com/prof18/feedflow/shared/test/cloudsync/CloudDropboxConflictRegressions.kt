package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.shared.domain.feedsync.PendingCloudChangesManager
import kotlinx.coroutines.flow.first
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Exercises Dropbox's revision based compare-and-swap path through real platform workers.
 * Provider SDK behaviour is covered by the adapter tests; this suite only tests sync
 * recovery once the adapter reports a conflict.
 */
internal suspend fun runDropboxConflictRegressions() {
    runDropboxInterleavedEdits()
    runDropboxFirstCreatorRace()
    runDropboxLostAcknowledgement()
    runDropboxPersistentConflict()
    runDropboxConflictSessionRotation()
}

private suspend fun runDropboxConflictSessionRotation() {
    val store = CloudStore()
    val device1 = createCloudDevice(CloudProvider.DROPBOX, store, "device1")
    val device2 = createCloudDevice(CloudProvider.DROPBOX, store, "device2")
    try {
        device1.seed()
        device1.backup()
        device2.refresh()
        device1.bookmark("article-one", true)
        val pendingCloudChanges = device1.application.koin.get<PendingCloudChangesManager>()
        val session = requireNotNull(pendingCloudChanges.sessionForEdit())
        var downloadsAfterRotation = 0
        var remoteAfterConflict = byteArrayOf()
        store.beforeUploadRead = {
            store.beforeUploadRead = {}
            device2.bookmark("article-two", true)
            device2.backup()
            remoteAfterConflict = store.snapshot().single().bytes
            device1.settings.rotateCloudSyncSession()
            store.beforeDownload = { downloadsAfterRotation++ }
        }
        device1.backup()
        assertEquals(0, downloadsAfterRotation, "A retry must not begin a download for a different session")
        assertContentEquals(remoteAfterConflict, store.snapshot().single().bytes)
        assertTrue(device1.database.getCloudPendingArticleFlags(session).isNotEmpty())

        val previousSync = device1.syncDatabase.getAllFeedItems()
        assertFailsWith<IllegalStateException> { pendingCloudChanges.checkAccountSession(session) }
        assertEquals(previousSync, device1.syncDatabase.getAllFeedItems())
    } finally {
        device1.close()
        device2.close()
    }
}

private suspend fun runDropboxInterleavedEdits() {
    val store = CloudStore()
    val device1 = createCloudDevice(CloudProvider.DROPBOX, store, "device1")
    val device2 = createCloudDevice(CloudProvider.DROPBOX, store, "device2")
    try {
        device1.seed()
        device1.backup()
        device2.refresh()
        device1.bookmark("article-one", true)
        var fired = false
        store.beforeUploadRead = {
            if (!fired) {
                fired = true
                // Device 2's subscription action performs its own backup. Avoid recursively
                // firing the race hook while that backup is in progress.
                store.beforeUploadRead = {}
                device1.read("article-two", true)
                device2.bookmark("article-two", true)
                device2.addSubscription()
                store.beforeUploadRead = {}
            }
        }
        device1.backup()
        device1.refresh()
        device2.refresh()
        assertEquals(false to true, device1.flags()["article-one"])
        assertEquals(true to true, device1.flags()["article-two"])
        assertEquals(2, device1.database.getFeedSources().size)
        assertEquals(device1.flags(), device2.flags())
        assertFalse(device1.repository.isUploadRequired.first())
    } finally {
        device1.close()
        device2.close()
    }
}

private suspend fun runDropboxFirstCreatorRace() {
    val store = CloudStore()
    val device1 = createCloudDevice(CloudProvider.DROPBOX, store, "device1")
    val device2 = createCloudDevice(CloudProvider.DROPBOX, store, "device2")
    try {
        device1.seed()
        device2.seed()
        device1.bookmark("article-one", true)
        device2.bookmark("article-two", true)
        var fired = false
        store.beforeUploadRead = {
            if (!fired) {
                fired = true
                // Device 2 wins the initial ADD while Device 1 is between its local read and upload.
                store.beforeUploadRead = {}
                device2.backup()
                store.beforeUploadRead = {}
            }
        }
        device1.backup()
        device1.refresh()
        device2.refresh()
        assertEquals(false to true, device1.flags()["article-one"])
        assertEquals(false to true, device1.flags()["article-two"])
    } finally {
        device1.close()
        device2.close()
    }
}

private suspend fun runDropboxLostAcknowledgement() {
    val store = CloudStore()
    val device1 = createCloudDevice(CloudProvider.DROPBOX, store, "device1")
    val device2 = createCloudDevice(CloudProvider.DROPBOX, store, "device2")
    try {
        device1.seed()
        device1.backup()
        device2.refresh()
        device1.bookmark("article-one", true)
        store.afterUploadFailure = IllegalStateException("lost Dropbox upload response")
        device1.backup()
        assertTrue(device1.repository.isUploadRequired.first())
        device2.refresh()
        device2.bookmark("article-two", true)
        device2.backup()
        device1.refresh()
        device1.backup()
        device1.refresh()
        assertFalse(device1.repository.isUploadRequired.first())
        assertEquals(false to true, device1.flags()["article-one"])
        assertEquals(false to true, device1.flags()["article-two"])
    } finally {
        device1.close()
        device2.close()
    }
}

private suspend fun runDropboxPersistentConflict() {
    val store = CloudStore()
    val device1 = createCloudDevice(CloudProvider.DROPBOX, store, "device1")
    val device2 = createCloudDevice(CloudProvider.DROPBOX, store, "device2")
    try {
        device1.seed()
        device1.backup()
        device2.refresh()
        device1.bookmark("article-one", true)
        var conflicts = 0
        lateinit var conflictHook: suspend () -> Unit
        conflictHook = {
            if (conflicts < 3) {
                conflicts++
                store.beforeUploadRead = {}
                device2.bookmark("article-two", conflicts % 2 == 1)
                device2.backup()
                if (conflicts < 3) store.beforeUploadRead = conflictHook
            }
        }
        store.beforeUploadRead = conflictHook
        device1.backup()
        assertEquals(3, conflicts)
        assertTrue(device1.repository.isUploadRequired.first())
        store.beforeUploadRead = {}
        device1.refresh()
        device1.backup()
        device1.refresh()
        assertFalse(device1.repository.isUploadRequired.first())
        assertEquals(false to true, device1.flags()["article-one"])
    } finally {
        device1.close()
        device2.close()
    }
}
