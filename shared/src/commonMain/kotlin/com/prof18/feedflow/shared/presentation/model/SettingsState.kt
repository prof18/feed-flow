package com.prof18.feedflow.shared.presentation.model

import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.shared.domain.model.SyncPeriod

data class SettingsState(
    val isMarkReadWhenScrollingEnabled: Boolean = true,
    val isShowReadItemsEnabled: Boolean = false,
    val isReaderModeEnabled: Boolean = false,
    val isRemoveTitleFromDescriptionEnabled: Boolean = false,
    val isHideDescriptionEnabled: Boolean = false,
    val isHideImagesEnabled: Boolean = false,
    val autoDeletePeriod: AutoDeletePeriod = AutoDeletePeriod.DISABLED,
    val isCrashReportingEnabled: Boolean = true,
    val syncPeriod: SyncPeriod = SyncPeriod.ONE_HOUR,
)
