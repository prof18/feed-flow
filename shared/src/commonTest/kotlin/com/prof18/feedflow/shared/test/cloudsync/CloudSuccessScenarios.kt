package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal enum class CloudSuccessScenario {
    ROUND_TRIP,
    SEQUENTIAL_BIDIRECTIONAL,
    SUPPORTED_FALSE_TRANSITIONS,
    REPEATED_SYNC,
    NORMAL_RESTART,
    BULK_READ,
    ADD_SUBSCRIPTION,
}

internal suspend fun runCloudSuccessScenario(provider: CloudProvider, scenario: CloudSuccessScenario) = coroutineScope {
    val store = CloudStore()
    var device1 = createCloudDevice(provider, store, "device1")
    val device2 = createCloudDevice(provider, store, "device2")
    val outcomes = mutableListOf<SyncResult>()
    var collector = launch(start = CoroutineStart.UNDISPATCHED) {
        device1.application.koin.get<FeedSyncMessageQueue>().messageQueue.collect { outcomes.add(it) }
    }
    try {
        assertNotEquals(device1.root, device2.root)
        device1.seed()
        assertTrue(device2.database.getFeedSources().isEmpty())
        device1.read("article-one", true)
        device1.bookmark("article-two", true)
        assertTrue(device1.settings.getIsSyncUploadRequired())
        device1.backup()
        assertFalse(device1.settings.getIsSyncUploadRequired())
        if (provider == CloudProvider.ICLOUD) assertEquals(0, store.fileCount(provider))
        store.propagate("device1")
        assertEquals(1, store.fileCount(provider))
        assertTrue(store.snapshot().single().bytes.size > 100)
        device2.refresh()
        assertEquals(listOf(CloudDevice.category), device2.database.getFeedSourceCategories())
        assertEquals(listOf(CloudDevice.source.id), device2.database.getFeedSources().map { it.id })
        assertEquals(device1.flags(), device2.flags())
        when (scenario) {
            CloudSuccessScenario.ROUND_TRIP -> Unit
            CloudSuccessScenario.SEQUENTIAL_BIDIRECTIONAL -> {
                device2.bookmark("article-one", true)
                device2.backup()
                store.propagate("device2")
                device1.refresh()
                assertEquals(device2.flags(), device1.flags())
                assertEquals(true to true, device1.flags()["article-one"])
            }
            CloudSuccessScenario.SUPPORTED_FALSE_TRANSITIONS -> {
                device1.bookmark("article-one", true)
                device1.read("article-two", true)
                device1.backup()
                store.propagate("device1")
                device2.refresh()
                device1.read("article-one", false)
                device1.bookmark("article-two", false)
                device1.backup()
                store.propagate("device1")
                device2.refresh()
                assertEquals(false to true, device2.flags()["article-one"])
                assertEquals(true to false, device2.flags()["article-two"])
            }
            CloudSuccessScenario.REPEATED_SYNC -> {
                val expected = device2.flags()
                device1.backup()
                store.propagate("device1")
                device2.refresh()
                device2.refresh()
                assertEquals(expected, device2.flags())
                assertEquals(1, store.fileCount(provider))
                assertEquals(1, device2.database.getFeedSources().size)
            }
            CloudSuccessScenario.NORMAL_RESTART -> {
                val path = device1.root
                val expected = device1.flags()
                collector.cancel()
                device1.close()
                assertEquals(expected, device2.flags())
                device1 = createCloudDevice(provider, store, "device1", root = path)
                collector = launch(start = CoroutineStart.UNDISPATCHED) {
                    device1.application.koin.get<FeedSyncMessageQueue>().messageQueue.collect { outcomes.add(it) }
                }
                assertEquals(expected, device1.flags())
                assertFalse(device1.settings.getIsSyncUploadRequired())
                device1.bookmark("article-one", true)
                device1.backup()
                store.propagate("device1")
                device2.refresh()
                assertEquals(device1.flags(), device2.flags())
                assertEquals(1, store.fileCount(provider))
            }
            CloudSuccessScenario.ADD_SUBSCRIPTION -> {
                device1.addSubscription()
                store.propagate("device1")
                device2.refresh()
                assertEquals(2, device2.database.getFeedSources().size)
                assertEquals(2, device2.database.getFeedSourceCategories().size)
                assertEquals(device1.flags(), device2.flags())
            }
            CloudSuccessScenario.BULK_READ -> {
                device1.markAllRead()
                device1.backup()
                store.propagate("device1")
                device2.refresh()
                assertTrue(device2.flags().values.all { it.first })
                assertEquals(true to true, device2.flags()["article-two"])
            }
        }
        assertTrue(outcomes.isNotEmpty(), "The actual worker must emit completion")
        assertTrue(outcomes.all { !it.isError() }, "Unexpected sync outcomes: $outcomes")
        println(
            "CLOUD_SYNC_SUCCESS provider=$provider scenario=$scenario bytes=${store.snapshot().single().bytes.size}",
        )
    } finally {
        collector.cancel()
        try {
            device1.close()
        } finally {
            device2.close()
        }
    }
}
