package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal suspend fun runQueuedCloudSuccess(provider: CloudProvider) = coroutineScope {
    val store = CloudStore()
    val device1 = createCloudDevice(provider, store, "queued-device1")
    val device2 = createCloudDevice(provider, store, "queued-device2")
    try {
        device1.seed()
        device1.read("article-one", true)
        device1.bookmark("article-two", true)
        val completion = async(start = CoroutineStart.UNDISPATCHED) {
            device1.application.koin.get<FeedSyncMessageQueue>().messageQueue.first()
        }
        device1.application.koin.get<FeedSyncWorker>().upload()
        assertFalse(completion.await().isError())
        assertFalse(device1.settings.getIsSyncUploadRequired())
        store.propagate("queued-device1")
        assertEquals(1, store.fileCount(provider))
        device2.refresh()
        assertEquals(device1.flags(), device2.flags())
        println("CLOUD_SYNC_QUEUED_SUCCESS provider=$provider")
    } finally {
        try {
            device1.close()
        } finally {
            device2.close()
        }
    }
}
