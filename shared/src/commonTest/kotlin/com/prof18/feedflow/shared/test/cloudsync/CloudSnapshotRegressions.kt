package com.prof18.feedflow.shared.test.cloudsync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal suspend fun runCloudSnapshotRegression(provider: CloudProvider) = coroutineScope {
    val store = CloudStore()
    val device1 = createCloudDevice(provider, store, "snapshot-device1")
    val device2 = createCloudDevice(provider, store, "snapshot-device2")
    val uploadStarted = CompletableDeferred<Unit>()
    val resumeUpload = CompletableDeferred<Unit>()
    try {
        device1.seed()
        device1.backup()
        store.propagate("snapshot-device1")
        store.beforeUploadRead = {
            uploadStarted.complete(Unit)
            resumeUpload.await()
        }
        val upload = launch { device1.backup() }
        uploadStarted.await()
        // This must finish while the transport is paused, and must not change its captured bytes.
        device1.read("article-one", true)
        resumeUpload.complete(Unit)
        upload.join()
        store.beforeUploadRead = {}
        store.propagate("snapshot-device1")
        device2.refresh()
        assertEquals(false to false, device2.flags()["article-one"])
        assertEquals(true to false, device1.flags()["article-one"])
        assertTrue(device1.settings.getIsSyncUploadRequired(), "An earlier upload must not acknowledge a later edit")
        device1.backup()
        store.propagate("snapshot-device1")
        assertFalse(device1.settings.getIsSyncUploadRequired())
        device2.refresh()
        assertEquals(true to false, device2.flags()["article-one"])
    } finally {
        resumeUpload.complete(Unit)
        store.beforeUploadRead = {}
        try {
            device1.close()
        } finally {
            device2.close()
        }
    }
}
