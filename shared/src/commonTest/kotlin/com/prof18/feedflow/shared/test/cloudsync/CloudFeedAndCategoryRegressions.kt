package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import kotlinx.coroutines.coroutineScope
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal suspend fun runCloudFeedAndCategoryRegressions(provider: CloudProvider) = coroutineScope {
    val store = CloudStore()
    val device1 = createCloudDevice(provider, store, "feed-category-device1")
    var device2 = createCloudDevice(provider, store, "feed-category-device2")
    try {
        device1.seed()
        device1.backup()
        store.propagate("feed-category-device1")
        device2.refresh()

        val sourceRepository = device2.application.koin.get<FeedSourcesRepository>()
        store.uploadFailure = IllegalStateException("Offline feed or category edit")
        sourceRepository.updateFeedSourceName(CloudDevice.source.id, "Offline title")
        assertTrue(device2.settings.getIsSyncUploadRequired())
        store.uploadFailure = null

        val device2Root = device2.root
        device2.close()
        device2 = createCloudDevice(provider, store, "feed-category-device2", device2Root)

        device1.bookmark("article-one", true)
        device1.addSubscription()
        device1.backup()
        store.propagate("feed-category-device1")

        device2.refresh()

        assertEquals("Offline title", device2.database.getFeedSource(CloudDevice.source.id)?.title)
        assertEquals(false to true, device2.flags()["article-one"])
        assertEquals(2, device2.database.getFeedSources().size)
        assertEquals(
            setOf(CloudDevice.category.id, "second-category"),
            device2.database.getFeedSourceCategories().map { it.id }.toSet(),
        )
    } finally {
        try {
            device1.close()
        } finally {
            device2.close()
        }
    }
}

internal suspend fun runCloudFeedAndCategoryDeletionRegressions(provider: CloudProvider) = coroutineScope {
    val store = CloudStore()
    val device1 = createCloudDevice(provider, store, "delete-device1")
    val device2 = createCloudDevice(provider, store, "delete-device2")
    try {
        device1.seed()
        device1.backup()
        store.propagate("delete-device1")
        device2.refresh()

        device2.application.koin.get<FeedSourcesRepository>().deleteFeed(CloudDevice.source)
        device2.application.koin.get<FeedCategoryRepository>().deleteCategory(CloudDevice.category.id)
        device2.backup()
        store.propagate("delete-device2")

        device1.repository.syncFeedSources()
        device1.repository.syncFeedItems()
        assertTrue(device1.database.getFeedSources().isEmpty())
        assertTrue(device1.database.getFeedSourceCategories().isEmpty())
    } finally {
        try {
            device1.close()
        } finally {
            device2.close()
        }
    }
}

internal suspend fun runCloudFeedAndCategoryStaleBackupRegressions(provider: CloudProvider) {
    val store = CloudStore()
    val device1 = createCloudDevice(provider, store, "stale-device1")
    val device2 = createCloudDevice(provider, store, "stale-device2")
    try {
        device1.seed()
        device1.backup()
        store.propagate("stale-device1")
        device2.refresh()

        device1.bookmark("article-one", true)
        device1.addSubscription()
        device1.backup()
        store.propagate("stale-device1")

        store.uploadFailure = IllegalStateException("Offline feed or category edit")
        device2.application.koin.get<FeedSourcesRepository>().updateFeedSourceName(
            CloudDevice.source.id,
            "Pending title",
        )
        store.uploadFailure = null
        val uploadsBeforeDirectBackup = store.events.count { it.operation == CloudStoreOperation.UPLOAD }
        device2.backup()

        assertTrue(store.events.count { it.operation == CloudStoreOperation.UPLOAD } > uploadsBeforeDirectBackup)
        store.propagate("stale-device2")
        device1.refresh()
        assertEquals(2, device1.database.getFeedSources().size)
        assertEquals(false to true, device1.flags()["article-one"])
        assertEquals("Pending title", device1.database.getFeedSource(CloudDevice.source.id)?.title)

        val uploadsBeforeFailedDownload = store.events.count { it.operation == CloudStoreOperation.UPLOAD }
        store.downloadFailure = IllegalStateException("Fresh base unavailable")
        device2.application.koin.get<FeedSourcesRepository>().updateFeedSourceName(
            CloudDevice.source.id,
            "Still pending",
        )
        store.uploadFailure = null
        device2.backup()

        assertEquals(uploadsBeforeFailedDownload, store.events.count { it.operation == CloudStoreOperation.UPLOAD })
        assertTrue(device2.settings.getIsSyncUploadRequired())
        assertEquals("Still pending", device2.database.getFeedSource(CloudDevice.source.id)?.title)
    } finally {
        store.downloadFailure = null
        store.uploadFailure = null
        try {
            device1.close()
        } finally {
            device2.close()
        }
    }
}

