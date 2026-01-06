package com.prof18.feedflow.shared.presentation.model

import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.shared.domain.model.SyncPeriod

data class SyncAndStorageState(
    val syncPeriod: SyncPeriod = SyncPeriod.ONE_HOUR,
    val autoDeletePeriod: AutoDeletePeriod = AutoDeletePeriod.DISABLED,
    val refreshFeedsOnLaunch: Boolean = true,
)
