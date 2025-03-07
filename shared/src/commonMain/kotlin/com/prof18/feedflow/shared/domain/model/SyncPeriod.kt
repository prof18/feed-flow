package com.prof18.feedflow.shared.domain.model

@Suppress("MagicNumber")
enum class SyncPeriod(val hours: Long) {
    NEVER(-1),
    ONE_HOUR(1),
    TWO_HOURS(2),
    SIX_HOURS(6),
    TWELVE_HOURS(12),
}
