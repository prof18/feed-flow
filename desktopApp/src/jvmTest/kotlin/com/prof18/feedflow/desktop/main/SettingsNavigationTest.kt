package com.prof18.feedflow.desktop.main

import com.prof18.feedflow.desktop.settings.DesktopSettingsCategory
import com.prof18.feedflow.desktop.settings.DesktopSettingsSection
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsNavigationTest {
    @Test
    fun `settings shortcuts reuse one window and request activation of the target page`() {
        val navigator = DesktopDialogWindowNavigator()
        navigator.openSettings(DesktopSettingsCategory.ACCOUNTS)
        val firstActivation = navigator.settingsActivationRequest
        navigator.openSettings(DesktopSettingsCategory.SYNC_STORAGE, DesktopSettingsSection.IMPORT_EXPORT)
        assertEquals(DesktopSettingsCategory.SYNC_STORAGE, navigator.settingsCategory)
        assertEquals(DesktopSettingsSection.IMPORT_EXPORT, navigator.settingsSection)
        assertEquals(1, DesktopDialogWindowDestination.entries.count(navigator::isOpen))
        assertTrue(navigator.settingsActivationRequest > firstActivation)
    }

    @Test
    fun `ordinary Settings opening remembers the last page after closing`() {
        val navigator = DesktopDialogWindowNavigator()
        navigator.openSettings()
        navigator.selectSettingsCategory(DesktopSettingsCategory.READING)
        navigator.close(DesktopDialogWindowDestination.Settings)
        navigator.openSettings()
        assertEquals(DesktopSettingsCategory.READING, navigator.settingsCategory)
        assertTrue(navigator.isOpen(DesktopDialogWindowDestination.Settings))
    }

    @Test
    fun `explicit page shortcut exits a nested settings section`() {
        val navigator = DesktopDialogWindowNavigator()
        navigator.openSettings(DesktopSettingsCategory.SYNC_STORAGE, DesktopSettingsSection.IMPORT_EXPORT)
        navigator.openSettings(DesktopSettingsCategory.FILTERS)
        assertEquals(DesktopSettingsCategory.FILTERS, navigator.settingsCategory)
        assertNull(navigator.settingsSection)
    }
}
