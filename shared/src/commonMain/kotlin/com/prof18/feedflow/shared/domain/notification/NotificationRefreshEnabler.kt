package com.prof18.feedflow.shared.domain.notification

import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.BackgroundSyncScheduler
import com.prof18.feedflow.shared.domain.model.SyncPeriod

internal class NotificationRefreshEnabler(
    private val settingsRepository: SettingsRepository,
    private val backgroundSyncScheduler: BackgroundSyncScheduler,
) {
    // Returns the period it switched to, or null when background refresh was already on.
    fun enableIfOff(): SyncPeriod? {
        if (settingsRepository.getSyncPeriod() != SyncPeriod.NEVER) return null
        settingsRepository.setSyncPeriod(PERIOD)
        backgroundSyncScheduler.updateSyncPeriod(PERIOD)
        return PERIOD
    }

    private companion object {
        val PERIOD = SyncPeriod.ONE_HOUR
    }
}
