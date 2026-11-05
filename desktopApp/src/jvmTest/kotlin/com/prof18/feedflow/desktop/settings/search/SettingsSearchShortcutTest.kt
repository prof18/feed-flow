package com.prof18.feedflow.desktop.settings.search

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsSearchShortcutTest {

    @Test
    fun `settings search shortcut matches the platform modifier`() {
        assertTrue(isSettingsSearchShortcut(Key.F, KeyEventType.KeyDown, meta = true, ctrl = false, isMacOs = true))
        assertTrue(isSettingsSearchShortcut(Key.F, KeyEventType.KeyDown, meta = false, ctrl = true, isMacOs = false))
        assertFalse(isSettingsSearchShortcut(Key.F, KeyEventType.KeyDown, meta = false, ctrl = true, isMacOs = true))
        assertFalse(isSettingsSearchShortcut(Key.F, KeyEventType.KeyUp, meta = true, ctrl = false, isMacOs = true))
        assertFalse(isSettingsSearchShortcut(Key.G, KeyEventType.KeyDown, meta = true, ctrl = false, isMacOs = true))
    }
}
