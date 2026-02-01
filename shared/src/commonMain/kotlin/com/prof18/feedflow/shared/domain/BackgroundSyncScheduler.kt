package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.shared.domain.model.SyncPeriod

fun interface BackgroundSyncScheduler {
    fun updateSyncPeriod(syncPeriod: SyncPeriod)
}
