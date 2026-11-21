package com.prof18.feedflow.desktop.window

import com.prof18.feedflow.shared.data.DesktopWindowSettingsRepository
import com.prof18.winkeep.SavedMonitor
import com.prof18.winkeep.SavedPlacement
import com.prof18.winkeep.ScreenArea
import com.prof18.winkeep.WindowBounds
import com.prof18.winkeep.WindowOrigin
import com.prof18.winkeep.WindowSnapshot
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedFlowWindowStateStoreTest {

    private val settings = MapSettings()
    private var flushCount = 0
    private val repository = DesktopWindowSettingsRepository(settings, flush = { flushCount++ })
    private val store = FeedFlowWindowStateStore(repository)

    @Test
    fun `nothing saved loads no snapshot`() = runTest {
        assertNull(store.load())
    }

    @Test
    fun `legacy values written by older FeedFlow versions load as a snapshot`() = runTest {
        settings.putInt("DESKTOP_WINDOW_WIDTH_DP", 1280)
        settings.putInt("DESKTOP_WINDOW_HEIGHT_DP", 800)
        settings.putFloat("DESKTOP_WINDOW_X_POSITION_DP", 120.5f)
        settings.putFloat("DESKTOP_WINDOW_Y_POSITION_DP", -40f)
        settings.putString("DESKTOP_WINDOW_PLACEMENT", "Maximized")

        assertEquals(
            WindowSnapshot(
                normalBounds = WindowBounds(WindowOrigin(120.5, -40.0), width = 1280.0, height = 800.0),
                placement = SavedPlacement.Maximized,
                monitor = null,
            ),
            store.load(),
        )
    }

    @Test
    fun `a missing size falls back to the old default size`() = runTest {
        settings.putString("DESKTOP_WINDOW_PLACEMENT", "Floating")

        assertEquals(
            WindowSnapshot(WindowBounds(position = null, width = 800.0, height = 600.0), SavedPlacement.Floating),
            store.load(),
        )
    }

    @Test
    fun `a position with only one coordinate lets the platform place the window`() = runTest {
        settings.putInt("DESKTOP_WINDOW_WIDTH_DP", 1024)
        settings.putInt("DESKTOP_WINDOW_HEIGHT_DP", 768)
        settings.putFloat("DESKTOP_WINDOW_X_POSITION_DP", 50f)

        assertNull(store.load()?.normalBounds?.position)
    }

    @Test
    fun `fullscreen loads as fullscreen and unknown placements load as floating`() = runTest {
        settings.putString("DESKTOP_WINDOW_PLACEMENT", "Fullscreen")
        assertEquals(SavedPlacement.Fullscreen, store.load()?.placement)

        settings.putString("DESKTOP_WINDOW_PLACEMENT", "Minimized")
        assertEquals(SavedPlacement.Floating, store.load()?.placement)
    }

    @Test
    fun `an unusable saved size is rejected so WinKeep opens the default size`() = runTest {
        settings.putInt("DESKTOP_WINDOW_WIDTH_DP", 0)
        settings.putInt("DESKTOP_WINDOW_HEIGHT_DP", 600)

        assertFailsWith<IllegalArgumentException> { store.load() }
    }

    @Test
    fun `a saved snapshot is written to the legacy keys with their legacy types`() = runTest {
        store.save(
            WindowSnapshot(
                normalBounds = WindowBounds(WindowOrigin(-1500.25, 30.0), width = 1023.6, height = 767.4),
                placement = SavedPlacement.Fullscreen,
                monitor = SavedMonitor("Display 2", ScreenArea(-1920.0, 0.0, 1920.0, 1080.0)),
            ),
        )

        assertEquals(1024, settings.getIntOrNull("DESKTOP_WINDOW_WIDTH_DP"))
        assertEquals(767, settings.getIntOrNull("DESKTOP_WINDOW_HEIGHT_DP"))
        assertEquals(-1500.25f, settings.getFloatOrNull("DESKTOP_WINDOW_X_POSITION_DP"))
        assertEquals(30f, settings.getFloatOrNull("DESKTOP_WINDOW_Y_POSITION_DP"))
        assertEquals("Fullscreen", settings.getStringOrNull("DESKTOP_WINDOW_PLACEMENT"))
    }

    @Test
    fun `a saved snapshot loads back without its monitor`() = runTest {
        val bounds = WindowBounds(WindowOrigin(200.0, 100.0), width = 900.0, height = 700.0)
        val monitor = SavedMonitor("0", ScreenArea(0.0, 0.0, 1440.0, 900.0))
        store.save(WindowSnapshot(bounds, SavedPlacement.Maximized, monitor))

        assertEquals(WindowSnapshot(bounds, SavedPlacement.Maximized, monitor = null), store.load())
    }

    @Test
    fun `saving a platform-placed window removes the old position`() = runTest {
        settings.putFloat("DESKTOP_WINDOW_X_POSITION_DP", 10f)
        settings.putFloat("DESKTOP_WINDOW_Y_POSITION_DP", 20f)

        store.save(WindowSnapshot(WindowBounds(position = null, width = 800.0, height = 600.0)))

        assertNull(settings.getFloatOrNull("DESKTOP_WINDOW_X_POSITION_DP"))
        assertNull(settings.getFloatOrNull("DESKTOP_WINDOW_Y_POSITION_DP"))
        assertEquals("Floating", settings.getStringOrNull("DESKTOP_WINDOW_PLACEMENT"))
    }

    @Test
    fun `saving leaves unrelated preferences untouched`() = runTest {
        settings.putString("THEME_MODE", "DARK")

        store.save(WindowSnapshot(WindowBounds(WindowOrigin(0.0, 0.0), width = 800.0, height = 600.0)))

        assertEquals("DARK", settings.getStringOrNull("THEME_MODE"))
    }

    @Test
    fun `flush reaches the preferences delegate`() = runTest {
        store.flush()

        assertEquals(1, flushCount)
    }

    @Test
    fun `a failing flush propagates so WinKeep reports it`() = runTest {
        val failing = FeedFlowWindowStateStore(
            DesktopWindowSettingsRepository(MapSettings(), flush = { error("disk full") }),
        )

        assertFailsWith<IllegalStateException> { failing.flush() }
    }

    @Test
    fun `clear removes only the window keys and flushes`() = runTest {
        settings.putString("THEME_MODE", "DARK")
        store.save(WindowSnapshot(WindowBounds(WindowOrigin(0.0, 0.0), width = 800.0, height = 600.0)))

        store.clear()

        assertNull(store.load())
        assertEquals("DARK", settings.getStringOrNull("THEME_MODE"))
        assertTrue(flushCount > 0)
    }
}
