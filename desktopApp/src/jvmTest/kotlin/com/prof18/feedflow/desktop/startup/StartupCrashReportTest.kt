package com.prof18.feedflow.desktop.startup

import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.i18n.feedFlowStrings
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.util.Locale
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StartupCrashReportTest {

    private val tempDir: File = Files.createTempDirectory("startup-crash").toFile()

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private val properties = Properties().apply {
        setProperty("user.home", "C:\\Users\\Jürgen")
        setProperty("user.dir", "C:\\Users\\Jürgen\\AppData\\Local\\FeedFlow")
        setProperty("os.name", "Windows 11")
        setProperty("os.version", "10.0")
        setProperty("os.arch", "amd64")
        setProperty("java.home", "C:\\Program Files\\WindowsApps\\FeedFlow\\runtime")
    }

    @Test
    fun `the report carries the environment and the stack trace`() {
        val report = startupCrashReportText(
            error = IllegalStateException("settings unreadable"),
            appVersion = "1.19.0",
            systemProperties = properties,
            now = Instant.parse("2026-10-28T10:00:00Z"),
        )

        assertTrue(report.contains("Version: 1.19.0"))
        assertTrue(report.contains("OS: Windows 11 10.0 (amd64)"))
        assertTrue(report.contains("Java home: C:\\Program Files\\WindowsApps\\FeedFlow\\runtime"))
        assertTrue(report.contains("Time: 2026-10-28T10:00:00Z"))
        assertTrue(report.contains("java.lang.IllegalStateException: settings unreadable"))
    }

    @Test
    fun `the report replaces the home folder so the account name stays private`() {
        val report = startupCrashReportText(
            error = IllegalStateException("cannot read C:\\Users\\Jürgen\\AppData\\Roaming\\FeedFlow"),
            appVersion = null,
            systemProperties = properties,
        )

        assertFalse(report.contains("Jürgen"))
        assertTrue(report.contains("Working directory: ~\\AppData\\Local\\FeedFlow"))
        assertTrue(report.contains("Version: unknown"))
    }

    @Test
    fun `the report is written to the first folder`() {
        val logFile = writeStartupCrashReport("boom", listOf(tempDir))

        assertEquals(File(tempDir, STARTUP_CRASH_LOG_FILE_NAME), logFile)
        assertEquals("boom", logFile?.readText())
    }

    @Test
    fun `an unusable folder falls back to the next one`() {
        val notAFolder = File(tempDir, "file").apply { writeText("") }
        val fallback = File(tempDir, "fallback")

        val logFile = writeStartupCrashReport("boom", listOf(notAFolder, fallback))

        assertEquals(File(fallback, STARTUP_CRASH_LOG_FILE_NAME), logFile)
    }

    @Test
    fun `no file is returned when no folder can be written`() {
        val notAFolder = File(tempDir, "file").apply { writeText("") }

        assertNull(writeStartupCrashReport("boom", listOf(notAFolder)))
    }

    @Test
    fun `strings follow the system language`() {
        assertSame(feedFlowStrings.getValue("it"), startupStrings(Locale.ITALY))
        assertSame(feedFlowStrings.getValue("zh_TW"), startupStrings(Locale.TAIWAN))
    }

    @Test
    fun `an unknown language falls back to English`() {
        assertSame(EnFeedFlowStrings, startupStrings(Locale.forLanguageTag("xx-YY")))
    }
}
