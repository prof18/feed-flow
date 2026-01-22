package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtrasSettingsViewModelTest : KoinTestBase() {

    private val viewModel: ExtrasSettingsViewModel by inject()

    @Test
    fun `state is loaded from settings repository on init`() = runTest {
        viewModel.state.test {
            val initialState = awaitItem()
            // Default value from SettingsRepository is false
            assertFalse(initialState.isReduceMotionEnabled)
        }
    }

    @Test
    fun `updateReduceMotionEnabled updates state`() = runTest {
        viewModel.state.test {
            awaitItem()

            viewModel.updateReduceMotionEnabled(true)
            assertTrue(awaitItem().isReduceMotionEnabled)

            viewModel.updateReduceMotionEnabled(false)
            assertFalse(awaitItem().isReduceMotionEnabled)
        }
    }
}
