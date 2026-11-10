package com.prof18.feedflow.desktop.utils

import androidx.compose.ui.platform.UriHandler
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DesktopE2eProperties
import com.prof18.feedflow.desktop.testing.DesktopE2eHooks
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopUriHandlerTest {

    private val log = File.createTempFile("ffe2e-uris", ".txt").apply { delete() }

    @BeforeTest
    fun setUp() {
        val properties = mapOf(DesktopE2eProperties.OPENED_URI_LOG to log.absolutePath)
        DesktopE2eHooks.install(AppEnvironment.Debug) { properties[it] }
    }

    @AfterTest
    fun tearDown() {
        DesktopE2eHooks.reset()
        log.delete()
    }

    @Test
    fun `E2E uri log records a browser uri instead of opening it`() {
        assertTrue(openDesktopUriSafely("https://example.com"))

        assertEquals(listOf("https://example.com"), log.readLines())
    }

    @Test
    fun `E2E uri log records a uri handler uri instead of opening it`() {
        var opened = false
        val handler = object : UriHandler {
            override fun openUri(uri: String) {
                opened = true
            }
        }

        assertTrue(handler.openUriSafely("https://example.com/article"))

        assertFalse(opened)
        assertEquals(listOf("https://example.com/article"), log.readLines())
    }

    @Test
    fun `E2E uri log ignores uris that cannot be opened`() {
        assertFalse(openDesktopUriSafely("javascript:alert(1)"))

        assertFalse(log.exists())
    }
}
