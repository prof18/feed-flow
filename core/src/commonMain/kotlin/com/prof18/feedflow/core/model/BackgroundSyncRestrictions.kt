package com.prof18.feedflow.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class BackgroundSyncRestrictions(
    val syncOnlyOnWifi: Boolean = false,
    val syncOnlyWhenCharging: Boolean = false,
)
