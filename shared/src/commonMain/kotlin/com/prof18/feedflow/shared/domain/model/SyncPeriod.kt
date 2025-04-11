package com.prof18.feedflow.shared.domain.model

@Suppress("MagicNumber")
enum class SyncPeriod(val minutes: Long) {
    NEVER(-1),
    FIFTEEN_MINUTES(15),
    THIRTY_MINUTES(30),
    ONE_HOUR(60),
    TWO_HOURS(120),
    SIX_HOURS(360),
    TWELVE_HOURS(720),
    ONE_DAY(1440),
}
