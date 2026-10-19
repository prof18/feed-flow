package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.junit4.v2.createComposeRule
import coil3.PlatformContext
import coil3.executeBlocking
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.prof18.feedflow.desktop.ui.DesktopUiEnvironment
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.LocalReduceMotion
import com.prof18.feedflow.desktop.ui.coilImageLoader
import com.prof18.feedflow.desktop.ui.desktopExternalLinkAction
import com.prof18.feedflow.i18n.DeFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DesktopPlatformAdaptersTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `locale and reduced motion update together and survive content recreation`() {
        val language = mutableStateOf("en")
        val reduced = mutableStateOf(false)
        val mounted = mutableStateOf(true)
        var observedTitle = ""
        var observedMotion = false
        rule.setContent {
            DesktopUiEnvironment(
                darkTheme = false,
                currentLanguageTag = language.value,
                reduceMotionEnabled = reduced.value,
            ) {
                if (mounted.value) {
                    val title = LocalFeedFlowStrings.current.settingsTitle
                    val motion = LocalReduceMotion.current
                    SideEffect {
                        observedTitle = title
                        observedMotion = motion
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(EnFeedFlowStrings.settingsTitle, observedTitle)
            language.value = "de"
            reduced.value = true
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(DeFeedFlowStrings.settingsTitle, observedTitle)
            assertTrue(observedMotion)
            mounted.value = false
        }
        rule.waitForIdle()
        rule.runOnIdle { mounted.value = true }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(DeFeedFlowStrings.settingsTitle, observedTitle)
            assertTrue(observedMotion)
        }
    }

    @Test
    fun `safe external link callback uses the recreated handler and reports launch errors`() {
        val mounted = mutableStateOf(true)
        val opened = mutableListOf<String>()
        val handler = mutableStateOf<UriHandler>(object : UriHandler {
            override fun openUri(uri: String) {
                error("No browser available")
            }
        })
        var failures = 0
        var callback: (String) -> Unit = {}
        rule.setContent {
            if (mounted.value) {
                DesktopUiEnvironment(darkTheme = false) {
                    val action = remember(handler.value) {
                        desktopExternalLinkAction(handler.value) { failures++ }
                    }
                    SideEffect { callback = action }
                }
            }
        }
        rule.runOnIdle {
            callback("https://feedflow.dev")
            callback("javascript:alert(1)")
            assertEquals(2, failures)
            mounted.value = false
        }
        rule.waitForIdle()
        rule.runOnIdle {
            handler.value = object : UriHandler {
                override fun openUri(uri: String) {
                    opened += uri
                }
            }
            mounted.value = true
        }
        rule.waitForIdle()
        rule.runOnIdle {
            callback("feedflow.dev")
            assertEquals(listOf("https://feedflow.dev"), opened)
            assertEquals(2, failures)
        }
    }

    @Test
    fun `environment recreation leaves the application image loader usable`() {
        val mounted = mutableStateOf(true)
        val loader = coilImageLoader(PlatformContext.INSTANCE, debug = false)
        var observed: Any? = null
        try {
            rule.setContent {
                if (mounted.value) {
                    DesktopUiEnvironment(darkTheme = false) {
                        SideEffect { observed = loader }
                    }
                }
            }
            rule.runOnIdle {
                assertSame(loader, observed)
                mounted.value = false
            }
            rule.waitForIdle()
            rule.runOnIdle { mounted.value = true }
            rule.waitForIdle()
            rule.runOnIdle { assertSame(loader, observed) }
            val request = ImageRequest.Builder(PlatformContext.INSTANCE)
                .data(
                    Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
                    ),
                )
                .build()
            assertIs<SuccessResult>(loader.executeBlocking(request))
        } finally {
            loader.shutdown()
        }
    }
}
