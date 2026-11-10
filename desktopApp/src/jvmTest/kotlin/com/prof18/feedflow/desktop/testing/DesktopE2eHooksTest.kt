package com.prof18.feedflow.desktop.testing

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DesktopE2eProperties
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopE2eHooksTest {

    private val tempDir = Files.createTempDirectory("ffe2e-hooks").toFile()

    @BeforeTest
    fun setUp() {
        DesktopE2eHooks.reset()
    }

    @AfterTest
    fun tearDown() {
        DesktopE2eHooks.reset()
        tempDir.deleteRecursively()
    }

    @Test
    fun `two part seed fills action and profile`() {
        install(DesktopE2eProperties.SEED to "reset-and-seed:content-rich")

        assertEquals(DesktopE2eSeedSpec("reset-and-seed", "content-rich", null, null), DesktopE2eHooks.seedSpec())
    }

    @Test
    fun `three part seed fills the account`() {
        install(DesktopE2eProperties.SEED to "reset-and-seed:sync-linked-mock:fresh_rss")

        assertEquals(
            DesktopE2eSeedSpec("reset-and-seed", "sync-linked-mock", "fresh_rss", null),
            DesktopE2eHooks.seedSpec(),
        )
    }

    @Test
    fun `audio url property fills the audio url`() {
        install(
            DesktopE2eProperties.SEED to "reset-and-seed:audio-episode",
            DesktopE2eProperties.SEED_AUDIO_URL to "https://example.com/episode.mp3?token=seed&part=1",
        )

        assertEquals("https://example.com/episode.mp3?token=seed&part=1", DesktopE2eHooks.seedSpec()?.audioUrl)
    }

    @Test
    fun `seed with one or four parts is rejected`() {
        install(DesktopE2eProperties.SEED to "reset-and-seed")
        assertFailsWith<IllegalArgumentException> { DesktopE2eHooks.seedSpec() }

        install(DesktopE2eProperties.SEED to "a:b:c:d")
        assertFailsWith<IllegalArgumentException> { DesktopE2eHooks.seedSpec() }
    }

    @Test
    fun `release builds ignore every property`() {
        val log = File(tempDir, "uris.txt")
        DesktopE2eHooks.install(AppEnvironment.Release) {
            mapOf(
                DesktopE2eProperties.SEED to "reset-and-seed:content-rich",
                DesktopE2eProperties.FILE_DIALOG_RESPONSE to File(tempDir, "response.txt").absolutePath,
                DesktopE2eProperties.OPENED_URI_LOG to log.absolutePath,
            )[it]
        }

        assertNull(DesktopE2eHooks.seedSpec())
        assertEquals(FileDialogScript.NotScripted, DesktopE2eHooks.fileDialogScript())
        assertFalse(DesktopE2eHooks.recordOpenedUri("https://a.b"))
        assertFalse(log.exists())
    }

    @Test
    fun `no response file configured means not scripted`() {
        install()

        assertEquals(FileDialogScript.NotScripted, DesktopE2eHooks.fileDialogScript())
    }

    @Test
    fun `response file decides cancel or choose on every call`() {
        val response = File(tempDir, "response.txt")
        install(DesktopE2eProperties.FILE_DIALOG_RESPONSE to response.absolutePath)

        assertEquals(FileDialogScript.Cancel, DesktopE2eHooks.fileDialogScript())

        response.writeText("\n")
        assertEquals(FileDialogScript.Cancel, DesktopE2eHooks.fileDialogScript())

        response.writeText("/tmp/x.opml\n")
        assertEquals(FileDialogScript.Choose(File("/tmp/x.opml")), DesktopE2eHooks.fileDialogScript())
    }

    @Test
    fun `opened uris are appended to the log in order`() {
        val log = File(tempDir, "uris.txt")
        install(DesktopE2eProperties.OPENED_URI_LOG to log.absolutePath)

        assertTrue(DesktopE2eHooks.recordOpenedUri("https://example.com/a"))
        assertTrue(DesktopE2eHooks.recordOpenedUri("mailto:a@b.c"))

        assertEquals(listOf("https://example.com/a", "mailto:a@b.c"), log.readLines())
    }

    @Test
    fun `no log configured records nothing`() {
        install()

        assertFalse(DesktopE2eHooks.recordOpenedUri("https://example.com"))
    }

    private fun install(vararg properties: Pair<String, String>) {
        val values = properties.toMap()
        DesktopE2eHooks.install(AppEnvironment.Debug) { values[it] }
    }
}
