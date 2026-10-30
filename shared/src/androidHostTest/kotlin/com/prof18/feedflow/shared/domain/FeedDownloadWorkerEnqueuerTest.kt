package com.prof18.feedflow.shared.domain

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.testing.WorkManagerTestInitHelper
import app.cash.turbine.test
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.koin.test.inject
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.Executor
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FeedDownloadWorkerEnqueuerTest : KoinTestBase() {
    private val settingsRepository: SettingsRepository by inject()
    private lateinit var context: Context
    private lateinit var manager: WorkManager
    private lateinit var enqueuer: FeedDownloadWorkerEnqueuer

    @BeforeTest
    fun setupWorkManager() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setExecutor(Executor { it.run() })
                .setWorkerCoroutineContext(testDispatcher)
                .build(),
        )
        manager = WorkManager.getInstance(context)
        enqueuer = FeedDownloadWorkerEnqueuer(settingsRepository, context)
    }

    @AfterTest
    fun closeWorkManager() {
        manager.cancelAllWork().result.get()
        shadowOf(Looper.getMainLooper()).idle()
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    @Test
    fun `refresh shows loading before network constraints allow the worker to start`() = runTest(testDispatcher) {
        enqueuer.widgetRefreshQueued.test {
            assertFalse(awaitItem())

            enqueuer.enqueueWidgetRefresh()

            assertTrue(awaitItem())
            val work = manager.getWorkInfosByTag(FeedDownloadWorker::class.java.name).get().single()
            assertEquals(WorkInfo.State.ENQUEUED, work.state)
            // A new widget session must recover pending feedback from WorkManager, not process memory.
            val newEnqueuer = FeedDownloadWorkerEnqueuer(settingsRepository, context)
            assertTrue(newEnqueuer.widgetRefreshQueued.first())

            manager.cancelWorkById(work.id).await()

            assertFalse(awaitItem())
        }
    }

    @Test
    fun `repeated refresh clicks keep a single pending download`() = runTest(testDispatcher) {
        enqueuer.enqueueWidgetRefresh()
        val firstWork = manager.getWorkInfosByTag(FeedDownloadWorker::class.java.name).get().single()

        enqueuer.enqueueWidgetRefresh()

        val work = manager.getWorkInfosByTag(FeedDownloadWorker::class.java.name).get().single()
        assertEquals(firstWork.id, work.id)
        assertEquals(WorkInfo.State.ENQUEUED, work.state)
        assertTrue(enqueuer.widgetRefreshQueued.first())
    }
}
