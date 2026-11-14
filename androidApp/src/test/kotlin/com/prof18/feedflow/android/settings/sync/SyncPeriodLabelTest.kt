package com.prof18.feedflow.android.settings.sync

import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPeriodLabelTest {
    @Test
    fun neverUsesTheNeverLabel() {
        assertEquals(EnFeedFlowStrings.settingsSyncPeriodNever, SyncPeriod.NEVER.label(EnFeedFlowStrings))
    }

    @Test
    fun oneHourUsesTheOneHourLabel() {
        assertEquals(EnFeedFlowStrings.settingsSyncPeriodOneHour, SyncPeriod.ONE_HOUR.label(EnFeedFlowStrings))
    }

    @Test
    fun everyPeriodHasALabel() {
        SyncPeriod.entries.forEach { period ->
            assertTrue(period.name, period.label(EnFeedFlowStrings).isNotEmpty())
        }
    }
}
