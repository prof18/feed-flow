@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.prof18.feedflow.desktop.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.platform.asAwtTransferable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.dragAndDrop
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import java.awt.Dialog
import java.awt.Window
import java.awt.datatransfer.DataFlavor
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopLicensesTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `inline details Back restores selected library and Done leaves licenses`() {
        var closes = 0
        content { LicensesContent(fixtures(), { closes++ }) }
        rule.onNodeWithText("Fixture library · 1.0").assertIsFocused()
        click("Fixture library · 1.0")
        rule.waitUntilExactlyOneExists(hasTestTag(DesktopLicensesE2eIds.DETAILS_DONE), timeoutMillis = 5_000)
        rule.onNodeWithTag(DesktopLicensesE2eIds.DETAILS_DONE).assertIsFocused()
        rule.onNodeWithTag(DesktopLicensesE2eIds.row("Fixture library")).assertDoesNotExist()
        rule.runOnIdle {
            assertTrue(
                Window.getWindows().filterIsInstance<Dialog>()
                    .none { it.isVisible && it.title == "Fixture library" },
            )
        }
        rule.onNodeWithTag(DesktopLicensesE2eIds.DETAILS_DONE)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.waitUntilDoesNotExist(hasTestTag(DesktopLicensesE2eIds.DETAILS_DONE), timeoutMillis = 5_000)
        rule.onNodeWithTag(DesktopLicensesE2eIds.DETAILS_DONE).assertDoesNotExist()
        rule.onNodeWithText("Fixture library · 1.0").assertIsFocused().assertIsSelected()
        rule.runOnIdle { assertEquals(0, closes) }
        rule.onNodeWithTag(DesktopLicensesE2eIds.LIST_DONE)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals(1, closes) }
    }

    @Test
    fun `empty or failed report has explicit feedback and a working close action`() {
        var closes = 0
        content { LicensesContent(null, { closes++ }, loadFailed = true) }
        rule.onNodeWithText(EnFeedFlowStrings.readerModeNoContentTitle).assertIsDisplayed()
        click(EnFeedFlowStrings.actionDone)
        rule.runOnIdle { assertEquals(1, closes) }
    }

    @Test
    fun `selection displays every license belonging to the selected library`() {
        content { LicensesContent(fixtures(), {}) }
        click("Fixture library · 1.0")
        rule.onNodeWithText("First complete license", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Second complete license", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Unrelated license", substring = true).assertDoesNotExist()
    }

    @Test
    fun `identical library labels expose their artifact identity and open the correct notices`() {
        val first = fixtures().libraries.first().copy(name = "Annotation", uniqueId = "androidx.annotation:annotation")
        val second = fixtures().libraries.last().copy(
            name = "Annotation",
            uniqueId = "org.jetbrains.compose.annotation-internal:annotation",
        )
        content { LicensesContent(Libs(listOf(first, second), first.licenses + second.licenses), {}) }
        listOf(first, second).forEach { library ->
            rule.onNodeWithContentDescription("Annotation · 1.0\n${library.uniqueId}")
                .performSemanticsAction(SemanticsActions.OnClick) { it() }
            rule.waitUntilExactlyOneExists(hasTestTag(DesktopLicensesE2eIds.DETAILS_DONE), timeoutMillis = 5_000)
            rule.onNodeWithTag(DesktopLicensesE2eIds.ARTIFACT_ID).assertIsDisplayed().assertTextEquals(library.uniqueId)
            library.licenses.forEach { license ->
                rule.onNodeWithText(requireNotNull(license.licenseContent)).assertIsDisplayed()
            }
            rule.onNodeWithTag(DesktopLicensesE2eIds.DETAILS_DONE)
                .performSemanticsAction(SemanticsActions.OnClick) { it() }
            rule.waitUntilDoesNotExist(hasTestTag(DesktopLicensesE2eIds.DETAILS_DONE), timeoutMillis = 5_000)
        }
    }

    @Test
    fun `every bundled artifact opens all of its complete notices`() {
        val report = bundledLibraries()
        content { LicensesContent(report, {}) }
        report.libraries.forEachIndexed { index, library ->
            rule.onNodeWithTag(DesktopLicensesE2eIds.LIST).performScrollToIndex(index)
            rule.onNodeWithTag(DesktopLicensesE2eIds.row(library.uniqueId))
                .assertIsDisplayed().performSemanticsAction(SemanticsActions.OnClick) { it() }
            rule.waitUntilExactlyOneExists(hasTestTag(DesktopLicensesE2eIds.DETAILS_DONE), timeoutMillis = 5_000)
            rule.onNodeWithTag(DesktopLicensesE2eIds.ARTIFACT_ID).assertIsDisplayed().assertTextEquals(library.uniqueId)
            if (library.licenses.isEmpty()) {
                rule.onNodeWithText(EnFeedFlowStrings.readerModeNoContentTitle).assertExists()
            }
            library.licenses.forEach { license ->
                val notice = license.licenseContent?.takeIf(String::isNotBlank)
                    ?: EnFeedFlowStrings.readerModeNoContentTitle
                rule.onNodeWithText(notice).assertExists()
            }
            rule.onNodeWithTag(DesktopLicensesE2eIds.DETAILS_DONE)
                .performSemanticsAction(SemanticsActions.OnClick) { it() }
            rule.waitUntilDoesNotExist(hasTestTag(DesktopLicensesE2eIds.DETAILS_DONE), timeoutMillis = 5_000)
        }
    }

    @Test
    fun `mouse selected notice copies its exact text with the platform shortcut`() {
        val clipboard = LicenseTestClipboard()
        content {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                LicenseDetails(fixtures().libraries.first(), {})
            }
        }
        val notice = "First complete license"
        val text = rule.onNodeWithText(notice)
        val layouts = mutableListOf<TextLayoutResult>()
        text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        val first = layout.getBoundingBox(0)
        val last = layout.getBoundingBox(notice.lastIndex)
        text.performMouseInput {
            dragAndDrop(start = Offset(first.left, first.center.y), end = Offset(last.right, last.center.y))
        }
        val shortcutKey = if (System.getProperty("os.name").startsWith("Mac")) Key.MetaLeft else Key.CtrlLeft
        text.performKeyInput {
            keyDown(shortcutKey)
            pressKey(Key.C)
            keyUp(shortcutKey)
        }
        rule.waitUntil(timeoutMillis = 5_000) { clipboard.text != null }
        rule.runOnIdle { assertEquals(notice, clipboard.text) }
    }

    @Test
    fun `library without license text opens explicit missing content feedback`() {
        content { LicensesContent(fixtures(), {}) }
        click("Missing library · 1.0")
        rule.onNodeWithText(EnFeedFlowStrings.readerModeNoContentTitle).assertIsDisplayed()
    }

    @Test
    fun `Done closes details and leaves the library list available`() {
        content { LicensesContent(fixtures(), {}) }
        click("Fixture library · 1.0")
        rule.onNodeWithTag(DesktopLicensesE2eIds.DETAILS_DONE)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithText("First complete license", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Fixture library · 1.0").assertIsDisplayed()
    }

    @Test
    fun `long license has a scrolling body and a readable pinned Done action`() {
        content { LicensesContent(fixtures(longText = true), {}) }
        click("Fixture library · 1.0")
        val license = hasText("First complete license", substring = true)
        rule.onNode(hasScrollAction() and (license or hasAnyDescendant(license)))
            .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, LICENSE_SCROLL_DISTANCE) }
        rule.onNodeWithTag(DesktopLicensesE2eIds.DETAILS_DONE).assertIsDisplayed()
    }

    @Test
    fun `large RTL list keeps its close action readable at compact size`() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                FeedFlowTheme(darkTheme = true, useOledTheme = true) {
                    Box(Modifier.size(width = 360.dp, height = 500.dp)) {
                        LicensesContent(fixtures(), {})
                    }
                }
            }
        }
        rule.onNodeWithText(EnFeedFlowStrings.actionDone).assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(EnFeedFlowStrings.actionDone)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(!layouts.single().hasVisualOverflow)
    }

    @Test
    fun `bundled library report includes the active Jewel dependencies`() {
        val libraries = bundledLibraries()
        assertTrue(
            libraries.libraries.any { it.uniqueId.startsWith("org.jetbrains.jewel:") },
            "The JVM resource must contain notices for the active Jewel dependencies",
        )
    }

    @Test
    fun `bundled font notice retains the complete distributed OFL text`() {
        val font = bundledLibraries().libraries.single { it.uniqueId == "font:ibm-plex-sans" }
        val license = font.licenses.single()
        assertEquals("OFL-1.1", license.spdxId)
        assertEquals(resource("fonts/OFL.txt"), license.licenseContent)
    }

    private fun bundledLibraries(): Libs = Libs.Builder().withJson(resource("aboutlibraries.json")).build()

    private fun content(body: @Composable () -> Unit) {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false, content = body)
            }
        }
    }

    private fun click(text: String) {
        rule.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    private fun fixtures(longText: Boolean = false): Libs {
        val licenses = setOf(
            License(
                name = "First license",
                url = null,
                licenseContent = if (longText) {
                    "First complete license\n".repeat(n = 300)
                } else {
                    "First complete license"
                },
                hash = "first",
            ),
            License("Second license", null, licenseContent = "Second complete license", hash = "second"),
        )
        val other = setOf(License("Other", null, licenseContent = "Unrelated license", hash = "other"))
        fun library(name: String, notices: Set<License>) = Library(
            uniqueId = name,
            artifactVersion = "1.0",
            name = name,
            description = null,
            website = null,
            developers = emptyList(),
            organization = null,
            scm = null,
            licenses = notices,
        )
        return Libs(
            listOf(
                library("Fixture library", licenses),
                library("Missing library", emptySet()),
                library("Other library", other),
            ),
            licenses + other,
        )
    }

    private fun resource(path: String): String = requireNotNull(javaClass.classLoader.getResourceAsStream(path)) {
        "Missing bundled resource: $path"
    }.bufferedReader().use { it.readText() }

    private companion object {
        const val LICENSE_SCROLL_DISTANCE = 400f
    }
}

private class LicenseTestClipboard : Clipboard {
    private var entry: ClipEntry? = null

    @Volatile
    var text: String? = null
        private set

    override suspend fun getClipEntry(): ClipEntry? = entry

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        entry = clipEntry
        text = clipEntry?.asAwtTransferable?.getTransferData(DataFlavor.stringFlavor) as? String
    }

    override val nativeClipboard: NativeClipboard = java.awt.datatransfer.Clipboard("license-test")
}
