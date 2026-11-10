package com.prof18.feedflow.shared.e2e

import com.prof18.feedflow.core.utils.AppDataPathBuilder
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DesktopE2eProperties
import com.prof18.feedflow.shared.di.desktopPreferencesNodeName
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopE2eIsolationTest {

    @AfterTest
    fun tearDown() {
        System.clearProperty(DesktopE2eProperties.DATA_DIR)
    }

    @Test
    fun `debug value is read in debug builds`() {
        assertEquals("x", DesktopE2eProperties.debugValue(AppEnvironment.Debug, DesktopE2eProperties.SEED) { "x" })
    }

    @Test
    fun `debug value is ignored in release builds`() {
        assertNull(DesktopE2eProperties.debugValue(AppEnvironment.Release, DesktopE2eProperties.SEED) { "x" })
    }

    @Test
    fun `blank debug value is ignored`() {
        assertNull(DesktopE2eProperties.debugValue(AppEnvironment.Debug, DesktopE2eProperties.SEED) { "  " })
    }

    @Test
    fun `data dir property overrides the debug data path`() {
        val dataDir = File(Files.createTempDirectory("ffe2e").toFile(), "data")
        System.setProperty(DesktopE2eProperties.DATA_DIR, dataDir.absolutePath)

        assertEquals(dataDir.absolutePath, AppDataPathBuilder.getAppDataPath(AppEnvironment.Debug))
        assertTrue(dataDir.isDirectory)
    }

    @Test
    fun `data dir property is ignored in release builds`() {
        val dataDir = File(Files.createTempDirectory("ffe2e").toFile(), "data")
        System.setProperty(DesktopE2eProperties.DATA_DIR, dataDir.absolutePath)

        assertTrue(AppDataPathBuilder.getAppDataPath(AppEnvironment.Release).endsWith("${File.separator}FeedFlow"))
    }

    @Test
    fun `preferences node name follows the environment and the property`() {
        val properties = mapOf(DesktopE2eProperties.PREFS_NODE to "feedflow-e2e")

        assertEquals("feedflow-e2e", desktopPreferencesNodeName(AppEnvironment.Debug) { properties[it] })
        assertEquals("feedflow-dev", desktopPreferencesNodeName(AppEnvironment.Debug) { null })
        assertEquals("feedflow", desktopPreferencesNodeName(AppEnvironment.Release) { "feedflow-e2e" })
    }
}
