package com.prof18.feedflow.shared.test.cloudsync

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal suspend fun runCloudFlagRegressions(provider: CloudProvider) {
    val store = CloudStore()
    val device1 = createCloudDevice(provider, store, "device1")
    val device2 = createCloudDevice(provider, store, "device2")
    try {
        device1.seed()
        val states = listOf(false to false, false to true, true to false, true to true)
        for (before in states) {
            for (after in states) {
                device1.read("article-one", before.first)
                device1.bookmark("article-one", before.second)
                device1.backup()
                store.propagate("device1")
                device2.refresh()
                assertEquals(before, device2.flags()["article-one"])

                device1.read("article-one", after.first)
                assertEquals(after.first to before.second, device1.flags()["article-one"])
                device1.bookmark("article-one", after.second)
                device1.backup()
                assertFalse(device1.settings.getIsSyncUploadRequired())
                store.propagate("device1")
                // The email's sequence refreshes the uploading device after clearing a flag.
                device1.refresh()
                device2.refresh()
                assertEquals(after, device1.flags()["article-one"], "Sender: $before -> $after")
                assertEquals(after, device2.flags()["article-one"], "Recipient: $before -> $after")
            }
        }
        device1.markAllRead()
        device1.backup()
        store.propagate("device1")
        device2.refresh()
        device1.read("article-two", false)
        device1.backup()
        store.propagate("device1")
        device1.refresh()
        device2.refresh()
        assertEquals(false to false, device1.flags()["article-two"])
        assertEquals(false to false, device2.flags()["article-two"])
    } finally {
        try {
            device1.close()
        } finally {
            device2.close()
        }
    }
}

internal suspend fun runStaleCloudRefreshRegression(provider: CloudProvider) {
    val store = CloudStore()
    val device1 = createCloudDevice(provider, store, "device1")
    val device2 = createCloudDevice(provider, store, "device2")
    try {
        device1.seed()
        device1.read("article-one", true)
        device1.backup()
        store.propagate("device1")
        device2.refresh()
        device1.bookmark("article-two", true)
        device1.addSubscription()
        store.propagate("device1")
        val remote = store.snapshot().single().bytes
        val uploadCount = store.events.count { it.operation == CloudStoreOperation.UPLOAD }
        device2.read("article-one", false)
        assertTrue(device2.settings.getIsSyncUploadRequired())

        device2.refresh()

        assertEquals(uploadCount, store.events.count { it.operation == CloudStoreOperation.UPLOAD })
        store.propagate("device2")
        assertContentEquals(remote, store.snapshot().single().bytes)
        assertEquals(2, device2.database.getFeedSources().size)
        assertEquals(false to true, device2.flags()["article-two"])
        // Preserving B's pending field intent during download is the separate C2/C3 work package.
    } finally {
        try {
            device1.close()
        } finally {
            device2.close()
        }
    }
}
