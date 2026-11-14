package com.prof18.feedflow.shared.domain.notification

import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.BackgroundSyncScheduler
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.test.KoinTestBase
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationRefreshEnablerTest : KoinTestBase() {

    private val recordingScheduler = RecordingBackgroundSyncScheduler()

    private val enabler: NotificationRefreshEnabler by inject()
    private val settingsRepository: SettingsRepository by inject()

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<BackgroundSyncScheduler> { recordingScheduler }
    }

    @Test
    fun `turns refresh on at one hour when it is off`() {
        settingsRepository.setSyncPeriod(SyncPeriod.NEVER)

        val result = enabler.enableIfOff()

        assertEquals(SyncPeriod.ONE_HOUR, result)
        assertEquals(SyncPeriod.ONE_HOUR, settingsRepository.getSyncPeriod())
        assertEquals(SyncPeriod.ONE_HOUR, recordingScheduler.lastPeriod)
    }

    @Test
    fun `keeps the period when refresh is already on`() {
        settingsRepository.setSyncPeriod(SyncPeriod.SIX_HOURS)

        val result = enabler.enableIfOff()

        assertNull(result)
        assertEquals(SyncPeriod.SIX_HOURS, settingsRepository.getSyncPeriod())
        assertNull(recordingScheduler.lastPeriod)
    }

    @Test
    fun `a second call after turning refresh on changes nothing`() {
        settingsRepository.setSyncPeriod(SyncPeriod.NEVER)
        enabler.enableIfOff()

        assertNull(enabler.enableIfOff())
        assertEquals(SyncPeriod.ONE_HOUR, settingsRepository.getSyncPeriod())
    }
}

private class RecordingBackgroundSyncScheduler : BackgroundSyncScheduler {
    var lastPeriod: SyncPeriod? = null

    override fun updateSyncPeriod(syncPeriod: SyncPeriod) {
        lastPeriod = syncPeriod
    }
}
