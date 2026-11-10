package com.prof18.feedflow.desktop.e2e.smoke

import com.prof18.feedflow.desktop.e2e.harness.DesktopE2eRule
import com.prof18.feedflow.desktop.e2e.harness.Seed
import org.junit.Rule
import org.junit.Test

class SmokeFlowsTest {

    @get:Rule
    val e2e = DesktopE2eRule()

    /**
     * empty-state message: desktop home/DesktopHomeScreenContent.kt:400 (emptyFeedMessage)
     *
     * The NoFeedsDialog does not open on its own: its only trigger, onEmptyStateClick, is passed down
     * to DesktopHomeScreenContent but never called, so the flow asserts the empty message only.
     */
    @Test
    fun `SM-001 first launch empty`() {
        val flow = e2e.start(Seed.Empty)

        flow.awaitText(flow.strings.emptyFeedMessage)
        flow.assertAbsent(flow.strings.noFeedModalTitle)
    }
}
