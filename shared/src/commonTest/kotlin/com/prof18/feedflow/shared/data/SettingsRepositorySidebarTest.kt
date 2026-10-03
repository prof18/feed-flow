package com.prof18.feedflow.shared.data

import com.prof18.feedflow.shared.test.KoinTestBase
import com.russhwolf.settings.Settings
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsRepositorySidebarTest : KoinTestBase() {
    private val settings: Settings by inject()

    @Test
    fun `sidebar uses the platform default until the user makes a choice`() {
        assertNull(SettingsRepository(settings).getLargeScreenSidebarVisible())
    }

    @Test
    fun `sidebar restores the last choice with a new repository`() {
        val repository = SettingsRepository(settings)

        for (visible in listOf(false, true, false)) {
            repository.setLargeScreenSidebarVisible(visible)

            assertEquals(visible, SettingsRepository(settings).getLargeScreenSidebarVisible())
        }
    }
}
