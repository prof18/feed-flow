package com.prof18.feedflow.desktop

import com.prof18.feedflow.core.utils.AppEnvironment
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopAppConfigTest {
    @Test
    fun `release properties keep release behavior and configured sync providers`() {
        val config = createAppConfig(
            Properties().apply {
                setProperty("is_release", "true")
                setProperty("dropbox_key", "fixture-only-key")
            },
            isIcloudEnabled = false,
        )

        assertEquals(AppEnvironment.Release, config.appEnvironment)
        assertTrue(config.isDropboxEnabled)
        assertTrue(config.isGoogleDriveEnabled)
    }

    @Test
    fun `missing keys fall back to debug without Dropbox and flatpak hides Google Drive`() {
        val config = createAppConfig(Properties().apply { setProperty("flatpak", "true") }, isIcloudEnabled = false)

        assertEquals(AppEnvironment.Debug, config.appEnvironment)
        assertFalse(config.isDropboxEnabled)
        assertFalse(config.isGoogleDriveEnabled)
    }

    @Test
    fun `Jewel theme loads on the desktop runtime`() {
        Class.forName("org.jetbrains.jewel.intui.standalone.theme.IntUiThemeKt")
    }
}
