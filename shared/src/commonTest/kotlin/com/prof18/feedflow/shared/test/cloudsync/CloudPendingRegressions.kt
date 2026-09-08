package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.shared.domain.feedsync.PendingCloudChangesManager
import kotlinx.coroutines.flow.first
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal suspend fun runCloudPendingRegressions(provider: CloudProvider) {
    val store = CloudStore()
    val device1 = createCloudDevice(provider, store, "device1")
    var device2 = createCloudDevice(provider, store, "device2")
    try {
        device1.seed()
        device1.read("article-one", true)
        device1.backup()
        store.propagate("device1")
        device2.refresh()
        val session = device2.application.koin.get<PendingCloudChangesManager>().sessionForEdit()
        device2.database.updateReadStatus(FeedItemId("article-one"), false, cloudSessionId = session)
        assertFalse(device2.settings.getIsSyncUploadRequired())
        assertTrue(device2.repository.isUploadRequired.first())
        val root = device2.root
        device2.close()
        device2 = createCloudDevice(provider, store, "device2", root)
        device2.refresh()
        assertEquals(false to false, device2.flags()["article-one"])
        assertTrue(
            device2.syncDatabase.getAllFeedItems().single { it.id == "article-one" }.isRead,
            "Refresh preserves the downloaded snapshot while pending unread wins in the app database",
        )
        device2.repository.performBackup()
        store.propagate("device2")
        assertFalse(device2.repository.isUploadRequired.first())
        device1.refresh()
        assertEquals(false to false, device1.flags()["article-one"])

        device1.bookmark("article-two", true)
        device1.backup()
        store.propagate("device1")
        val previous = device2.flags()
        val oldSync = device2.syncDatabase.getAllFeedItems()
        store.beforeDownload = { device2.settings.rotateCloudSyncSession() }
        device2.refresh()
        assertEquals(previous, device2.flags(), "Old-account download must not be applied")
        assertEquals(oldSync, device2.syncDatabase.getAllFeedItems(), "Old-account file must not replace the sync DB")
    } finally {
        device1.close()
        device2.close()
    }
}
