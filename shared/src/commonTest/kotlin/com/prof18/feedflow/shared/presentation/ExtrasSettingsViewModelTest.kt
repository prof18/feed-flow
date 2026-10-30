package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtrasSettingsViewModelTest : KoinTestBase() {

    private val viewModel: ExtrasSettingsViewModel by inject()
    private val settingsRepository: SettingsRepository by inject()

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

    @Test
    fun `state loads the persisted language override`() = runTest {
        settingsRepository.setForceEnglishEnabled(true)
        viewModel.state.test {
            assertTrue(awaitItem().isForceEnglishEnabled)
        }
    }

    @Test
    fun `language toggle updates state and persisted settings`() = runTest {
        viewModel.state.test {
            assertFalse(awaitItem().isForceEnglishEnabled)

            viewModel.updateForceEnglishEnabled(true)
            assertTrue(awaitItem().isForceEnglishEnabled)
            assertTrue(settingsRepository.getForceEnglishEnabled())

            viewModel.updateForceEnglishEnabled(false)
            assertFalse(awaitItem().isForceEnglishEnabled)
            assertFalse(settingsRepository.getForceEnglishEnabled())
        }
    }
}
