package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.utils.AppEnvironment
import com.russhwolf.settings.MapSettings
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsRepositoryCloudSessionTest {
    @Test
    fun `rotating cloud session waits for active cloud session block`() {
        val repository = SettingsRepository(MapSettings(), AppEnvironment.Debug)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val rotated = AtomicBoolean(false)
        val originalSession = repository.cloudSyncSession()

        val worker = thread(start = true) {
            repository.withCloudSession {
                entered.countDown()
                release.await(5, TimeUnit.SECONDS)
            }
        }
        assertTrue(entered.await(5, TimeUnit.SECONDS))

        val rotator = thread(start = true) {
            repository.rotateCloudSyncSession()
            rotated.set(true)
        }
        Thread.sleep(100)
        assertFalse(rotated.get())

        release.countDown()
        worker.join(5_000)
        rotator.join(5_000)
        assertTrue(rotated.get())
        assertTrue(repository.cloudSyncSession() != originalSession)
    }
}