internal suspend fun runCloudFeedAndCategoryAccountGuardRegressions(provider: CloudProvider) {
    val device = createCloudDevice(provider, CloudStore(), "account-edit")
    try {
        device.seed()
        device.database.updateReadStatus(FeedItemId("article-one"), true)
        device.repository.syncFeedItems()
        assertEquals(true to false, device.flags()["article-one"])
        val session = requireNotNull(device.repository.cloudSessionForEdit())
        val guard = device.repository.cloudEditGuard(session)
        val sourcesBefore = device.database.getFeedSources()
        val flagsBefore = device.flags()
        device.settings.rotateCloudSyncSession()

        assertFailsWith<IllegalStateException> {
            device.database.updateFeedSourceName(
                CloudDevice.source.id,
                "Stale account edit",
                cloudSessionId = session,
                withCurrentSession = guard,
            )
        }
        assertFailsWith<IllegalStateException> {
            device.database.deleteAllCloudSubscriptions(session, withCurrentSession = guard)
        }
        assertFailsWith<IllegalStateException> {
            device.database.updateReadStatus(
                FeedItemId("article-one"),
                true,
                cloudSessionId = session,
                withCurrentSession = guard,
            )
        }
        assertFailsWith<IllegalStateException> {
            device.database.updateFeedItemReadAndBookmarked(
                emptyList(),
                cloudSessionId = session,
                replaceAll = true,
                withCurrentSession = guard,
            )
        }
        assertEquals(sourcesBefore, device.database.getFeedSources())
        assertEquals(flagsBefore, device.flags())
        assertTrue(device.database.getCloudPendingFeedAndCategoryChanges(session).isEmpty())
        assertTrue(device.database.getCloudPendingArticleFlags(session).isEmpty())
        verifyLocalEditGuardAfterAccountChange(device, provider)
    } finally {
        device.close()
    }
}

private suspend fun verifyLocalEditGuardAfterAccountChange(device: CloudDevice, provider: CloudProvider) {
    val accounts = device.application.koin.get<AccountsRepository>()
    accounts.clearAllAccounts()
    val session = device.repository.cloudSessionForEdit()
    assertEquals(null, session)
    val guard = device.repository.cloudEditGuard(session)
    when (provider) {
        CloudProvider.DROPBOX -> {
            device.application.koin.get<DropboxSettings>().setDropboxData("fixture-credentials")
            accounts.setDropboxAccount()
        }
        CloudProvider.GOOGLE_DRIVE -> {
            device.application.koin.get<GoogleDriveSettings>().setGoogleDriveLinked(true)
            accounts.setGoogleDriveAccount()
        }
        CloudProvider.ICLOUD -> {
            device.application.koin.get<ICloudSettings>().setUseICloud(true)
            accounts.setICloudAccount()
        }
    }
    val before = device.database.getFeedSources()
    assertFailsWith<IllegalStateException> {
        device.database.updateFeedSourceName(
            CloudDevice.source.id,
            "Stale local edit",
            cloudSessionId = session,
            withCurrentSession = guard,
        )
    }
    assertFailsWith<IllegalStateException> { device.repository.localEditCommitted(session) }
    assertEquals(before, device.database.getFeedSources())
}
