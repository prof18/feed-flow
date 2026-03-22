package com.prof18.feedflow.shared.domain.model

data class BackgroundSyncRestrictions(
    val syncOnlyOnWifi: Boolean = false,
    val syncOnlyWhenCharging: Boolean = false,
)
