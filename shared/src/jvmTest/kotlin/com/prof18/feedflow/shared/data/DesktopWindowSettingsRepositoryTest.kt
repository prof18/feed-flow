package com.prof18.feedflow.shared.data

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopWindowSettingsRepositoryTest {
    @Test
    fun `placement is null when never stored`() {
        val repository = DesktopWindowSettingsRepository(MapSettings())

        assertNull(repository.getDesktopWindowPlacement())
    }

    @Test
    fun `placement is persisted and restored`() {
        val repository = DesktopWindowSettingsRepository(MapSettings())

        repository.setDesktopWindowPlacement("Maximized")

        assertEquals("Maximized", repository.getDesktopWindowPlacement())
    }

    @Test
    fun `window size and position are persisted and restored`() {
        val repository = DesktopWindowSettingsRepository(MapSettings())

        repository.setDesktopWindowWidthDp(1024)
        repository.setDesktopWindowHeightDp(768)
        repository.setDesktopWindowXPositionDp(120f)
        repository.setDesktopWindowYPositionDp(64f)

        assertEquals(1024, repository.getDesktopWindowWidthDp())
        assertEquals(768, repository.getDesktopWindowHeightDp())
        assertEquals(120f, repository.getDesktopWindowXPositionDp())
        assertEquals(64f, repository.getDesktopWindowYPositionDp())
    }

    @Test
    fun `no saved window state until a window value is stored`() {
        val settings = MapSettings()
        settings.putString("THEME_MODE", "DARK")
        val repository = DesktopWindowSettingsRepository(settings)

        assertFalse(repository.hasSavedWindowState())

        repository.setDesktopWindowYPositionDp(10f)

        assertTrue(repository.hasSavedWindowState())
    }

    @Test
    fun `clearing the window state keeps the other preferences`() {
        val settings = MapSettings()
        settings.putString("THEME_MODE", "DARK")
        val repository = DesktopWindowSettingsRepository(settings)
        repository.setDesktopWindowWidthDp(1024)
        repository.setDesktopWindowXPositionDp(1f)
        repository.setDesktopWindowYPositionDp(2f)
        repository.setDesktopWindowPlacement("Floating")

        repository.clearDesktopWindowState()

        assertFalse(repository.hasSavedWindowState())
        assertEquals("DARK", settings.getStringOrNull("THEME_MODE"))
    }

    @Test
    fun `removing the position keeps the size`() {
        val repository = DesktopWindowSettingsRepository(MapSettings())
        repository.setDesktopWindowWidthDp(1024)
        repository.setDesktopWindowXPositionDp(1f)
        repository.setDesktopWindowYPositionDp(2f)

        repository.removeDesktopWindowPosition()

        assertNull(repository.getDesktopWindowXPositionDp())
        assertNull(repository.getDesktopWindowYPositionDp())
        assertEquals(1024, repository.getDesktopWindowWidthDp())
    }

    @Test
    fun `flush calls the preferences delegate`() {
        var flushes = 0
        val repository = DesktopWindowSettingsRepository(MapSettings(), flush = { flushes++ })

        repository.flushDesktopWindowState()

        assertEquals(1, flushes)
    }
}
