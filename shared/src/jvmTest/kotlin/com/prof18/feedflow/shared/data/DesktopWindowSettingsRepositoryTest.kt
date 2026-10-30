package com.prof18.feedflow.shared.data

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
}
