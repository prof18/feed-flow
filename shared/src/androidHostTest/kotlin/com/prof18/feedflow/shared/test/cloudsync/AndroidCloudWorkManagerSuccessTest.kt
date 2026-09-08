package com.prof18.feedflow.shared.test.cloudsync

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.domain.feedsync.SyncWorkManager
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.Executor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidCloudWorkManagerSuccessTest : KoinTestBase() {
    @Test
    fun `queued Dropbox backup runs the real Android worker to success`() = verifyQueue(CloudProvider.DROPBOX)

    @Test
    fun `queued Drive backup runs the real Android worker to success`() = verifyQueue(CloudProvider.GOOGLE_DRIVE)

    @Test
    fun `queued Dropbox backup preserves existing cloud state during upgrade`() =
        verifyUpgradeQueue(CloudProvider.DROPBOX)

    @Test
    fun `queued Drive backup preserves existing cloud state during upgrade`() =
        verifyUpgradeQueue(CloudProvider.GOOGLE_DRIVE)

    private fun verifyQueue(provider: CloudProvider) = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = CloudStore()
        val device = createCloudDevice(provider, store, "work-manager")
        val recipient = createCloudDevice(provider, store, "recipient")
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setExecutor(Executor { it.run() })
                .setWorkerCoroutineContext(testDispatcher)
                .setWorkerFactory(DeviceWorkerFactory(device))
                .build(),
        )
        val manager = WorkManager.getInstance(context)
        try {
            device.seed()
            device.read("article-one", true)
            device.bookmark("article-two", true)
            device.application.koin.get<FeedSyncWorker>().upload()
            val query = WorkQuery.Builder.fromTags(listOf(SyncWorkManager::class.java.name)).build()
            val work = manager.getWorkInfos(query).get().single()
            if (work.state == WorkInfo.State.ENQUEUED) {
                requireNotNull(WorkManagerTestInitHelper.getTestDriver(context)).setAllConstraintsMet(work.id)
            }
            testDispatcher.scheduler.advanceUntilIdle()
            shadowOf(Looper.getMainLooper()).idle()
            val completed = requireNotNull(manager.getWorkInfoById(work.id).get())
            assertEquals(WorkInfo.State.SUCCEEDED, completed.state)
            assertFalse(device.settings.getIsSyncUploadRequired())
            assertEquals(1, store.fileCount(provider))
            recipient.refresh()
            assertEquals(device.flags(), recipient.flags())
        } finally {
            manager.cancelAllWork().result.get()
            shadowOf(Looper.getMainLooper()).idle()
            WorkManagerTestInitHelper.closeWorkDatabase()
            try {
                device.close()
            } finally {
                recipient.close()
            }
        }
    }

    private fun verifyUpgradeQueue(provider: CloudProvider) = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = CloudStore()
        val uploader = createCloudDevice(provider, store, "recovery-uploader")
        val device = createCloudDevice(provider, store, "recovery-device")
        val recipient = createCloudDevice(provider, store, "recovery-recipient")
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setExecutor(Executor { it.run() })
                .setWorkerCoroutineContext(testDispatcher)
                .setWorkerFactory(DeviceWorkerFactory(device))
                .build(),
        )
        val manager = WorkManager.getInstance(context)
        try {
            uploader.seed()
            uploader.read("article-one", true)
            uploader.bookmark("article-two", true)
            uploader.backup()
            val cloudFlags = uploader.flags()

            device.seed()
            device.syncDatabase.getAllFeedItems()
            device.database.updateReadStatus(FeedItemId("article-two"), true)
            device.settings.setIsSyncUploadRequired(true)
            device.refresh()
            val localBeforeBackup = device.flags()

            device.application.koin.get<FeedSyncWorker>().upload()
            val query = WorkQuery.Builder.fromTags(listOf(SyncWorkManager::class.java.name)).build()
            val work = manager.getWorkInfos(query).get().single()
            if (work.state == WorkInfo.State.ENQUEUED) {
                requireNotNull(WorkManagerTestInitHelper.getTestDriver(context)).setAllConstraintsMet(work.id)
            }
            testDispatcher.scheduler.advanceUntilIdle()
            shadowOf(Looper.getMainLooper()).idle()
            val completed = requireNotNull(manager.getWorkInfoById(work.id).get())
            assertEquals(WorkInfo.State.SUCCEEDED, completed.state)
            assertEquals(localBeforeBackup, device.flags())
            recipient.refresh()
            assertEquals(true to false, recipient.flags()["article-one"])
            assertEquals(false to true, recipient.flags()["article-two"])
            assertEquals(cloudFlags, recipient.flags())
            device.refresh()
            assertEquals(cloudFlags, device.flags())
            assertFalse(device.settings.getIsSyncUploadRequired())
        } finally {
            manager.cancelAllWork().result.get()
            shadowOf(Looper.getMainLooper()).idle()
            WorkManagerTestInitHelper.closeWorkDatabase()
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
}

private class DeviceWorkerFactory(private val device: CloudDevice) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = if (workerClassName == SyncWorkManager::class.java.name) {
        SyncWorkManager(device.application.koin.get(), appContext, workerParameters)
    } else {
        null
    }
}
