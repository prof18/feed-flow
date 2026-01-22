package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AboutAndSupportSettingsViewModelTest : KoinTestBase() {

    private val viewModel: AboutAndSupportSettingsViewModel by inject()

    @Test
    fun `state is loaded from settings repository on init`() = runTest {
        viewModel.state.test {
            val initialState = awaitItem()
            // Default value from SettingsRepository is true
            assertTrue(initialState.isCrashReportingEnabled)
        }
    }

    @Test
    fun `updateCrashReporting updates state`() = runTest {
        viewModel.state.test {
            awaitItem()

            viewModel.updateCrashReporting(false)
            assertFalse(awaitItem().isCrashReportingEnabled)

            viewModel.updateCrashReporting(true)
            assertTrue(awaitItem().isCrashReportingEnabled)
        }
    }
}
