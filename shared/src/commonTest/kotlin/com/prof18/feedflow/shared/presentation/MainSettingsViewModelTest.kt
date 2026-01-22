package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals

class MainSettingsViewModelTest : KoinTestBase() {

    private val viewModel: MainSettingsViewModel by inject()

    @Test
    fun `state is loaded from settings repository on init`() = runTest {
        viewModel.settingsState.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)
        }
    }

    @Test
    fun `updateThemeMode updates state`() = runTest {
        viewModel.settingsState.test {
            awaitItem() // initial state

            viewModel.updateThemeMode(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, awaitItem().themeMode)

            viewModel.updateThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)

            viewModel.updateThemeMode(ThemeMode.OLED)
            assertEquals(ThemeMode.OLED, awaitItem().themeMode)

            viewModel.updateThemeMode(ThemeMode.SYSTEM)
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)
        }
    }
}
