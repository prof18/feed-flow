package com.prof18.feedflow.desktop

import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopConfigTest {
    @Test
    fun `local builds have version metadata without private properties`() {
        val config = createAppConfig(Properties(), isIcloudEnabled = false)

        assertEquals(DesktopBuildInfo.VERSION, config.version)
        assertTrue(config.version?.matches(Regex("\\d+\\.\\d+\\.\\d+")) == true)
    }

    @Test
    fun `configured release version takes precedence over generated version`() {
        val properties = Properties().apply { setProperty("version", "2.0.0") }

        assertEquals("2.0.0", createAppConfig(properties, isIcloudEnabled = false).version)
    }
}
