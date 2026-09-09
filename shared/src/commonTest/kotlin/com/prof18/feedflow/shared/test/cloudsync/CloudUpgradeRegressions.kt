package com.prof18.feedflow.shared.test.cloudsync

import app.cash.turbine.test
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal suspend fun runCloudUpgradeRegressions(provider: CloudProvider) {
    refreshReplacesUntrackedLegacyFlags(provider)
    backupPreservesCloudAndTrackedEdits(provider)
    failedDownloadPreservesPendingEditsAcrossRestart(provider)
    missingBackupUploadsFullLocalState(provider)
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

private suspend fun backupPreservesCloudAndTrackedEdits(provider: CloudProvider) {
    val store = CloudStore()
    val uploader = createCloudDevice(provider, store, "upgrade-backup-uploader")
    val device = createCloudDevice(provider, store, "upgrade-backup-device")
    val recipient = createCloudDevice(provider, store, "upgrade-backup-recipient")
    try {
        uploader.seed()
        uploader.read("article-one", true)
        uploader.bookmark("article-two", true)
        uploader.backup()
        store.propagate("upgrade-backup-uploader")

        device.seed()
        device.database.updateBookmarkStatus(FeedItemId("article-one"), true)
        device.database.updateReadStatus(FeedItemId("article-two"), true)
        device.settings.setIsSyncUploadRequired(true)
        device.repository.cloudSessionForEdit()
        device.read("article-one", false)
        device.bookmark("article-two", false)
        val localBeforeBackup = device.flags()

        device.backup()
        store.propagate("upgrade-backup-device")

        assertEquals(
            localBeforeBackup,
            device.flags(),
            "Backup must not apply the downloaded snapshot to the app database",
        )
        recipient.refresh()
        val expected = mapOf(
            "article-one" to (false to false),
            "article-two" to (false to false),
        )
        assertEquals(expected, recipient.flags(), "Cloud fields must win except for precise journaled edits")

        device.refresh()
        assertEquals(expected, device.flags(), "The uploading device must converge through the next normal refresh")
        assertFalse(device.settings.getIsSyncUploadRequired())
    } finally {
        try {
            uploader.close()
        } finally {
            try {
                device.close()
            } finally {
                recipient.close()
            }
        }
    }
}

private suspend fun failedDownloadPreservesPendingEditsAcrossRestart(provider: CloudProvider) {
    val store = CloudStore()
    val uploader = createCloudDevice(provider, store, "upgrade-retry-uploader")
    var device = createCloudDevice(provider, store, "upgrade-retry-device")
    val recipient = createCloudDevice(provider, store, "upgrade-retry-recipient")
    try {
        uploader.seed()
        uploader.read("article-one", true)
        uploader.backup()
        store.propagate("upgrade-retry-uploader")
        device.refresh()

        device.read("article-one", false)
        device.bookmark("article-two", true)
        val expected = device.flags()
        val remote = store.snapshot().single().bytes
        val uploadsBeforeFailure = store.events.count { it.operation == CloudStoreOperation.UPLOAD }
        store.downloadFailure = IllegalStateException("Cloud download unavailable")
        device.application.koin.get<FeedSyncMessageQueue>().userMessages.test {
            device.backup()
            expectNoEvents()
        }

        assertEquals(uploadsBeforeFailure, store.events.count { it.operation == CloudStoreOperation.UPLOAD })
        assertContentEquals(remote, store.snapshot().single().bytes)
        assertEquals(expected, device.flags())
        assertTrue(device.settings.getIsSyncUploadRequired())

        val root = device.root
        device.close()
        device = createCloudDevice(provider, store, "upgrade-retry-device", root)
        store.downloadFailure = null
        device.backup()
        store.propagate("upgrade-retry-device")

        assertFalse(device.settings.getIsSyncUploadRequired())
        recipient.refresh()
        assertEquals(expected, recipient.flags(), "Pending edits must survive failure and process restart")
    } finally {
        store.downloadFailure = null
        try {
            uploader.close()
        } finally {
            try {
                device.close()
            } finally {
                recipient.close()
            }
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
