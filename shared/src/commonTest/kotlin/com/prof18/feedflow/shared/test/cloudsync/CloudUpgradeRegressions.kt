package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.core.model.FeedItemId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal suspend fun runCloudUpgradeRegressions(provider: CloudProvider) {
    refreshReplacesUntrackedLegacyFlags(provider)
    if (provider != CloudProvider.ICLOUD) missingBackupUploadsFullLocalState(provider)
}

private suspend fun refreshReplacesUntrackedLegacyFlags(provider: CloudProvider) {
    val store = CloudStore()
    val uploader = createCloudDevice(provider, store, "upgrade-refresh-uploader")
    val device = createCloudDevice(provider, store, "upgrade-refresh-device")
    try {
        uploader.seed()
        uploader.read("article-one", true)
        uploader.bookmark("article-two", true)
        uploader.backup()
        store.propagate("upgrade-refresh-uploader")
        val cloudFlags = uploader.flags()

        device.seed()
        device.database.updateBookmarkStatus(FeedItemId("article-one"), true)
        device.database.updateReadStatus(FeedItemId("article-two"), true)
        device.settings.setIsSyncUploadRequired(true)

        device.refresh()

        assertEquals(cloudFlags, device.flags(), "A normal refresh must replace untracked pre-journal flags")
    } finally {
        try {
            uploader.close()
        } finally {
            device.close()
        }
    }
}

private suspend fun missingBackupUploadsFullLocalState(provider: CloudProvider) {
    val store = CloudStore()
    val device = createCloudDevice(provider, store, "upgrade-bootstrap-device")
    val recipient = createCloudDevice(provider, store, "upgrade-bootstrap-recipient")
    try {
        device.seed()
        device.database.updateReadStatus(FeedItemId("article-one"), true)
        device.database.updateBookmarkStatus(FeedItemId("article-two"), true)
        device.settings.setIsSyncUploadRequired(true)
        val expected = device.flags()

        store.uploadFailure = IllegalStateException("Cloud upload unavailable")
        device.backup()

        assertEquals(0, store.fileCount(provider))
        assertEquals(expected, device.flags())
        assertTrue(device.settings.getIsSyncUploadRequired())

        store.uploadFailure = null
        device.refresh()
        store.propagate("upgrade-bootstrap-device")

        assertEquals(1, store.fileCount(provider))
        assertFalse(device.settings.getIsSyncUploadRequired())
        recipient.refresh()
        assertEquals(listOf(CloudDevice.category), recipient.database.getFeedSourceCategories())
        assertEquals(listOf(CloudDevice.source.id), recipient.database.getFeedSources().map { it.id })
        assertEquals(expected, recipient.flags(), "A confirmed missing backup must bootstrap all local flags")
    } finally {
        store.uploadFailure = null
        try {
            device.close()
        } finally {
            recipient.close()
        }
    }
}
