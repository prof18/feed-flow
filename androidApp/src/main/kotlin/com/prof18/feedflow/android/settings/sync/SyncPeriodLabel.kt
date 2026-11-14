package com.prof18.feedflow.android.settings.sync

import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.shared.domain.model.SyncPeriod

internal fun SyncPeriod.label(strings: FeedFlowStrings): String = when (this) {
    SyncPeriod.NEVER -> strings.settingsSyncPeriodNever
    SyncPeriod.FIFTEEN_MINUTES -> strings.settingsSyncPeriodFifteenMinutes
    SyncPeriod.THIRTY_MINUTES -> strings.settingsSyncPeriodThirtyMinutes
    SyncPeriod.ONE_HOUR -> strings.settingsSyncPeriodOneHour
    SyncPeriod.TWO_HOURS -> strings.settingsSyncPeriodTwoHours
    SyncPeriod.SIX_HOURS -> strings.settingsSyncPeriodSixHours
    SyncPeriod.TWELVE_HOURS -> strings.settingsSyncPeriodTwelveHours
    SyncPeriod.ONE_DAY -> strings.settingsSyncPeriodOneDay
}
